package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredBiomeVerifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Owns the fixed political geography of the authored fantasy continent. */
public final class RealmSitePlanner {
    public static final int LAYOUT_REVISION = 12;

    private RealmSitePlanner() {
    }

    public static RealmSiteLayoutSavedData.RealmSite designedSite(String homelandId) {
        int[] center = nominalCenter(homelandId);
        int baseY = designedBaseY(homelandId);
        LivingKingdoms.LOGGER.info(
                "Using authored homeland anchor {} at {},{} baseY={} revision={}",
                homelandId, center[0], center[1], baseY, LAYOUT_REVISION
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                center[0], center[1], baseY, LAYOUT_REVISION, false
        );
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite storeDesignedSite(
            ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        RealmSiteLayoutSavedData.RealmSite designed = designedSite(homelandId);
        data.put(homelandId, designed);
        return designed;
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite ensureSite(ServerLevel level,
                                                                              String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        return storeDesignedSite(level, homelandId);
    }

    public static synchronized void markBuilt(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite site = data.site(homelandId).orElseThrow();
        AuthoredBiomeVerifier.verifyCapital(level, homelandId, site);
        RealmLayoutIntegrity.apply(level, homelandId, site);
        data.markBuilt(homelandId, LAYOUT_REVISION);
        LivingKingdoms.LOGGER.info(
                "Completed authored homeland {} at {},{}, baseY={}, revision={}",
                homelandId, site.centerX(), site.centerZ(), site.baseY(), LAYOUT_REVISION
        );
    }

    public static RealmSiteLayoutSavedData.RealmSite site(ServerLevel level, String homelandId) {
        return level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE)
                .site(homelandId).orElse(null);
    }

    public static boolean isBuilt(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = site(level, homelandId);
        return site != null && site.built() && site.revision() >= LAYOUT_REVISION;
    }

    public static BlockPos residencePosition(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = site(level, homelandId);
        if (site != null && site.built() && site.revision() >= LAYOUT_REVISION) {
            return SafeResidenceLocator.residence(level, homelandId, residenceId);
        }
        int[] center = site == null ? nominalCenter(homelandId)
                : new int[]{site.centerX(), site.centerZ()};
        int baseY = site == null ? designedBaseY(homelandId) : site.baseY();
        int[] offset = residenceOffset(residenceId);
        int y = "silvana_tree_home".equals(residenceId) ? baseY + 17 : baseY + 1;
        return new BlockPos(center[0] + offset[0], y, center[1] + offset[1]);
    }

    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    public static int[] nominalCenter(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> new int[]{-2_400, -1_200};
            case "kardum_league" -> new int[]{2_200, -1_500};
            case "red_steppe" -> new int[]{3_400, 300};
            case "velas_free_city" -> new int[]{600, 2_500};
            case "sahar_theocracy" -> new int[]{3_200, 2_600};
            case "grey_crown_ruins" -> new int[]{3_800, -2_800};
            case "northern_dragonlands" -> new int[]{0, -4_200};
            case "western_archipelago" -> new int[]{-4_200, 1_800};
            default -> new int[]{0, 0};
        };
    }

    public static int designedBaseY(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> 79;
            case "kardum_league" -> 92;
            case "red_steppe" -> 76;
            case "velas_free_city" -> 68;
            case "sahar_theocracy" -> 73;
            case "grey_crown_ruins" -> 88;
            case "northern_dragonlands" -> 105;
            case "western_archipelago" -> 67;
            default -> 72;
        };
    }

    private static int[] residenceOffset(String residenceId) {
        return switch (residenceId) {
            case "erden_city_room" -> new int[]{26, 36};
            case "erden_farm_home" -> new int[]{132, 98};
            case "river_fishing_hut" -> new int[]{-146, 91};
            case "forest_camp" -> new int[]{116, -132};
            case "silvana_tree_home" -> new int[]{-45, -28};
            case "silvana_moonwell_lodge" -> new int[]{73, 87};
            case "kardum_gate_lodge" -> new int[]{-4, -72};
            case "kardum_worker_quarters" -> new int[]{-72, 43};
            default -> new int[]{26, 36};
        };
    }
}
