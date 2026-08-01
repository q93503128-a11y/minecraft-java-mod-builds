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
    public static final int LAYOUT_REVISION = 6;
    private static final int[][] OUTER_SAMPLES = {
            {224, 0}, {-224, 0}, {0, 224}, {0, -224},
            {224, 224}, {-224, 224}, {224, -224}, {-224, -224},
            {112, 224}, {-112, 224}, {112, -224}, {-112, -224},
            {224, 112}, {-224, 112}, {224, -112}, {-224, -112}
    };

    private RealmSitePlanner() {
    }

    /** Pure generator survey. Safe to execute on a world-generation background executor. */
    public static RealmSiteLayoutSavedData.RealmSite surveyGeneratedTerrain(ServerLevel level, String homelandId) {
        int[] nominal = nominalCenter(homelandId);
        List<int[]> centers = new ArrayList<>();
        for (int dx = -128; dx <= 128; dx += 64) {
            for (int dz = -128; dz <= 128; dz += 64) centers.add(new int[]{nominal[0] + dx, nominal[1] + dz});
        }
        for (int radius : new int[]{320, 640, 960, 1280}) {
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
                .orElseGet(() -> candidates.stream()
                        .min(Comparator.comparingDouble(Candidate::score))
                        .orElseThrow(() -> new IllegalStateException("No terrain candidate for " + homelandId)));

        LivingKingdoms.LOGGER.info(
                "Generator survey {} selected {},{} range={} water={}/25 average={} outer_land={}/16 outer_heights={} score={}",
                homelandId, selected.x(), selected.z(), selected.maxY() - selected.minY(),
                selected.waterSamples(), selected.averageY(), 16 - selected.outerWaterSamples(),
                selected.outerHeightKinds(), selected.score()
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                selected.x(), selected.z(), selected.averageY(), LAYOUT_REVISION, false
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
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        int water = 0;
        int samples = 0;
        for (int ox = -48; ox <= 48; ox += 24) {
            for (int oz = -48; oz <= 48; oz += 24) {
                TerrainPoint point = generatedPoint(generator, randomState, level, x + ox, z + oz);
                min = Math.min(min, point.y());
                max = Math.max(max, point.y());
                sum += point.y();
                samples++;
                if (point.water()) water++;
            }
        }

        Set<Integer> outerHeights = new HashSet<>();
        int outerWater = 0;
        for (int[] offset : OUTER_SAMPLES) {
            TerrainPoint point = generatedPoint(generator, randomState, level, x + offset[0], z + offset[1]);
            outerHeights.add(point.y());
            if (point.water()) outerWater++;
        }

        int average = Math.round(sum / (float) Math.max(1, samples));
        int range = max - min;
        double submergedPenalty = water > 4 ? 50_000.0 + water * 1_000.0 : water * 180.0;
        double lowPenalty = average < 68 ? 50_000.0 + (68 - average) * 1_500.0 : 0.0;
        double outerWaterPenalty = outerWater > 6
                ? 100_000.0 + outerWater * 2_000.0
                : outerWater * 700.0;
        double outerFlatPenalty = outerHeights.size() < 2 ? 50_000.0 : outerHeights.size() < 3 ? 2_000.0 : 0.0;
        double score;
        if ("kardum_league".equals(homelandId)) {
            score = Math.abs(range - 18) * 3.0 + Math.max(0, 78 - average) * 8.0
                    + submergedPenalty + lowPenalty + outerWaterPenalty + outerFlatPenalty;
        } else if ("silvana_forest".equals(homelandId)) {
            score = Math.max(0, range - 20) * 5.0 + Math.abs(range - 10) * 1.5
                    + submergedPenalty + lowPenalty + outerWaterPenalty + outerFlatPenalty;
        } else {
            score = Math.max(0, range - 24) * 10.0 + Math.abs(range - 10) * 2.0
                    + Math.max(0, average - 108) * 1.5
                    + submergedPenalty + lowPenalty + outerWaterPenalty + outerFlatPenalty;
        }
        return new Candidate(x, z, min, max, average, water, outerWater, outerHeights.size(), score);
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
        int minimumAverage = "kardum_league".equals(homelandId) ? 72 : 68;
        int maximumWater = "silvana_forest".equals(homelandId) ? 4 : 3;
        return candidate.averageY() >= minimumAverage
                && candidate.waterSamples() <= maximumWater
                && candidate.outerWaterSamples() <= 6
                && candidate.outerHeightKinds() >= 2;
    }

    public static int[] nominalCenter(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> new int[]{1500, 250};
            case "kardum_league" -> new int[]{-1500, 250};
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

    private record TerrainPoint(int y, boolean water) {}

    private record Candidate(
            int x,
            int z,
            int minY,
            int maxY,
            int averageY,
            int waterSamples,
            int outerWaterSamples,
            int outerHeightKinds,
            double score
    ) {}
}
