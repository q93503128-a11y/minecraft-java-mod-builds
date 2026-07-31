package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sequential asynchronous CI verification for noise terrain and all three regional capitals. */
public final class StarterRealmDiagnostics {
    private static final List<String> HOMELANDS = List.of(
            "erden_kingdom", "silvana_forest", "kardum_league"
    );

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
                    "LK_REALM_DIAGNOSTIC_PASS regions=3 residences=8 noise_terrain=true surveyed_sites=true erden_facilities=10 layout_revision={} generation_ms={}",
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
        for (int[] offset : new int[][]{{205, 0}, {-205, 0}, {0, 205}, {0, -205}, {190, 170}, {-185, -175}}) {
            heights.add(RealmSitePlanner.surfaceY(realm, site.centerX() + offset[0], site.centerZ() + offset[1]));
        }
        if (heights.size() < 3) {
            throw new IllegalStateException("Noise terrain is effectively flat outside " + homelandId + ": " + heights);
        }
    }

    private static void verifyErdenFacilities(ServerLevel realm, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(112, site.baseY()));
        requireSolid(realm, new BlockPos(cx - 34, y + 1, cz - 91), "citadel");
        requireSolid(realm, new BlockPos(cx + 18, y + 1, cz - 82), "administration hall");
        requireSolid(realm, new BlockPos(cx - 74, y + 1, cz - 78), "temple");
        requireSolid(realm, new BlockPos(cx, y, cz), "market square");
        requireSolid(realm, new BlockPos(cx + 47, y + 1, cz + 14), "inn");
        requireSolid(realm, new BlockPos(cx - 77, y + 1, cz + 17), "guild hall");
        requireSolid(realm, new BlockPos(cx + 78, y + 1, cz - 35), "smithy");
        requireSolid(realm, new BlockPos(cx - 106, y + 1, cz - 39), "barracks");
        requireSolid(realm, new BlockPos(cx + 82, y + 1, cz + 72), "granary");
        requireSolid(realm, new BlockPos(cx - 120, y + 3, cz + 80), "city wall");
        if (realm.getBlockState(new BlockPos(cx - 164, y - 2, cz)).getBlock() != Blocks.WATER) {
            throw new IllegalStateException("Erden canal was not integrated with the capital");
        }
        verifyRoofSupport(realm, new BlockPos(cx - 56, y + 6, cz - 40));
    }

    private static void requireSolid(ServerLevel realm, BlockPos pos, String facility) {
        if (realm.getBlockState(pos).isAir()) {
            throw new IllegalStateException("Missing Erden facility " + facility + " at " + pos);
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
