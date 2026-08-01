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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Surveys generator columns without loading chunks, then persists one connected-land capital anchor. */
public final class RealmSitePlanner {
    public static final int LAYOUT_REVISION = 7;
    private static final int[][] OUTER_SAMPLES = {
            {320, 0}, {-320, 0}, {0, 320}, {0, -320},
            {320, 320}, {-320, 320}, {320, -320}, {-320, -320},
            {160, 320}, {-160, 320}, {160, -320}, {-160, -320},
            {320, 160}, {-320, 160}, {320, -160}, {-320, -160}
    };

    private RealmSitePlanner() {
    }

    /** Pure generator survey. Safe to execute on a world-generation background executor. */
    public static RealmSiteLayoutSavedData.RealmSite surveyGeneratedTerrain(ServerLevel level, String homelandId) {
        int[] nominal = nominalCenter(homelandId);
        List<int[]> centers = new ArrayList<>();

        // Dense local search first. Capitals stay in their authored geopolitical region instead of
        // drifting toward the first merely survivable patch near world spawn.
        for (int dx = -384; dx <= 384; dx += 128) {
            for (int dz = -384; dz <= 384; dz += 128) {
                centers.add(new int[]{nominal[0] + dx, nominal[1] + dz});
            }
        }
        for (int radius : new int[]{640, 1_024, 1_536, 2_048, 3_072}) {
            for (int[] direction : new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
            }) {
                centers.add(new int[]{nominal[0] + direction[0] * radius, nominal[1] + direction[1] * radius});
            }
        }

        List<Candidate> candidates = centers.stream()
                .map(center -> sample(level, homelandId, center[0], center[1]))
                .toList();
        Candidate selected = candidates.stream()
                .filter(candidate -> acceptable(homelandId, candidate))
                .min(Comparator.comparingDouble(Candidate::score))
                .orElseThrow(() -> new IllegalStateException(
                        "No capital site passed the terrain-integrity gate for " + homelandId
                                + "; refusing to create a cliff box or submerged city"
                ));

