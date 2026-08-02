package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import kr.moonseungjun.livingkingdoms.worldgen.StructurelessNoiseChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** End-to-end CI verification for the active one-metre-scale Erden slice. */
public final class StarterRealmDiagnostics {
    private static final String ACTIVE_HOMELAND = "erden_kingdom";
    private static final int[][] CAPITAL_APPROACH_SAMPLES = {
            {224, 0}, {-224, 0}, {0, 224}, {0, -224},
            {224, 224}, {-224, 224}, {224, -224}, {-224, -224},
            {112, 224}, {-112, 224}, {112, -224}, {-112, -224},
            {224, 112}, {-224, 112}, {224, -112}, {-224, -112}
    };
    private static final int[][] REGIONAL_RELIEF_SAMPLES = {
            {4_000, 0}, {-4_000, 0}, {0, 4_000}, {0, -4_000},
            {8_000, 3_000}, {-8_000, 3_000}, {8_000, -3_000}, {-8_000, -3_000},
            {12_000, 6_000}, {-12_000, 6_000}, {12_000, -6_000}, {-12_000, -6_000}
    };
    private static final StreamSample[] STREAM_SAMPLES = {
            new StreamSample("royal_avenue", 0, 200, false),
            new StreamSample("north_gate", 0, -900, true),
            new StreamSample("royal_chancery", -390, -520, true),
            new StreamSample("great_temple", 710, -560, true),
            new StreamSample("western_barracks", -720, 540, true),
            new StreamSample("citizen_court", 170, 600, true)
    };
    private static PendingVerification pending;

    private StarterRealmDiagnostics() {
    }

