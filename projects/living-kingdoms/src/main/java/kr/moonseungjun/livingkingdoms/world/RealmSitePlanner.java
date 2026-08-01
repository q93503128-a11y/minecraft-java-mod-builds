package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Surveys generated noise terrain and stores anchors without writing an entire kingdom in one tick. */
public final class RealmSitePlanner {
    public static final int LAYOUT_REVISION = 4;

    private RealmSitePlanner() {
    }

    /** Returns the persisted site or surveys a new one. This method never constructs buildings. */
    public static synchronized RealmSiteLayoutSavedData.RealmSite ensureSite(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite current = data.site(homelandId).orElse(null);
        if (current != null) return current;

        RealmSiteLayoutSavedData.RealmSite surveyed = survey(level, homelandId);
        data.put(homelandId, surveyed);
        return surveyed;
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

    /**
     * Returns a real residence position after construction and a harmless projected position while it is pending.
     * Reading codex/map data must never trigger construction or synchronously load the entire capital.
     */
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
            baseY = 65;
            built = false;
        } else {
            centerX = site.centerX();
            centerZ = site.centerZ();
            baseY = site.baseY();
            built = site.built() && site.revision() >= LAYOUT_REVISION;
        }

        int x = centerX + offset[0];
        int z = centerZ + offset[1];
        int y;
        if ("silvana_tree_home".equals(residenceId)) {
            y = baseY + 17;
        } else if (built) {
            y = surfaceY(level, x, z) + 1;
        } else {
            y = Math.max(65, baseY + 1);
        }
        return new BlockPos(x, y, z);
    }

    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    private static RealmSiteLayoutSavedData.RealmSite survey(ServerLevel level, String homelandId) {
        int[] nominal = nominalCenter(homelandId);
        List<Candidate> candidates = new ArrayList<>();
        for (int dx = -128; dx <= 128; dx += 64) {
            for (int dz = -128; dz <= 128; dz += 64) {
                candidates.add(sample(level, homelandId, nominal[0] + dx, nominal[1] + dz));
            }
        }
        Candidate selected = candidates.stream().min(Comparator.comparingDouble(Candidate::score))
                .orElseThrow(() -> new IllegalStateException("No terrain candidate for " + homelandId));
        LivingKingdoms.LOGGER.info(
                "Terrain survey {} selected {},{} range={} water={}/25 average={} score={}",
                homelandId, selected.x(), selected.z(), selected.maxY() - selected.minY(),
                selected.waterSamples(), selected.averageY(), selected.score()
        );
        return new RealmSiteLayoutSavedData.RealmSite(
                selected.x(), selected.z(), selected.averageY(), LAYOUT_REVISION, false
        );
    }

    private static Candidate sample(ServerLevel level, String homelandId, int x, int z) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        int water = 0;
        int samples = 0;
        for (int ox = -48; ox <= 48; ox += 24) {
            for (int oz = -48; oz <= 48; oz += 24) {
                int sx = x + ox;
                int sz = z + oz;
                int y = surfaceY(level, sx, sz);
                min = Math.min(min, y);
                max = Math.max(max, y);
                sum += y;
                samples++;
                if (!level.getFluidState(new BlockPos(sx, y, sz)).isEmpty()) water++;
            }
        }
        int average = Math.round(sum / (float) Math.max(1, samples));
        int range = max - min;
        double submergedPenalty = water > 8 ? 20_000.0 + water * 500.0 : water * 40.0;
        double lowPenalty = average < 64 ? 20_000.0 + (64 - average) * 500.0 : 0.0;
        double score;
        if ("kardum_league".equals(homelandId)) {
            score = Math.abs(range - 18) * 3.0 + Math.max(0, 78 - average) * 4.0
                    + submergedPenalty + lowPenalty;
        } else if ("silvana_forest".equals(homelandId)) {
            score = Math.max(0, range - 16) * 6.0 + Math.abs(range - 8) * 1.5
                    + submergedPenalty + lowPenalty;
        } else {
            score = range * 8.0 + Math.abs(water - 1) * 12.0
                    + Math.max(0, average - 105) * 1.5 + submergedPenalty + lowPenalty;
        }
        return new Candidate(x, z, min, max, average, water, score);
    }

    private static int[] nominalCenter(String homelandId) {
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

    private record Candidate(int x, int z, int minY, int maxY, int averageY,
                             int waterSamples, double score) {
    }
}
