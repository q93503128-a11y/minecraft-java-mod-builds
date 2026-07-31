package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Surveys generated noise terrain before any kingdom is placed. */
public final class RealmSitePlanner {
    public static final int LAYOUT_REVISION = 2;

    private RealmSitePlanner() {
    }

    public static synchronized RealmSiteLayoutSavedData.RealmSite ensureBuilt(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData data = level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE);
        RealmSiteLayoutSavedData.RealmSite site = data.site(homelandId).orElseGet(() -> {
            RealmSiteLayoutSavedData.RealmSite surveyed = survey(level, homelandId);
            data.put(homelandId, surveyed);
            return surveyed;
        });
        if (!site.built() || site.revision() < LAYOUT_REVISION) {
            TerrainIntegratedCapitalBuilder.build(level, homelandId, site);
            data.markBuilt(homelandId, LAYOUT_REVISION);
            site = new RealmSiteLayoutSavedData.RealmSite(
                    site.centerX(), site.centerZ(), site.baseY(), LAYOUT_REVISION, true
            );
            LivingKingdoms.LOGGER.info(
                    "Built optimized terrain-integrated homeland {} at {},{}, baseY={}, revision={}",
                    homelandId, site.centerX(), site.centerZ(), site.baseY(), LAYOUT_REVISION
            );
        }
        return site;
    }

    public static RealmSiteLayoutSavedData.RealmSite site(ServerLevel level, String homelandId) {
        return level.getDataStorage().computeIfAbsent(RealmSiteLayoutSavedData.TYPE)
                .site(homelandId).orElse(null);
    }

    public static BlockPos residencePosition(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = ensureBuilt(level, homelandId);
        int[] offset = residenceOffset(residenceId);
        int x = site.centerX() + offset[0];
        int z = site.centerZ() + offset[1];
        int y = surfaceY(level, x, z) + 1;
        return new BlockPos(x, y, z);
    }

    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    private static RealmSiteLayoutSavedData.RealmSite survey(ServerLevel level, String homelandId) {
        int[] nominal = nominalCenter(homelandId);
        List<Candidate> candidates = new ArrayList<>();
        for (int dx = -192; dx <= 192; dx += 96) {
            for (int dz = -192; dz <= 192; dz += 96) {
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
            case "erden_farm_home" -> new int[]{175, 105};
            case "river_fishing_hut" -> new int[]{-170, 115};
            case "forest_camp" -> new int[]{135, -165};
            case "silvana_moonwell_lodge" -> new int[]{86, 85};
            case "kardum_gate_lodge" -> new int[]{0, -76};
            case "kardum_worker_quarters" -> new int[]{-72, 42};
            default -> new int[]{26, 36};
        };
    }

    private record Candidate(int x, int z, int minY, int maxY, int averageY,
                             int waterSamples, double score) {
    }
}
