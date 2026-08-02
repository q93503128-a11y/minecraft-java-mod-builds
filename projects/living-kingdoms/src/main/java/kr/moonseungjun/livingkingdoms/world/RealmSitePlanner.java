package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredBiomeVerifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Owns the fixed geography of the active Erden kingdom slice. */
public final class RealmSitePlanner {
    public static final int LAYOUT_REVISION = 14;

    private RealmSitePlanner() {
    }

    public static RealmSiteLayoutSavedData.RealmSite designedSite(String homelandId) {
        requireErden(homelandId);
        int[] center = nominalCenter(homelandId);
        int baseY = designedBaseY(homelandId);
        LivingKingdoms.LOGGER.info(
                "Using one-metre Erden anchor at {},{} baseY={} revision={}",
                center[0], center[1], baseY, LAYOUT_REVISION
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                center[0], center[1], baseY, LAYOUT_REVISION, false
        );
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite storeDesignedSite(
            ServerLevel level, String homelandId) {
        requireErden(homelandId);
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        RealmSiteLayoutSavedData.RealmSite designed = designedSite(homelandId);
        data.put(homelandId, designed);
        return designed;
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite ensureSite(ServerLevel level,
                                                                              String homelandId) {
        requireErden(homelandId);
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        return storeDesignedSite(level, homelandId);
    }

    public static synchronized void markBuilt(ServerLevel level, String homelandId) {
        requireErden(homelandId);
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite site = data.site(homelandId).orElseThrow();
        AuthoredBiomeVerifier.verifyCapital(level, homelandId, site);
        RealmLayoutIntegrity.apply(level, homelandId, site);
        data.markBuilt(homelandId, LAYOUT_REVISION);
        LivingKingdoms.LOGGER.info(
                "Completed active Erden site at {},{}, baseY={}, revision={}",
                site.centerX(), site.centerZ(), site.baseY(), LAYOUT_REVISION
        );
    }

    public static RealmSiteLayoutSavedData.RealmSite site(ServerLevel level, String homelandId) {
        requireErden(homelandId);
        return level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE)
                .site(homelandId).orElse(null);
    }

    public static boolean isBuilt(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = site(level, homelandId);
        return site != null && site.built() && site.revision() >= LAYOUT_REVISION;
    }

    public static BlockPos residencePosition(ServerLevel level, String homelandId, String residenceId) {
        requireErden(homelandId);
        if (!"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive residence: " + residenceId);
        }
        RealmSiteLayoutSavedData.RealmSite site = site(level, homelandId);
        if (site != null && site.built() && site.revision() >= LAYOUT_REVISION) {
            return SafeResidenceLocator.residence(level, homelandId, residenceId);
        }
        int[] center = site == null ? nominalCenter(homelandId)
                : new int[]{site.centerX(), site.centerZ()};
        int baseY = site == null ? designedBaseY(homelandId) : site.baseY();
        return new BlockPos(center[0] + 320, baseY + 1, center[1] + 180);
    }

    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    public static int[] nominalCenter(String homelandId) {
        requireErden(homelandId);
        return new int[]{0, 0};
    }

    public static int designedBaseY(String homelandId) {
        requireErden(homelandId);
        return 72;
    }

    private static void requireErden(String homelandId) {
        if (!"erden_kingdom".equals(homelandId)) {
            throw new IllegalArgumentException("Inactive homeland: " + homelandId);
        }
    }
}
