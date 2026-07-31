package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Shared dynamic boundaries and civic landmarks derived from surveyed sites. */
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
        if (site == null) return false;
        long dx = pos.getX() - site.centerX();
        long dz = pos.getZ() - site.centerZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.ensureBuilt(level, jurisdiction);
        int[] offset = switch (jurisdiction) {
            case "silvana_forest" -> new int[]{-84, 0, 74};
            case "kardum_league" -> new int[]{60, 11, -72};
            default -> new int[]{-96, 2, -35};
        };
        int x = site.centerX() + offset[0];
        int z = site.centerZ() + offset[2];
        int y = Math.max(RealmSitePlanner.surfaceY(level, x, z) + 1, site.baseY() + offset[1]);
        return new BlockPos(x, y, z);
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        return RealmSitePlanner.residencePosition(level, homelandId, residenceId);
    }
}
