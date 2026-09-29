package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Deterministic New Drabyel service placement resolved from the bound migrated world.
 *
 * <p>The source anchors, road line and protected source-content coordinates come from the pinned public
 * DrehmalMap extraction. They only choose where to search. Final Y, collision clearance and nearby original
 * villagers/block entities are evaluated against the live world. Unsafe services simply stay absent.</p>
 */
final class DrabyelHubAutoPlacement {
    private record RoadPoint(int x, int z) {}
    private record Candidate(int x, int y, int z, double score) {}

    /**
     * Simplified centerline extracted from the pinned Drehmal path GeoJSON around New Drabyel.
     * Used only to keep service actors beside the road instead of in its center.
     */
    private static final List<RoadPoint> ROAD = List.of(
            new RoadPoint(502, 1801), new RoadPoint(503, 1808), new RoadPoint(505, 1814),
            new RoadPoint(507, 1821), new RoadPoint(511, 1825), new RoadPoint(513, 1829),
            new RoadPoint(509, 1833), new RoadPoint(508, 1838), new RoadPoint(509, 1844),
            new RoadPoint(511, 1849), new RoadPoint(515, 1850), new RoadPoint(523, 1849),
            new RoadPoint(532, 1846), new RoadPoint(532, 1841), new RoadPoint(533, 1834),
            new RoadPoint(536, 1829), new RoadPoint(542, 1828), new RoadPoint(548, 1830),
            new RoadPoint(554, 1832), new RoadPoint(556, 1838), new RoadPoint(557, 1845),
            new RoadPoint(557, 1851), new RoadPoint(555, 1855), new RoadPoint(551, 1857),
            new RoadPoint(544, 1857), new RoadPoint(539, 1855), new RoadPoint(536, 1852)
    );

    private static final Map<ServerLevel, Map<String, DrabyelHubServiceCatalog.Service>> CACHE =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerLevel, Map<String, Long>> RETRY_AT =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final long FAILED_SCAN_RETRY_TICKS = 200L;
    private static final double MIN_SERVICE_SPACING_SQ = 2.5D * 2.5D;

    private DrabyelHubAutoPlacement() {}

    static List<DrabyelHubServiceCatalog.Service> runtimeServices(ServerLevel level) {
        if (level == null || level.getServer() == null || !DrehmalWorldBinding.isBound(level.getServer())) return List.of();

        Map<String, DrabyelHubServiceCatalog.Service> cache =
                CACHE.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        Map<String, Long> retryAt = RETRY_AT.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        long gameTime = level.getGameTime();
        List<DrabyelHubServiceCatalog.Service> out = new ArrayList<>();

        for (var base : DrabyelHubServiceCatalog.hub().services()) {
            DrabyelHubServiceCatalog.Service staticService = staticProduction(base);
            if (staticService != null) {
                cache.put(base.locator(), staticService);
                out.add(staticService);
                continue;
            }

            DrabyelHubServiceCatalog.Service resolved = cache.get(base.locator());
            if (resolved == null && gameTime >= retryAt.getOrDefault(base.locator(), Long.MIN_VALUE)) {
                resolved = resolve(level, base, cache.values());
                if (resolved != null) {
                    cache.put(base.locator(), resolved);
                    retryAt.remove(base.locator());
                } else {
                    retryAt.put(base.locator(), gameTime + FAILED_SCAN_RETRY_TICKS);
                }
            }
            if (resolved != null) out.add(resolved);
        }
        return List.copyOf(out);
    }

    static DrabyelHubServiceCatalog.Service runtimeService(ServerLevel level, String locator) {
        if (locator == null || locator.isBlank()) return null;
        for (var service : runtimeServices(level)) if (locator.equals(service.locator())) return service;
        return null;
    }

    static Set<String> sourceRoles() {
        return DrabyelHubServiceCatalog.hub().services().stream()
                .filter(service -> DrabyelMapPlacementCatalog.placement(service.locator()) != null)
                .map(DrabyelHubServiceCatalog.Service::role)
                .collect(Collectors.toUnmodifiableSet());
    }

    static void clear() {
        CACHE.clear();
        RETRY_AT.clear();
    }

    private static DrabyelHubServiceCatalog.Service staticProduction(DrabyelHubServiceCatalog.Service base) {
        return base.productionEnabled() && base.verifiedIn26_2()
                && base.runtimePosition() != null && base.runtimeYaw() != null
                ? base : null;
    }

