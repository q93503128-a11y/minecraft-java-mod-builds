package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Shared dynamic boundaries and civic landmarks derived from completed surveyed sites. */
public final class RealmJurisdiction {
    private RealmJurisdiction() {
    }

    public static String at(ServerLevel level, BlockPos pos) {
        if (inside(level, pos, "erden_kingdom", 360)) return "erden_kingdom";
        if (inside(level, pos, "silvana_forest", 320)) return "silvana_forest";
        if (inside(level, pos, "kardum_league", 320)) return "kardum_league";
        return null;
    }

    public static boolean inside(ServerLevel level, BlockPos pos, String homelandId, int radius) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return false;
        long dx = pos.getX() - site.centerX();
        long dz = pos.getZ() - site.centerZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        return SafeResidenceLocator.jail(level, jurisdiction);
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        return SafeResidenceLocator.residence(level, homelandId, residenceId);
    }
}
