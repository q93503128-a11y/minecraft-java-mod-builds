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

/** End-to-end CI verification for the revision 8 authored realm. */
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
                verifyTerrainTransition(realm, site, homelandId);
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
                verifySpawn(realm, residence.id(), feet);
            }
            RealmSiteLayoutSavedData.RealmSite erden = requiredSite(realm, "erden_kingdom");
            verifyErdenStructures(realm, erden);

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            if (elapsedMs > 900_000L) {
                throw new IllegalStateException("Realm construction exceeded 900 seconds: " + elapsedMs);
            }
            LivingKingdoms.LOGGER.info(
                    "LK_REALM_DIAGNOSTIC_PASS regions=3 residences=8 authored_terrain=true smooth_transitions=true structure_shells=true debris_zero=true layout_revision={} generation_ms={}",
                    RealmSitePlanner.LAYOUT_REVISION, elapsedMs
            );
        } catch (Throwable throwable) {
            fail("Final realm verification failed", throwable);
        }
    }

    private static RealmSiteLayoutSavedData.RealmSite requiredSite(ServerLevel realm, String id) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, id);
        if (site == null) throw new IllegalStateException("Missing site " + id);
        return site;
    }

    private static void verifySpawn(ServerLevel realm, String id, BlockPos feet) {
        if (!SafeResidenceLocator.isWalkable(realm, feet)) {
            throw new IllegalStateException("Unsafe residence spawn " + id + " at " + feet);
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
            if (realm.getFluidState(new BlockPos(x, y, z)).isEmpty()) land++;
        }
        if (land < 9) {
            throw new IllegalStateException("Capital district is isolated by water: " + homelandId + " land=" + land);
        }
        if (heights.size() < 2) {
            throw new IllegalStateException("Outer terrain is unnaturally flat: " + homelandId);
        }
    }

    private static void verifyTerrainTransition(ServerLevel realm,
                                                RealmSiteLayoutSavedData.RealmSite site,
                                                String homelandId) {
        int inner = switch (homelandId) {
            case "silvana_forest" -> 90;
            case "kardum_league" -> 116;
            default -> 136;
        };
        int outer = inner + 54;
        int worstStep = 0;
        int worstX = site.centerX();
        int worstZ = site.centerZ();
        for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            Integer previous = null;
            for (int distance = inner; distance <= outer; distance += 6) {
                int x = site.centerX() + direction[0] * distance;
                int z = site.centerZ() + direction[1] * distance;
                int y = terrainY(realm, x, z);
                if (previous != null) {
                    int step = Math.abs(y - previous);
                    if (step > worstStep) {
                        worstStep = step;
                        worstX = x;
                        worstZ = z;
                    }
                }
                previous = y;
            }
        }
        // Six horizontal blocks per sample: values above twelve still expose the artificial box
        // cliffs seen in earlier alphas, while a ten-block natural ridge is retained as terrain.
        if (worstStep > 12) {
            throw new IllegalStateException("Cliff transition detected around " + homelandId
                    + ": step=" + worstStep + " at " + worstX + "," + worstZ);
        }
        LivingKingdoms.LOGGER.info("Verified terrain transition {} max_step={} at {},{}",
                homelandId, worstStep, worstX, worstZ);
    }

    private static void verifyNoConstructionDebris(ServerLevel realm,
                                                    RealmSiteLayoutSavedData.RealmSite site,
                                                    String homelandId) {
        ConstructionDebrisCleaner.schedule(realm, homelandId, site);
        int radius = "erden_kingdom".equals(homelandId) ? 330 : 260;
        AABB bounds = new AABB(site.centerX() - radius, realm.getMinY(), site.centerZ() - radius,
                site.centerX() + radius + 1, realm.getMaxY(), site.centerZ() + radius + 1);
        List<ItemEntity> items = realm.getEntitiesOfClass(ItemEntity.class, bounds);
        if (!items.isEmpty()) {
            throw new IllegalStateException("Item debris remained around " + homelandId + ": " + items.size());
        }
    }

    private static void verifyErdenStructures(ServerLevel realm,
                                              RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(104, site.baseY()));
        requireBlock(realm, new BlockPos(cx, y + 7, cz), Blocks.LANTERN, "market lantern");
        requireBlock(realm, new BlockPos(cx, y, cz + 40), Blocks.PACKED_MUD, "main road");
        requireBlock(realm, new BlockPos(cx - 124, y + 3, cz + 30), Blocks.STONE_BRICKS, "city wall");
        int canalX = cx - 151 + (int) Math.round(Math.sin(cz * 0.045) * 5.0);
        requireBlock(realm, new BlockPos(canalX, y - 2, cz), Blocks.WATER, "canal");

        BlockPos homeFloor = new BlockPos(cx - 58, y, cz - 39);
        if (realm.getBlockState(homeFloor).isAir()) {
            throw new IllegalStateException("House floor missing at " + homeFloor);
        }
        BlockPos wall = new BlockPos(cx - 58, y + 2, cz - 38);
        if (realm.getBlockState(wall).isAir()) {
            throw new IllegalStateException("House wall missing at " + wall);
        }
        boolean roofFound = false;
        for (int dy = 6; dy <= 12; dy++) {
            if (!realm.getBlockState(new BlockPos(cx - 58, y + dy, cz - 39)).isAir()) {
                roofFound = true;
                break;
            }
        }
        if (!roofFound) throw new IllegalStateException("House roof missing above " + homeFloor);
    }

    private static int terrainY(ServerLevel realm, int x, int z) {
        realm.getChunk(x >> 4, z >> 4);
        return realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static void requireBlock(ServerLevel realm, BlockPos pos, Block expected, String name) {
        Block actual = realm.getBlockState(pos).getBlock();
        if (actual != expected) {
            throw new IllegalStateException("Missing " + name + " at " + pos
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void fail(String message, Throwable throwable) {
        if (throwable == null) LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message);
        else LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message, throwable);
    }
}
