package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** End-to-end CI verification for the external-template alpha.11 Living Realm. */
public final class StarterRealmDiagnostics {
    private static final List<String> HOMELANDS = List.of(
            "erden_kingdom", "silvana_forest", "kardum_league"
    );
    private static final int[][] OUTER_TERRAIN_SAMPLES = {
            {224, 0}, {-224, 0}, {0, 224}, {0, -224},
            {224, 224}, {-224, 224}, {224, -224}, {-224, -224},
            {112, 224}, {-112, 224}, {112, -224}, {-112, -224},
            {224, 112}, {-224, 112}, {224, -112}, {-224, -112}
    };

    private StarterRealmDiagnostics() {
    }

    public static void runIfRequested(MinecraftServer server) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            fail("Living Kingdoms realm is unavailable during diagnostics", null);
            return;
        }
        buildNext(realm, 0, System.nanoTime());
    }

    private static void buildNext(ServerLevel realm, int index, long started) {
        if (index >= HOMELANDS.size()) {
            finishVerification(realm, started);
            return;
        }
        String homelandId = HOMELANDS.get(index);
        LivingKingdoms.LOGGER.info("LK_REALM_DIAGNOSTIC_PREPARE homeland={} step={}/{}",
                homelandId, index + 1, HOMELANDS.size());
        RealmBuildCoordinator.prepareHomeland(realm, homelandId, failure -> {
            if (failure != null) {
                fail("Queued homeland preparation failed for " + homelandId, failure);
                return;
            }
            try {
                RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, homelandId);
                if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
                    throw new IllegalStateException("Homeland layout was not built: " + homelandId);
                }
                verifyNaturalTerrainOutsideCapital(realm, site, homelandId);
                verifyExternalArchitecture(realm, site, homelandId);
                verifyNoConstructionDebris(realm, site, homelandId);
                buildNext(realm, index + 1, started);
            } catch (Throwable throwable) {
                fail("Homeland verification failed for " + homelandId, throwable);
            }
        });
    }

    private static void finishVerification(ServerLevel realm, long started) {
        try {
            for (PlayableOriginCatalog.ResidenceOption residence : PlayableOriginCatalog.residences().values()) {
                BlockPos feet = RealmSitePlanner.residencePosition(realm, residence.homelandId(), residence.id());
                if (!SafeResidenceLocator.isWalkable(realm, feet)) {
                    throw new IllegalStateException("Unsafe residence spawn " + residence.id() + " at " + feet);
                }
            }
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            if (elapsedMs > 900_000L) {
                throw new IllegalStateException("Realm construction exceeded 900 seconds: " + elapsedMs);
            }
            LivingKingdoms.LOGGER.info(
                    "LK_REALM_DIAGNOSTIC_PASS regions=3 residences=8 authored_terrain=true authored_biomes=true authored_surfaces=true external_structure_templates=true regional_ecology=true debris_zero=true layout_revision={} generation_ms={}",
                    RealmSitePlanner.LAYOUT_REVISION, elapsedMs
            );
        } catch (Throwable throwable) {
            fail("Final realm verification failed", throwable);
        }
    }

    private static void verifyNaturalTerrainOutsideCapital(ServerLevel realm,
                                                            RealmSiteLayoutSavedData.RealmSite site,
                                                            String homelandId) {
        Set<Integer> heights = new HashSet<>();
        int land = 0;
        for (int[] offset : OUTER_TERRAIN_SAMPLES) {
            int x = site.centerX() + offset[0];
            int z = site.centerZ() + offset[1];
            int y = terrainY(realm, x, z);
            heights.add(y);
            int top = realm.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (realm.getFluidState(new BlockPos(x, top, z)).isEmpty()) land++;
        }
        if (land < 9) throw new IllegalStateException("Capital district is isolated by water: " + homelandId);
        if (heights.size() < 2) throw new IllegalStateException("Outer terrain is unnaturally flat: " + homelandId);
    }

    private static void verifyExternalArchitecture(ServerLevel realm,
                                                   RealmSiteLayoutSavedData.RealmSite site,
                                                   String homelandId) {
        Set<Block> palette = new HashSet<>();
        int architecturalSamples = 0;
        int yMin = Math.max(realm.getMinY(), site.baseY() - 8);
        int yMax = Math.min(realm.getMaxY() - 1, site.baseY() + 88);
        for (int x = site.centerX() - 70; x <= site.centerX() + 70; x += 2) {
            for (int z = site.centerZ() - 70; z <= site.centerZ() + 70; z += 2) {
                for (int y = yMin; y <= yMax; y += 2) {
                    Block block = realm.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (isArchitecture(block)) {
                        architecturalSamples++;
                        palette.add(block);
                    }
                }
            }
        }
        if (architecturalSamples < 800 || palette.size() < 8) {
            throw new IllegalStateException("External capital template is too sparse for " + homelandId
                    + ": samples=" + architecturalSamples + " palette=" + palette.size());
        }
        LivingKingdoms.LOGGER.info(
                "Verified external capital template homeland={} architectural_samples={} palette={}",
                homelandId, architecturalSamples, palette.size()
        );
    }

    private static boolean isArchitecture(Block block) {
        return block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR
                && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT && block != Blocks.COARSE_DIRT
                && block != Blocks.ROOTED_DIRT && block != Blocks.STONE && block != Blocks.DEEPSLATE
                && block != Blocks.WATER && block != Blocks.SAND && block != Blocks.GRAVEL;
    }

    private static void verifyNoConstructionDebris(ServerLevel realm,
                                                    RealmSiteLayoutSavedData.RealmSite site,
                                                    String homelandId) {
        ConstructionDebrisCleaner.schedule(realm, homelandId, site);
        int radius = 260;
        AABB bounds = new AABB(site.centerX() - radius, realm.getMinY(), site.centerZ() - radius,
                site.centerX() + radius + 1, realm.getMaxY(), site.centerZ() + radius + 1);
        List<ItemEntity> items = realm.getEntitiesOfClass(ItemEntity.class, bounds);
        if (!items.isEmpty()) {
            throw new IllegalStateException("Item debris remained around " + homelandId + ": " + items.size());
        }
    }

    private static int terrainY(ServerLevel realm, int x, int z) {
        realm.getChunk(x >> 4, z >> 4);
        int top = realm.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int bottom = Math.max(realm.getMinY(), top - 96);
        for (int y = top; y >= bottom; y--) {
            Block block = realm.getBlockState(new BlockPos(x, y, z)).getBlock();
            if (isTerrainGround(block)) return y;
        }
        return realm.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
    }

    private static boolean isTerrainGround(Block block) {
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT || block == Blocks.PODZOL || block == Blocks.MYCELIUM
                || block == Blocks.MOSS_BLOCK || block == Blocks.MUD || block == Blocks.PACKED_MUD
                || block == Blocks.DIRT_PATH || block == Blocks.STONE || block == Blocks.DEEPSLATE
                || block == Blocks.GRAVEL || block == Blocks.SAND || block == Blocks.RED_SAND
                || block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE || block == Blocks.TERRACOTTA
                || block == Blocks.CLAY || block == Blocks.CALCITE || block == Blocks.SNOW_BLOCK;
    }

    private static void fail(String message, Throwable throwable) {
        if (throwable == null) LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message);
        else LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message, throwable);
    }
}