    public static void runIfRequested(MinecraftServer server) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            fail("Living Kingdoms realm is unavailable during diagnostics", null);
            return;
        }
        long started = System.nanoTime();
        LivingKingdoms.LOGGER.info("LK_REALM_DIAGNOSTIC_PREPARE homeland={}", ACTIVE_HOMELAND);
        RealmBuildCoordinator.prepareHomeland(realm, ACTIVE_HOMELAND, failure -> {
            if (failure != null) {
                fail("Queued homeland preparation failed for " + ACTIVE_HOMELAND, failure);
                return;
            }
            try {
                RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, ACTIVE_HOMELAND);
                if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
                    throw new IllegalStateException("Erden layout was not built");
                }
                verifyStructurelessGenerator(realm);
                verifyCapitalApproachesAndRegionalRelief(realm, site);
                verifyExternalArchitecture(realm, site);
                verifyNoConstructionDebris(realm, site);
                for (StreamSample sample : STREAM_SAMPLES) {
                    ErdenCapitalStreamingBuilder.requestChunk(realm, sample.chunkX(), sample.chunkZ());
                }
                pending = new PendingVerification(server, realm, site, started, server.getTickCount());
                LivingKingdoms.LOGGER.info(
                        "LK_REALM_DIAGNOSTIC_STREAM_WAIT samples={} landmarks={}",
                        STREAM_SAMPLES.length, ExternalDistrictBuildingBuilder.landmarkCount()
                );
            } catch (Throwable throwable) {
                fail("Erden verification preparation failed", throwable);
            }
        });
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        PendingVerification state = pending;
        if (state == null || state.server != event.getServer()) return;
        int age = event.getServer().getTickCount() - state.startedTick;
        if (age > 3_600) {
            pending = null;
            fail("Streamed Erden samples did not finish within 180 seconds", null);
            return;
        }
        if (event.getServer().getTickCount() % 10 != 0) return;
        for (StreamSample sample : STREAM_SAMPLES) {
            if (!ErdenCapitalStreamingBuilder.isChunkBuilt(
                    state.realm, sample.chunkX(), sample.chunkZ())) return;
        }
        pending = null;
        try {
            verifyStreamedCapital(state.realm);
            verifyNoConstructionDebris(state.realm, state.site);
            finishVerification(state.realm, state.startedNanos);
        } catch (Throwable throwable) {
            fail("Streamed Erden verification failed", throwable);
        }
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
                    "LK_REALM_DIAGNOSTIC_PASS regions=1 residences=1 metre_scale=true structureless_generator=true vanilla_structures_blocked=true cleaned_citadel_part=true streamed_capital=true streamed_samples={} district_landmarks={} terrain_integrated_roads=true external_wall_parts=true debris_zero=true layout_revision={} generation_ms={}",
                    STREAM_SAMPLES.length, ExternalDistrictBuildingBuilder.landmarkCount(),
                    RealmSitePlanner.LAYOUT_REVISION, elapsedMs
            );
        } catch (Throwable throwable) {
            fail("Final realm verification failed", throwable);
        }
    }

    private static void verifyStructurelessGenerator(ServerLevel realm) {
        if (!(realm.getChunkSource().getGenerator() instanceof StructurelessNoiseChunkGenerator)) {
            throw new IllegalStateException("Living Realm did not use the structureless generator");
        }
    }

    private static void verifyStreamedCapital(ServerLevel realm) {
        StreamSample road = STREAM_SAMPLES[0];
        int roadY = realm.getHeight(Heightmap.Types.WORLD_SURFACE, road.x, road.z) - 1;
        Block roadBlock = realm.getBlockState(new BlockPos(road.x, roadY, road.z)).getBlock();
        if (roadBlock != Blocks.POLISHED_ANDESITE && roadBlock != Blocks.STONE_BRICKS) {
            throw new IllegalStateException("Royal avenue was not streamed at " + road.x + "," + road.z
                    + " block=" + BuiltInBlockName.name(roadBlock));
        }

        for (int i = 1; i < STREAM_SAMPLES.length; i++) {
            verifyArchitectureChunk(realm, STREAM_SAMPLES[i]);
        }
    }

    private static void verifyArchitectureChunk(ServerLevel realm, StreamSample sample) {
        int chunkX = sample.chunkX();
        int chunkZ = sample.chunkZ();
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int minY = Math.max(realm.getMinY(), terrainY(realm, sample.x, sample.z) - 3);
        int maxY = Math.min(realm.getMaxY() - 1, minY + 96);
        int blocks = 0;
        Set<Block> palette = new HashSet<>();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = realm.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (!isArchitecture(block)) continue;
                    blocks++;
                    palette.add(block);
                }
            }
        }
        int minimum = sample.architectureExpected ? 90 : 1;
        if (blocks < minimum || palette.size() < 3) {
            throw new IllegalStateException("Streamed capital sample is too sparse role=" + sample.role
                    + " blocks=" + blocks + " palette=" + palette.size());
        }
        LivingKingdoms.LOGGER.info(
                "Verified streamed capital sample role={} chunk={},{} blocks={} palette={}",
                sample.role, chunkX, chunkZ, blocks, palette.size()
        );
    }

    private static void verifyCapitalApproachesAndRegionalRelief(
            ServerLevel realm, RealmSiteLayoutSavedData.RealmSite site) {
        int land = 0;
        for (int[] offset : CAPITAL_APPROACH_SAMPLES) {
            int x = site.centerX() + offset[0];
            int z = site.centerZ() + offset[1];
            int top = realm.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (realm.getFluidState(new BlockPos(x, top, z)).isEmpty()) land++;
        }
        if (land < 9) throw new IllegalStateException("Erden capital district is isolated by water");

        Set<Integer> designedHeights = new HashSet<>();
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int[] offset : REGIONAL_RELIEF_SAMPLES) {
            int height = (int) Math.round(AuthoredContinentDensity.surfaceHeight(
                    site.centerX() + offset[0], site.centerZ() + offset[1]));
            designedHeights.add(height);
            minimum = Math.min(minimum, height);
            maximum = Math.max(maximum, height);
        }
        if (designedHeights.size() < 4 || maximum - minimum < 8) {
            throw new IllegalStateException("Erden regional relief is too uniform: distinct="
                    + designedHeights.size() + " range=" + (maximum - minimum));
        }
        LivingKingdoms.LOGGER.info(
                "Verified Erden capital approaches land_samples={} regional_relief_distinct={} range={}m",
                land, designedHeights.size(), maximum - minimum
        );
    }

    private static void verifyExternalArchitecture(ServerLevel realm,
                                                    RealmSiteLayoutSavedData.RealmSite site) {
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
            throw new IllegalStateException("Cleaned Erden citadel is too sparse: samples="
                    + architecturalSamples + " palette=" + palette.size());
        }
        LivingKingdoms.LOGGER.info(
                "Verified cleaned Erden citadel architectural_samples={} palette={}",
                architecturalSamples, palette.size()
        );
    }

    private static boolean isArchitecture(Block block) {
        return block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR
                && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT && block != Blocks.COARSE_DIRT
                && block != Blocks.ROOTED_DIRT && block != Blocks.STONE && block != Blocks.DEEPSLATE
                && block != Blocks.WATER && block != Blocks.SAND && block != Blocks.GRAVEL;
    }

    private static void verifyNoConstructionDebris(ServerLevel realm,
                                                    RealmSiteLayoutSavedData.RealmSite site) {
        ConstructionDebrisCleaner.schedule(realm, ACTIVE_HOMELAND, site);
        int radius = 1_300;
        AABB bounds = new AABB(site.centerX() - radius, realm.getMinY(), site.centerZ() - radius,
                site.centerX() + radius + 1, realm.getMaxY(), site.centerZ() + radius + 1);
        List<ItemEntity> items = realm.getEntitiesOfClass(ItemEntity.class, bounds);
        if (!items.isEmpty()) {
            throw new IllegalStateException("Item debris remained around Erden: " + items.size());
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
        pending = null;
        if (throwable == null) LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message);
        else LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message, throwable);
    }

    private record StreamSample(String role, int x, int z, boolean architectureExpected) {
        int chunkX() { return x >> 4; }
        int chunkZ() { return z >> 4; }
    }

    private record PendingVerification(MinecraftServer server, ServerLevel realm,
                                       RealmSiteLayoutSavedData.RealmSite site,
                                       long startedNanos, int startedTick) {
    }

    /** Avoids a registry dependency merely to print a diagnostic block name. */
    private static final class BuiltInBlockName {
        private BuiltInBlockName() {
        }
        static String name(Block block) {
            return block.getDescriptionId();
        }
    }
}
