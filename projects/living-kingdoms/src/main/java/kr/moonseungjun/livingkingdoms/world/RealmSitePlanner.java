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
    private static final int DETAILED_CANDIDATE_LIMIT = 64;
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
        List<int[]> centers = surveyCenters(nominal);

        List<CoarseCandidate> shortlist = centers.stream()
                .map(center -> coarseSample(level, homelandId, nominal, center[0], center[1]))
                .sorted(Comparator.comparingDouble(CoarseCandidate::score))
                .limit(DETAILED_CANDIDATE_LIMIT)
                .toList();
        List<Candidate> candidates = shortlist.stream()
                .map(center -> sample(level, homelandId, nominal, center.x(), center.z()))
                .toList();
        Selection selection = selectCandidate(homelandId, candidates);
        Candidate selected = selection.candidate();

        if (selection.fallback()) {
            LivingKingdoms.LOGGER.warn(
                    "No ideal capital site existed for {} on this seed; using the driest lowest-impact candidate {},{} range={} water={}/49 outer_water={}/16 median={} deviation={}",
                    homelandId, selected.x(), selected.z(), selected.range(), selected.waterSamples(),
                    selected.outerWaterSamples(), selected.medianY(), selected.meanDeviation()
            );
        }
        LivingKingdoms.LOGGER.info(
                "Generator survey {} selected {},{} range={} water={}/49 median={} deviation={} outer_land={}/16 outer_range={} coarse={} detailed={} fallback={} score={}",
                homelandId, selected.x(), selected.z(), selected.range(), selected.waterSamples(),
                selected.medianY(), selected.meanDeviation(), 16 - selected.outerWaterSamples(),
                selected.outerRange(), centers.size(), candidates.size(), selection.fallback(), selected.score()
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                selected.x(), selected.z(), selected.medianY(), LAYOUT_REVISION, false
        );
    }

    private static List<int[]> surveyCenters(int[] nominal) {
        List<int[]> centers = new ArrayList<>();
        for (int dx = -1_024; dx <= 1_024; dx += 128) {
            for (int dz = -1_024; dz <= 1_024; dz += 128) {
                centers.add(new int[]{nominal[0] + dx, nominal[1] + dz});
            }
        }
        for (int radius : new int[]{1_536, 2_048, 3_072, 4_096, 5_120, 6_144}) {
            for (int[] direction : new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
            }) {
                centers.add(new int[]{nominal[0] + direction[0] * radius,
                        nominal[1] + direction[1] * radius});
            }
        }
        return centers;
    }

    private static Selection selectCandidate(String homelandId, List<Candidate> candidates) {
        Comparator<Candidate> normal = Comparator.comparingDouble(Candidate::score);
        Candidate ideal = candidates.stream().filter(candidate -> acceptable(homelandId, candidate))
                .min(normal).orElse(null);
        if (ideal != null) return new Selection(ideal, false);

        Candidate emergency = candidates.stream().filter(candidate -> emergencyAcceptable(homelandId, candidate))
                .min(normal).orElse(null);
        if (emergency != null) return new Selection(emergency, true);

        // A difficult random seed must not abort world creation. The city core is deliberately
        // graded to one authored level and its boundary is blended later, so the safest remaining
        // choice is the dry candidate with the least cut/fill and edge impact.
        Candidate fallback = candidates.stream()
                .min(Comparator.comparingDouble(candidate -> fallbackScore(homelandId, candidate)))
                .orElseThrow(() -> new IllegalStateException("Capital survey returned no candidates"));
        return new Selection(fallback, true);
    }

    private static double fallbackScore(String homelandId, Candidate candidate) {
        int preferredY = "kardum_league".equals(homelandId) ? 72 : 66;
        double lowTerrain = Math.max(0, preferredY - candidate.medianY()) * 120_000.0;
        double water = candidate.waterSamples() * 100_000.0
                + candidate.outerWaterSamples() * 160_000.0;
        double shape = candidate.range() * 3_500.0
                + candidate.outerRange() * 2_000.0
                + candidate.meanDeviation() * 8_000.0
                + candidate.heavyCutFillSamples() * 4_000.0;
        return water + lowTerrain + shape + candidate.score();
    }

    /** Stores a background-survey result on the server thread. */
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

    /** Map reads do not start construction or load remote chunks. */
    public static BlockPos residencePosition(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = site(level, homelandId);
        if (site != null && site.built() && site.revision() >= LAYOUT_REVISION) {
            return SafeResidenceLocator.residence(level, homelandId, residenceId);
        }
        int[] center = site == null ? nominalCenter(homelandId)
                : new int[]{site.centerX(), site.centerZ()};
        int baseY = site == null ? 68 : site.baseY();
        int[] offset = residenceOffset(residenceId);
        int y = "silvana_tree_home".equals(residenceId)
                ? Math.max(70, Math.min(122, baseY)) + 17 : Math.max(68, baseY + 1);
        return new BlockPos(center[0] + offset[0], y, center[1] + offset[1]);
    }

    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    private static CoarseCandidate coarseSample(ServerLevel level, String homelandId,
                                                int[] nominal, int x, int z) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        List<Integer> heights = new ArrayList<>(9);
        int water = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int ox = -120; ox <= 120; ox += 120) {
            for (int oz = -120; oz <= 120; oz += 120) {
                TerrainPoint point = generatedPoint(generator, randomState, level, x + ox, z + oz);
                heights.add(point.y());
                min = Math.min(min, point.y());
                max = Math.max(max, point.y());
                if (point.water()) water++;
            }
        }
        heights.sort(Integer::compareTo);
        int median = heights.get(heights.size() / 2);
        double deviation = heights.stream().mapToDouble(value -> Math.abs(value - median))
                .average().orElse(99.0);
        double distance = Math.hypot(x - nominal[0], z - nominal[1]) / 24.0;
        double rangePenalty = "kardum_league".equals(homelandId)
                ? Math.abs((max - min) - 16) * 28.0 : (max - min) * 95.0;
        double heightPenalty = "kardum_league".equals(homelandId)
                ? Math.max(0, 72 - median) * 120.0 : Math.max(0, 66 - median) * 180.0;
        return new CoarseCandidate(x, z,
                rangePenalty + deviation * 170.0 + water * 2_600.0 + heightPenalty + distance);
    }

    private static Candidate sample(ServerLevel level, String homelandId, int[] nominal, int x, int z) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        List<Integer> heights = new ArrayList<>(49);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int water = 0;
        for (int ox = -120; ox <= 120; ox += 40) {
            for (int oz = -120; oz <= 120; oz += 40) {
                TerrainPoint point = generatedPoint(generator, randomState, level, x + ox, z + oz);
                heights.add(point.y());
                min = Math.min(min, point.y());
                max = Math.max(max, point.y());
                if (point.water()) water++;
            }
        }
        heights.sort(Integer::compareTo);
        int median = heights.get(heights.size() / 2);
        double deviation = heights.stream().mapToDouble(height -> Math.abs(height - median))
                .average().orElse(Double.MAX_VALUE);
        int heavy = (int) heights.stream().filter(height -> Math.abs(height - median) > 6).count();

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
        double cutFillPenalty = heavy * 500.0 + deviation * 260.0;
        double edgePenalty = Math.max(0, outerRange - 28) * 650.0;
        double distancePenalty = Math.hypot(x - nominal[0], z - nominal[1]) / 16.0;
        double score;
        if ("kardum_league".equals(homelandId)) {
            score = Math.abs(range - 18) * 46.0 + Math.max(0, 78 - median) * 80.0
                    + waterPenalty + cutFillPenalty + edgePenalty + distancePenalty;
        } else if ("silvana_forest".equals(homelandId)) {
            score = Math.abs(range - 10) * 62.0 + Math.max(0, range - 20) * 650.0
                    + waterPenalty + cutFillPenalty + edgePenalty + distancePenalty;
        } else {
            score = range * 145.0 + Math.max(0, median - 104) * 80.0
                    + waterPenalty + cutFillPenalty + edgePenalty + distancePenalty;
        }
        return new Candidate(x, z, min, max, median, water, outerWater, outerHeights.size(),
                deviation, heavy, outerRange, score);
    }

    private static TerrainPoint generatedPoint(ChunkGenerator generator, RandomState randomState,
                                                ServerLevel level, int x, int z) {
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE, level, randomState) - 1;
        NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
        BlockState state = column.getBlock(y);
        return new TerrainPoint(y, !state.getFluidState().isEmpty());
    }

    private static boolean acceptable(String homelandId, Candidate candidate) {
        if (candidate.medianY() < ("kardum_league".equals(homelandId) ? 72 : 68)) return false;
        if (candidate.outerWaterSamples() > 5 || candidate.outerHeightKinds() < 3) return false;
        if (candidate.outerRange() > ("kardum_league".equals(homelandId) ? 48 : 38)) return false;
        return switch (homelandId) {
            case "kardum_league" -> candidate.waterSamples() <= 3 && candidate.range() >= 5
                    && candidate.range() <= 34 && candidate.meanDeviation() <= 8.0
                    && candidate.heavyCutFillSamples() <= 22;
            case "silvana_forest" -> candidate.waterSamples() <= 6 && candidate.range() <= 24
                    && candidate.meanDeviation() <= 6.5 && candidate.heavyCutFillSamples() <= 18;
            default -> candidate.waterSamples() <= 3 && candidate.range() <= 19
                    && candidate.meanDeviation() <= 5.0 && candidate.heavyCutFillSamples() <= 13;
        };
    }

    private static boolean emergencyAcceptable(String homelandId, Candidate candidate) {
        if (candidate.medianY() < 64 || candidate.waterSamples() > 10
                || candidate.outerWaterSamples() > 8 || candidate.outerHeightKinds() < 2
                || candidate.outerRange() > 60) return false;
        return switch (homelandId) {
            case "kardum_league" -> candidate.range() <= 42 && candidate.meanDeviation() <= 12.0;
            case "silvana_forest" -> candidate.range() <= 32 && candidate.meanDeviation() <= 9.0;
            default -> candidate.range() <= 28 && candidate.meanDeviation() <= 8.0;
        };
    }

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

    private record CoarseCandidate(int x, int z, double score) {
    }

    private record Selection(Candidate candidate, boolean fallback) {
    }

    private record Candidate(int x, int z, int minY, int maxY, int medianY,
                             int waterSamples, int outerWaterSamples, int outerHeightKinds,
                             double meanDeviation, int heavyCutFillSamples, int outerRange,
                             double score) {
        int range() {
            return maxY - minY;
        }
    }
}
