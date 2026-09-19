package io.github.q93503128.turnbound.world;

import java.util.List;

/**
 * Small deterministic patrol selector for field encounters.
 *
 * <p>The "skip tiny next hops, vary the next patrol point, pause between moves" pattern is adapted from
 * GuardVillagersFabric's CC0 perimeter patrol. TURNBOUND keeps it deterministic so a shared encounter does not
 * jitter differently for different players or clients.</p>
 */
final class FieldRoamPlanner {
    static final double DEFAULT_MIN_NEXT_DISTANCE_SQ = 36.0D;

    record Point(double x, double z) {}

    private FieldRoamPlanner() {}

    static int nextIndex(List<Point> points, int currentIndex, long seed, double minDistanceSq) {
        if (points == null || points.size() <= 1) return 0;
        int size = points.size();
        int current = Math.floorMod(currentIndex, size);
        Point from = points.get(current);

        int startOffset = 1 + Math.floorMod((int) mix(seed), size - 1);
        int bestIndex = current;
        double bestDistance = -1.0D;

        for (int attempt = 0; attempt < size - 1; attempt++) {
            int offset = 1 + Math.floorMod(startOffset - 1 + attempt, size - 1);
            int candidateIndex = Math.floorMod(current + offset, size);
            Point candidate = points.get(candidateIndex);
            double dx = candidate.x() - from.x();
            double dz = candidate.z() - from.z();
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq > bestDistance) {
                bestDistance = distanceSq;
                bestIndex = candidateIndex;
            }
            if (distanceSq >= minDistanceSq) return candidateIndex;
        }
        return bestIndex == current ? Math.floorMod(current + 1, size) : bestIndex;
    }

    static int dwellTicks(int minTicks, int maxTicks, long seed) {
        int min = Math.max(0, minTicks);
        int max = Math.max(min, maxTicks);
        if (max == min) return min;
        int span = max - min + 1;
        return min + Math.floorMod((int) mix(seed ^ 0x9E3779B97F4A7C15L), span);
    }

    private static long mix(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }
}