        LivingKingdoms.LOGGER.info(
                "Generator survey {} selected {},{} range={} water={}/49 median={} deviation={} outer_land={}/16 outer_range={} score={}",
                homelandId, selected.x(), selected.z(), selected.maxY() - selected.minY(),
                selected.waterSamples(), selected.medianY(), selected.meanDeviation(),
                16 - selected.outerWaterSamples(), selected.outerRange(), selected.score()
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                selected.x(), selected.z(), selected.medianY(), LAYOUT_REVISION, false
        );
    }

    /** Stores a background-survey result on the server thread. */
    public static synchronized RealmSiteLayoutSavedData.RealmSite storeSurvey(
            ServerLevel level,
            String homelandId,
            RealmSiteLayoutSavedData.RealmSite surveyed
    ) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null && current.revision() >= LAYOUT_REVISION) return current;
        data.put(homelandId, surveyed);
        return surveyed;
    }

    /** Synchronous fallback that still does not load chunks. */
    public static synchronized RealmSiteLayoutSavedData.RealmSite ensureSite(ServerLevel level, String homelandId) {
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
                "Completed terrain-integrated homeland {} at {},{}, baseY={}, revision={}",
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

    /** Map reads never start construction or force a remote chunk load. */
    public static BlockPos residencePosition(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = site(level, homelandId);
        int[] offset = residenceOffset(residenceId);
        int centerX;
        int centerZ;
        int baseY;
        boolean built;
        if (site == null) {
            int[] nominal = nominalCenter(homelandId);
            centerX = nominal[0];
            centerZ = nominal[1];
            baseY = 68;
            built = false;
        } else {
            centerX = site.centerX();
            centerZ = site.centerZ();
            baseY = site.baseY();
            built = site.built() && site.revision() >= LAYOUT_REVISION;
        }
        int x = centerX + offset[0];
        int z = centerZ + offset[1];
        int y = "silvana_tree_home".equals(residenceId)
                ? Math.max(70, Math.min(122, baseY)) + 17
                : built ? surfaceY(level, x, z) + 1 : Math.max(68, baseY + 1);
        return new BlockPos(x, y, z);
    }

    /** Actual generated-world surface, used only after selected-site pregeneration. */
    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    private static Candidate sample(ServerLevel level, String homelandId, int x, int z) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        List<Integer> innerHeights = new ArrayList<>(49);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int water = 0;
        for (int ox = -72; ox <= 72; ox += 24) {
            for (int oz = -72; oz <= 72; oz += 24) {
                TerrainPoint point = generatedPoint(generator, randomState, level, x + ox, z + oz);
                innerHeights.add(point.y());
                min = Math.min(min, point.y());
                max = Math.max(max, point.y());
                if (point.water()) water++;
            }
        }
        innerHeights.sort(Integer::compareTo);
        int median = innerHeights.get(innerHeights.size() / 2);
        double meanDeviation = innerHeights.stream()
                .mapToDouble(height -> Math.abs(height - median))
                .average().orElse(Double.MAX_VALUE);
        int heavyCutFillSamples = (int) innerHeights.stream()
                .filter(height -> Math.abs(height - median) > 5)
                .count();

        Set<Integer> outerHeights = new HashSet<>();
        int outerWater = 0;
        int outerMin = Integer.MAX_VALUE;
        int outerMax = Integer.MIN_VALUE;
        for (int[] offset : OUTER_SAMPLES) {
            TerrainPoint point = generatedPoint(generator, randomState, level, x + offset[0], z + offset[1]);
            outerHeights.add(point.y());
            outerMin = Math.min(outerMin, point.y());
            outerMax = Math.max(outerMax, point.y());
            if (point.water()) outerWater++;
        }

        int range = max - min;
        int outerRange = outerMax - outerMin;
        double waterPenalty = water * 1_100.0 + outerWater * 2_000.0;
        double cutFillPenalty = heavyCutFillSamples * 500.0 + meanDeviation * 180.0;
        double edgePenalty = Math.max(0, outerRange - 24) * 550.0;
        double score;
        if ("kardum_league".equals(homelandId)) {
            score = Math.abs(range - 16) * 42.0 + Math.max(0, 78 - median) * 80.0
                    + waterPenalty + cutFillPenalty + edgePenalty;
        } else if ("silvana_forest".equals(homelandId)) {
            score = Math.abs(range - 8) * 55.0 + Math.max(0, range - 16) * 600.0
                    + waterPenalty + cutFillPenalty + edgePenalty;
        } else {
            score = range * 120.0 + Math.max(0, median - 104) * 80.0
                    + waterPenalty + cutFillPenalty + edgePenalty;
        }
        return new Candidate(x, z, min, max, median, water, outerWater, outerHeights.size(),
                meanDeviation, heavyCutFillSamples, outerRange, score);
    }

    private static TerrainPoint generatedPoint(
            ChunkGenerator generator,
            RandomState randomState,
            ServerLevel level,
            int x,
            int z
    ) {
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE, level, randomState) - 1;
        NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
        BlockState state = column.getBlock(y);
        return new TerrainPoint(y, !state.getFluidState().isEmpty());
    }

    private static boolean acceptable(String homelandId, Candidate candidate) {
        if (candidate.medianY() < ("kardum_league".equals(homelandId) ? 72 : 68)) return false;
        if (candidate.outerWaterSamples() > 4 || candidate.outerHeightKinds() < 3) return false;
        if (candidate.outerRange() > ("kardum_league".equals(homelandId) ? 42 : 30)) return false;

        return switch (homelandId) {
            case "kardum_league" -> candidate.waterSamples() <= 2
                    && candidate.maxY() - candidate.minY() >= 6
                    && candidate.maxY() - candidate.minY() <= 28
                    && candidate.meanDeviation() <= 6.5
                    && candidate.heavyCutFillSamples() <= 22;
            case "silvana_forest" -> candidate.waterSamples() <= 5
                    && candidate.maxY() - candidate.minY() <= 17
                    && candidate.meanDeviation() <= 4.5
                    && candidate.heavyCutFillSamples() <= 14;
            default -> candidate.waterSamples() <= 2
                    && candidate.maxY() - candidate.minY() <= 12
                    && candidate.meanDeviation() <= 3.0
                    && candidate.heavyCutFillSamples() <= 8;
        };
    }

    /** Fixed geopolitical anchors shared by every player and every multiplayer session. */
    public static int[] nominalCenter(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> new int[]{-9_000, -1_500};
            case "kardum_league" -> new int[]{-2_500, -9_000};
            case "red_steppe" -> new int[]{9_500, -1_000};
            case "velas_free_city" -> new int[]{1_500, 7_500};
            case "sahar_theocracy" -> new int[]{9_000, 9_000};
            case "grey_crown_ruins" -> new int[]{8_500, -7_500};
            case "northern_dragonlands" -> new int[]{0, -15_000};
            case "western_archipelago" -> new int[]{-14_000, 7_000};
            default -> new int[]{0, 0};
        };
    }

    private static int[] residenceOffset(String residenceId) {
        return switch (residenceId) {
            case "erden_city_room" -> new int[]{20, 31};
            case "erden_farm_home" -> new int[]{170, 105};
            case "river_fishing_hut" -> new int[]{-176, 110};
            case "forest_camp" -> new int[]{133, -167};
            case "silvana_tree_home" -> new int[]{-58, -30};
            case "silvana_moonwell_lodge" -> new int[]{82, 82};
            case "kardum_gate_lodge" -> new int[]{-10, -77};
            case "kardum_worker_quarters" -> new int[]{-78, 38};
            default -> new int[]{20, 31};
        };
    }

    private record TerrainPoint(int y, boolean water) {
    }

    private record Candidate(
            int x,
            int z,
            int minY,
            int maxY,
            int medianY,
            int waterSamples,
            int outerWaterSamples,
            int outerHeightKinds,
            double meanDeviation,
            int heavyCutFillSamples,
            int outerRange,
            double score
    ) {
    }
}
