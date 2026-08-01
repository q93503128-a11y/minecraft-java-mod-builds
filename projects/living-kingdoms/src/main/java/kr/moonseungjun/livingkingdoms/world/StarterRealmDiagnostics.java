package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sequential asynchronous CI verification for noise terrain and all three regional capitals. */
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

            RealmSiteLayoutSavedData.RealmSite erden = RealmSitePlanner.site(realm, "erden_kingdom");
            if (erden == null) throw new IllegalStateException("Erden site is missing");
            verifyErdenFacilities(realm, erden);

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            if (elapsedMs > 900_000L) {
                throw new IllegalStateException(
                        "Noise terrain preparation and capital construction exceeded 900 seconds: " + elapsedMs
                );
            }
            LivingKingdoms.LOGGER.info(
                    "LK_REALM_DIAGNOSTIC_PASS regions=3 residences=8 noise_terrain=true surveyed_sites=true connected_land=true erden_facilities=10 layout_revision={} generation_ms={}",
                    RealmSitePlanner.LAYOUT_REVISION, elapsedMs
            );
        } catch (Throwable throwable) {
            fail("Final realm verification failed", throwable);
        }
    }

    private static void fail(String message, Throwable throwable) {
        if (throwable == null) LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message);
        else LivingKingdoms.LOGGER.error("LK_REALM_DIAGNOSTIC_FAIL {}", message, throwable);
    }

    private static void verifySpawn(ServerLevel realm, String id, BlockPos feet) {
        if (realm.getBlockState(feet.below()).isAir()) {
            throw new IllegalStateException("Air floor at dynamic residence " + id + ": " + feet.below());
        }
        if (!realm.getBlockState(feet).isAir() || !realm.getBlockState(feet.above()).isAir()) {
            throw new IllegalStateException("Blocked headroom at dynamic residence " + id + ": " + feet);
        }
    }

    private static void verifyNaturalTerrainOutsideCapital(ServerLevel realm,
                                                            RealmSiteLayoutSavedData.RealmSite site,
                                                            String homelandId) {
        Set<Integer> heights = new HashSet<>();
        int landSamples = 0;
        int waterSamples = 0;
        for (int[] offset : OUTER_TERRAIN_SAMPLES) {
            int x = site.centerX() + offset[0];
            int z = site.centerZ() + offset[1];
            int y = RealmSitePlanner.surfaceY(realm, x, z);
            heights.add(y);
            if (realm.getFluidState(new BlockPos(x, y, z)).isEmpty()) landSamples++;
            else waterSamples++;
        }
        if (landSamples < 10) {
            throw new IllegalStateException(
                    "Capital is isolated by water outside " + homelandId
                            + ": land=" + landSamples + " water=" + waterSamples
            );
        }
        if (heights.size() < 2) {
            throw new IllegalStateException(
                    "Natural terrain has no height variation outside " + homelandId + ": " + heights
            );
        }
        LivingKingdoms.LOGGER.info(
                "Verified connected natural terrain {} land={}/{} height_kinds={}",
                homelandId, landSamples, OUTER_TERRAIN_SAMPLES.length, heights.size()
        );
    }

    private static void verifyErdenFacilities(ServerLevel realm, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(112, site.baseY()));

        requireBlock(realm, new BlockPos(cx - 18, y + 3, cz - 70), Blocks.LANTERN, "citadel hall");
        requireBlock(realm, new BlockPos(cx + 32, y + 3, cz - 67), Blocks.LANTERN, "administration hall");
        requireBlock(realm, new BlockPos(cx - 63, y + 2, cz - 66), Blocks.BELL, "temple");
        requireBlock(realm, new BlockPos(cx, y + 7, cz), Blocks.LANTERN, "market square");
        requireBlock(realm, new BlockPos(cx + 57, y + 2, cz + 21), Blocks.CAMPFIRE, "inn");
        requireBlock(realm, new BlockPos(cx - 72, y + 2, cz + 22), Blocks.CARTOGRAPHY_TABLE, "guild hall");
        requireBlock(realm, new BlockPos(cx + 98, y + 1, cz - 30), Blocks.BLAST_FURNACE, "smithy");
        requireBlock(realm, new BlockPos(cx - 86, y + 2, cz - 36), Blocks.IRON_BARS, "barracks");
        requireBlock(realm, new BlockPos(cx + 85, y + 2, cz + 79), Blocks.BARREL, "granary");
        requireBlock(realm, new BlockPos(cx - 120, y + 3, cz + 80), Blocks.STONE_BRICKS, "city wall");

        int canalX = cx - 164 + (int) Math.round(Math.sin(cz * 0.045) * 6.0);
        requireBlock(realm, new BlockPos(canalX, y - 2, cz), Blocks.WATER, "canal");
        verifyRoofSupport(realm, new BlockPos(cx - 58, y + 6, cz - 40));
        verifyInteriorHeadroom(realm, new BlockPos(cx - 25, y + 1, cz - 80), "citadel interior");
    }

    private static void requireBlock(ServerLevel realm, BlockPos pos, Block expected, String facility) {
        Block actual = realm.getBlockState(pos).getBlock();
        if (actual != expected) {
            throw new IllegalStateException(
                    "Missing Erden facility " + facility + " at " + pos
                            + ": expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void verifyInteriorHeadroom(ServerLevel realm, BlockPos feet, String facility) {
        if (!realm.getBlockState(feet).isAir() || !realm.getBlockState(feet.above()).isAir()) {
            throw new IllegalStateException("Blocked " + facility + " at " + feet);
        }
        if (realm.getBlockState(feet.below()).isAir()) {
            throw new IllegalStateException("Missing floor below " + facility + " at " + feet.below());
        }
    }

    private static void verifyRoofSupport(ServerLevel realm, BlockPos roof) {
        if (realm.getBlockState(roof).isAir()) {
            throw new IllegalStateException("Expected connected house roof at " + roof);
        }
        for (int dy = 1; dy <= 8; dy++) {
            if (!realm.getBlockState(roof.below(dy)).isAir()) return;
        }
        throw new IllegalStateException("Floating roof detected at " + roof);
    }
}
