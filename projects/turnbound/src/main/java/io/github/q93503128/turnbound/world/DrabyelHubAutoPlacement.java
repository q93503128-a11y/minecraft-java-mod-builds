package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Deterministic New Drabyel service placement resolved from the actual bound 26.2 world.
 *
 * <p>The source anchors come from the public Drehmal map extraction (signs/traders/paths), but the final Y and
 * exact adjacent standing block are chosen from the live world. Candidates that collide with source block entities,
 * liquids, unstable ground or existing villagers are rejected. Nothing here writes terrain.</p>
 */
final class DrabyelHubAutoPlacement {
    private record Anchor(String role, int x, int y, int z, int radius, double preferredRoadDistance) {}
    private record RoadPoint(int x, int z) {}
    private record Candidate(int x, int y, int z, double score) {}

    private static final Map<String, Anchor> ANCHORS = Map.of(
            "GREETER", new Anchor("GREETER", 502, 69, 1804, 8, 3.5D),
            "TRAVEL", new Anchor("TRAVEL", 506, 67, 1836, 9, 3.5D),
            "MARKET", new Anchor("MARKET", 536, 67, 1834, 10, 4.0D),
            "BLACKSMITH", new Anchor("BLACKSMITH", 526, 65, 1839, 8, 4.0D),
            "STORY", new Anchor("STORY", 520, 67, 1823, 9, 4.0D),
            "SUMMON", new Anchor("SUMMON", 511, 66, 1850, 8, 5.0D)
    );

    /**
     * Simplified centerline from the public Drehmal 2.2 road/path GeoJSON around New Drabyel.
     * It is used only to keep service actors beside the road rather than standing in its middle.
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
                out.add(staticService);
                continue;
            }
            DrabyelHubServiceCatalog.Service resolved = cache.get(base.locator());
            if (resolved == null && gameTime >= retryAt.getOrDefault(base.locator(), Long.MIN_VALUE)) {
                resolved = resolve(level, base);
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
        return Set.copyOf(ANCHORS.keySet());
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
            DrabyelHubServiceCatalog.Service base
    ) {
        Anchor anchor = ANCHORS.get(base.role());
        if (anchor == null) return null;

        Candidate best = null;
        for (int dx = -anchor.radius(); dx <= anchor.radius(); dx++) {
            for (int dz = -anchor.radius(); dz <= anchor.radius(); dz++) {
                int x = anchor.x() + dx;
                int z = anchor.z() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - anchor.y()) > 2) continue;
                if (!safeStandingBlock(level, x, y, z)) continue;

                double roadDistance = Math.sqrt(roadDistanceSq(x + 0.5D, z + 0.5D));
                if (roadDistance < 2.0D) continue; // never stand in the authored road centerline
                double anchorDistanceSq = dx * dx + dz * dz;
                double roadPenalty = Math.abs(roadDistance - anchor.preferredRoadDistance()) * 6.0D;
                double heightPenalty = Math.abs(y - anchor.y()) * 2.0D;
                double score = anchorDistanceSq + roadPenalty + heightPenalty;
                Candidate candidate = new Candidate(x, y, z, score);
                if (best == null || candidate.score() < best.score()
                        || (candidate.score() == best.score() && tieBreak(candidate, best) < 0)) {
                    best = candidate;
                }
            }
        }
        if (best == null) return null;

        RoadPoint facing = ROAD.stream()
                .min(Comparator.comparingDouble(point -> distanceSq(best.x() + 0.5D, best.z() + 0.5D, point.x(), point.z())))
                .orElse(new RoadPoint(anchor.x(), anchor.z()));
        float yaw = yawToward(best.x() + 0.5D, best.z() + 0.5D, facing.x() + 0.5D, facing.z() + 0.5D);

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
        if (max - min > 1) return false;

        // Preserve original authored interactions: signs, chests, lecterns, caches and other block entities.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (level.getBlockEntity(feet.offset(dx, dy, dz)) != null) return false;
                }
            }
        }

        AABB crowd = new AABB(
                x - 2.25D, y - 1.0D, z - 2.25D,
                x + 3.25D, y + 3.0D, z + 3.25D);
        if (!level.getEntitiesOfClass(Villager.class, crowd).isEmpty()) return false;
        if (!level.getEntitiesOfClass(WanderingTrader.class, crowd).isEmpty()) return false;
        return true;
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
