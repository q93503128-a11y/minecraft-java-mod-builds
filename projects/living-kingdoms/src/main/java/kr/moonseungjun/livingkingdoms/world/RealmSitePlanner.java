package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists stable political geography and requires connected dry land around every capital. */
public final class RealmSitePlanner {
    public static final int LAYOUT_REVISION = 8;
    private static final int SEARCH_RADIUS = 2_048;
    private static final int SEARCH_STEP = 256;
    private static final int[][] OUTER_SAMPLES = {
            {224, 0}, {-224, 0}, {0, 224}, {0, -224},
            {224, 224}, {-224, 224}, {224, -224}, {-224, -224},
            {112, 224}, {-112, 224}, {112, -224}, {-112, -224},
            {224, 112}, {-224, 112}, {224, -112}, {-224, -112}
    };

    private RealmSitePlanner() {
    }

    public static RealmSiteLayoutSavedData.RealmSite surveyGeneratedTerrain(ServerLevel level,
                                                                             String homelandId) {
        int[] anchor = nominalCenter(homelandId);
        List<Candidate> candidates = new ArrayList<>();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx += SEARCH_STEP) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz += SEARCH_STEP) {
                candidates.add(sample(level, homelandId, anchor[0] + dx, anchor[1] + dz, dx, dz));
            }
        }
        Comparator<Candidate> score = Comparator.comparingDouble(Candidate::score);
        Candidate selected = candidates.stream()
                .filter(candidate -> candidate.water() <= 4 && candidate.outerWater() <= 7)
                .min(score)
                .orElseGet(() -> candidates.stream()
                        .filter(candidate -> candidate.outerWater() <= 9)
                        .min(score)
                        .orElseThrow(() -> new IllegalStateException(
                                "No connected capital district exists for " + homelandId)));
        LivingKingdoms.LOGGER.info(
                "Authored district {} selected {},{} baseY={} inner_land={}/25 outer_land={}/16 range={} distance={} score={}",
                homelandId, selected.x(), selected.z(), selected.baseY(),
                25 - selected.water(), 16 - selected.outerWater(), selected.range(),
                selected.distance(), selected.score()
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                selected.x(), selected.z(), selected.baseY(), LAYOUT_REVISION, false
        );
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite storeSurvey(
            ServerLevel level, String homelandId, RealmSiteLayoutSavedData.RealmSite surveyed) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        data.put(homelandId, surveyed);
        return surveyed;
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite ensureSite(ServerLevel level,
                                                                              String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        return storeSurvey(level, homelandId, surveyGeneratedTerrain(level, homelandId));
    }

    public static synchronized void markBuilt(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        data.markBuilt(homelandId, LAYOUT_REVISION);
        RealmSiteLayoutSavedData.RealmSite site = data.site(homelandId).orElseThrow();
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
        int baseY = site == null ? preferredBaseY(homelandId) : site.baseY();
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

    private static Candidate sample(ServerLevel level, String homelandId,
                                    int x, int z, int offsetX, int offsetZ) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        List<Integer> heights = new ArrayList<>(25);
        int water = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -128; dx <= 128; dx += 64) {
            for (int dz = -128; dz <= 128; dz += 64) {
                TerrainPoint point = generatedPoint(generator, randomState, level, x + dx, z + dz);
                heights.add(point.y());
                min = Math.min(min, point.y());
                max = Math.max(max, point.y());
                if (point.water()) water++;
            }
        }
        int outerWater = 0;
        for (int[] offset : OUTER_SAMPLES) {
            if (generatedPoint(generator, randomState, level, x + offset[0], z + offset[1]).water()) {
                outerWater++;
            }
        }
        heights.sort(Integer::compareTo);
        int median = heights.get(heights.size() / 2);
        int preferred = preferredBaseY(homelandId);
        int baseY = clamp(median, preferred - 10, preferred + 22);
        double distance = Math.hypot(offsetX, offsetZ);
        double terrainPreference;
        if ("kardum_league".equals(homelandId)) {
            terrainPreference = Math.abs((max - min) - 18) * 80.0 + Math.max(0, 76 - median) * 500.0;
        } else if ("silvana_forest".equals(homelandId)) {
            terrainPreference = Math.abs((max - min) - 9) * 110.0;
        } else {
            terrainPreference = (max - min) * 180.0;
        }
        double score = water * 30_000.0 + outerWater * 45_000.0
                + terrainPreference + distance * 1.5;
        return new Candidate(x, z, baseY, water, outerWater, max - min, distance, score);
    }

    private static TerrainPoint generatedPoint(ChunkGenerator generator, RandomState randomState,
                                                ServerLevel level, int x, int z) {
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE, level, randomState) - 1;
        NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
        BlockState state = column.getBlock(y);
        return new TerrainPoint(y, !state.getFluidState().isEmpty());
    }

    private static int preferredBaseY(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> 76;
            case "kardum_league" -> 86;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record TerrainPoint(int y, boolean water) {}
    private record Candidate(int x, int z, int baseY, int water, int outerWater, int range,
                             double distance, double score) {}
}
