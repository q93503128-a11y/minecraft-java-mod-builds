package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

/** Bounded CI-only verification for noise terrain, surveyed sites and complete starter capitals. */
public final class StarterRealmDiagnostics {
    private StarterRealmDiagnostics() {
    }

    public static void runIfRequested(MinecraftServer server) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) throw new IllegalStateException("Living Kingdoms realm is unavailable during diagnostics");

        long started = System.nanoTime();
        for (String homelandId : PlayableOriginCatalog.HOMELANDS) {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.ensureBuilt(realm, homelandId);
            if (!site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
                throw new IllegalStateException("Homeland layout was not built: " + homelandId);
            }
            verifyNaturalTerrainOutsideCapital(realm, site, homelandId);
        }

        for (PlayableOriginCatalog.ResidenceOption residence : PlayableOriginCatalog.residences().values()) {
            BlockPos feet = RealmSitePlanner.residencePosition(realm, residence.homelandId(), residence.id());
            verifySpawn(realm, residence.id(), feet);
        }

        RealmSiteLayoutSavedData.RealmSite erden = RealmSitePlanner.site(realm, "erden_kingdom");
        if (erden == null) throw new IllegalStateException("Erden site is missing");
        verifyErdenFacilities(realm, erden);

        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        if (elapsedMs > 420_000L) {
            throw new IllegalStateException("Noise terrain survey and capital construction exceeded 420 seconds: " + elapsedMs);
        }
        LivingKingdoms.LOGGER.info(
                "LK_REALM_DIAGNOSTIC_PASS regions=3 residences=8 noise_terrain=true surveyed_sites=true erden_facilities=10 layout_revision={} generation_ms={}",
                RealmSitePlanner.LAYOUT_REVISION, elapsedMs
        );
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
        for (int[] offset : new int[][]{{280, 0}, {-280, 0}, {0, 280}, {0, -280}, {240, 190}, {-210, -250}}) {
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
        boolean supported = false;
        for (int dy = 1; dy <= 8; dy++) {
            if (!realm.getBlockState(roof.below(dy)).isAir()) {
                supported = true;
                break;
            }
        }
        if (!supported) throw new IllegalStateException("Floating roof detected at " + roof);
    }
}
