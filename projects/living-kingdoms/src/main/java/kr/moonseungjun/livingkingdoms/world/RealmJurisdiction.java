package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Shared Erden boundaries and civic landmarks derived from the active authored kingdom. */
public final class RealmJurisdiction {
    private static final int CAPITAL_BUFFER = 32;

    private RealmJurisdiction() {
    }

    public static String at(ServerLevel level, BlockPos pos) {
        if (!RealmSitePlanner.isBuilt(level, PlayableOriginCatalog.DEFAULT_HOMELAND)) return null;
        if (insideCapital(pos) || ErdenRegionalSettlementCatalog.settlementAt(pos.getX(), pos.getZ()) != null) {
            return PlayableOriginCatalog.DEFAULT_HOMELAND;
        }
        return null;
    }

    public static boolean inside(ServerLevel level, BlockPos pos, String homelandId, int radius) {
        if (!PlayableOriginCatalog.DEFAULT_HOMELAND.equals(homelandId)) return false;
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, PlayableOriginCatalog.DEFAULT_HOMELAND);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return false;
        long dx = pos.getX() - site.centerX();
        long dz = pos.getZ() - site.centerZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        requireErden(jurisdiction, "jurisdiction");
        return SafeResidenceLocator.jail(level, PlayableOriginCatalog.DEFAULT_HOMELAND);
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        requireErden(homelandId, "homeland");
        return SafeResidenceLocator.residence(level, PlayableOriginCatalog.DEFAULT_HOMELAND, residenceId);
    }

    private static boolean insideCapital(BlockPos pos) {
        return pos.getX() >= ErdenCapitalStreamingBuilder.WEST_WALL_X - CAPITAL_BUFFER
                && pos.getX() <= ErdenCapitalStreamingBuilder.EAST_WALL_X + CAPITAL_BUFFER
                && pos.getZ() >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - CAPITAL_BUFFER
                && pos.getZ() <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + CAPITAL_BUFFER;
    }

    private static void requireErden(String id, String type) {
        if (!PlayableOriginCatalog.DEFAULT_HOMELAND.equals(id)) {
            throw new IllegalArgumentException("Inactive " + type + ": " + id);
        }
    }
}