    private static DrabyelHubServiceCatalog.Service resolve(
            ServerLevel level,
            DrabyelHubServiceCatalog.Service base,
            java.util.Collection<DrabyelHubServiceCatalog.Service> alreadyResolved
    ) {
        var placement = DrabyelMapPlacementCatalog.placement(base.locator());
        if (placement == null) return null;

        Candidate best = null;
        for (var seed : placement.seeds()) {
            for (int[] offset : offsets(placement.searchRadius())) {
                int x = seed.x() + offset[0];
                int z = seed.z() + offset[1];
                if (DrabyelMapPlacementCatalog.excluded(placement, x, z)) continue;

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - placement.expectedY()) > 4) continue;
                if (!safeStandingBlock(level, x, y, z)) continue;
                if (nearResolvedService(alreadyResolved, base.locator(), x, z)) continue;

                double roadDistance = Math.sqrt(roadDistanceSq(x + 0.5D, z + 0.5D));
                if (roadDistance < 1.25D) continue;
                double seedDistanceSq = offset[0] * offset[0] + offset[1] * offset[1];
                double roadPenalty = Math.abs(roadDistance - placement.preferredRoadDistance()) * 6.0D;
                double heightPenalty = Math.abs(y - placement.expectedY()) * 2.0D;
                double score = seedDistanceSq + roadPenalty + heightPenalty;

                Candidate candidate = new Candidate(x, y, z, score);
                if (best == null || candidate.score() < best.score()
                        || (candidate.score() == best.score() && tieBreak(candidate, best) < 0)) {
                    best = candidate;
                }
            }
        }
        if (best == null) return null;

        float yaw = yawToward(
                best.x() + 0.5D,
                best.z() + 0.5D,
                placement.faceTarget().x() + 0.5D,
                placement.faceTarget().z() + 0.5D);

        return new DrabyelHubServiceCatalog.Service(
                base.locator(), base.role(), base.playerLabel(), base.facilityHint(), base.zone(), base.visualAsset(),
                new DrabyelHubServiceCatalog.Position(best.x(), best.y(), best.z()),
                yaw,
                base.interactionRadius(),
                true,
                true);
    }

    private static boolean safeStandingBlock(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos below = feet.below();
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return false;
        if (!level.getFluidState(below).isEmpty()) return false;

        for (int dy = 0; dy <= 2; dy++) {
            BlockPos space = feet.above(dy);
            if (!level.getBlockState(space).getCollisionShape(level, space).isEmpty()) return false;
            if (!level.getFluidState(space).isEmpty()) return false;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int nearby = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                min = Math.min(min, nearby);
                max = Math.max(max, nearby);
            }
        }
        if (max - min > 2) return false;

        // Explicit source exclusions already protect signs, traders, caches and map displays. Reject only
        // block entities in the actor's own standing column here; the former 5x5 blanket made real town frontage
        // unusable and caused service roles to fail closed during the first client playtest.
        for (int dy = -1; dy <= 2; dy++) {
            if (level.getBlockEntity(feet.offset(0, dy, 0)) != null) return false;
        }

        AABB crowd = new AABB(
                x - 1.25D, y - 1.0D, z - 1.25D,
                x + 2.25D, y + 3.0D, z + 2.25D);
        if (!level.getEntitiesOfClass(AbstractVillager.class, crowd).isEmpty()) return false;
        return true;
    }

    private static boolean nearResolvedService(
            java.util.Collection<DrabyelHubServiceCatalog.Service> resolved,
            String currentLocator,
            int x,
            int z
    ) {
        for (var service : resolved) {
            if (service == null || currentLocator.equals(service.locator()) || service.runtimePosition() == null) continue;
            double dx = x - service.runtimePosition().x();
            double dz = z - service.runtimePosition().z();
            if (dx * dx + dz * dz < MIN_SERVICE_SPACING_SQ) return true;
        }
        return false;
    }

    private static List<int[]> offsets(int radius) {
        List<int[]> out = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz <= radius * radius) out.add(new int[]{dx, dz});
            }
        }
        out.sort(Comparator.comparingInt(value -> value[0] * value[0] + value[1] * value[1]));
        return out;
    }

    private static double roadDistanceSq(double x, double z) {
        double best = Double.MAX_VALUE;
        for (RoadPoint point : ROAD) best = Math.min(best, distanceSq(x, z, point.x() + 0.5D, point.z() + 0.5D));
        return best;
    }

    private static double distanceSq(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return dx * dx + dz * dz;
    }

    private static int tieBreak(Candidate left, Candidate right) {
        int x = Integer.compare(left.x(), right.x());
        return x != 0 ? x : Integer.compare(left.z(), right.z());
    }

    private static float yawToward(double x, double z, double targetX, double targetZ) {
        double dx = targetX - x;
        double dz = targetZ - z;
        if (dx * dx + dz * dz <= 0.000001D) return 0.0F;
        return (float)Math.toDegrees(Math.atan2(-dx, dz));
    }
}
