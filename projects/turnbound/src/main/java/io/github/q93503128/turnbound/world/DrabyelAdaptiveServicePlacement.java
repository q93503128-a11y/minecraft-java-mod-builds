package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Resolves New Drabyel service actors from pinned source-map seeds against the live migrated world.
 *
 * <p>This removes the need for a player to hand-place every service NPC. The source map chooses the semantic zone;
 * the live 26.2 server chooses the nearest standable, unobstructed block. Unsafe or conflicting services fail closed
 * instead of being forced into the town.</p>
 */
final class DrabyelAdaptiveServicePlacement {
    private static final Map<ServerLevel, Map<String, DrabyelHubServiceCatalog.Service>> CACHE = new IdentityHashMap<>();
    private static final double MIN_SERVICE_SPACING_SQ = 3.5D * 3.5D;

    private DrabyelAdaptiveServicePlacement() {}

    static synchronized List<DrabyelHubServiceCatalog.Service> productionServices(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return List.of();
        return List.copyOf(snapshot(level).values());
    }

    static synchronized DrabyelHubServiceCatalog.Service service(ServerPlayer player, String locator) {
        if (player == null || locator == null || !(player.level() instanceof ServerLevel level)) return null;
        return snapshot(level).get(locator);
    }

    static synchronized void clear() {
        CACHE.clear();
    }

    private static Map<String, DrabyelHubServiceCatalog.Service> snapshot(ServerLevel level) {
        return CACHE.computeIfAbsent(level, DrabyelAdaptiveServicePlacement::resolve);
    }

    private static Map<String, DrabyelHubServiceCatalog.Service> resolve(ServerLevel level) {
        Map<String, DrabyelHubServiceCatalog.Service> resolved = new LinkedHashMap<>();
        List<DrabyelHubServiceCatalog.Position> chosen = new ArrayList<>();

        // Explicitly verified catalog entries always win.
        for (var service : DrabyelHubServiceCatalog.productionServices()) {
            resolved.put(service.locator(), service);
            chosen.add(service.runtimePosition());
        }

        for (var authored : DrabyelHubServiceCatalog.hub().services()) {
            if (resolved.containsKey(authored.locator())) continue;
            var placement = DrabyelMapPlacementCatalog.placement(authored.locator());
            if (placement == null) continue;

            DrabyelHubServiceCatalog.Position position = resolvePosition(level, placement, chosen);
            if (position == null) {
                Turnbound.LOGGER.warn("TURNBOUND could not resolve a safe New Drabyel service position for {}",
                        authored.locator());
                continue;
            }

            float yaw = yawTo(position, placement.faceTarget());
            var runtime = new DrabyelHubServiceCatalog.Service(
                    authored.locator(),
                    authored.role(),
                    authored.playerLabel(),
                    authored.facilityHint(),
                    authored.zone(),
                    authored.visualAsset(),
                    position,
                    yaw,
                    authored.interactionRadius(),
                    true,
                    true);
            resolved.put(runtime.locator(), runtime);
            chosen.add(position);
        }

        Turnbound.LOGGER.info("TURNBOUND resolved New Drabyel services: {}/{}",
                resolved.size(), DrabyelHubServiceCatalog.hub().services().size());
        return Map.copyOf(resolved);
    }

    private static DrabyelHubServiceCatalog.Position resolvePosition(
            ServerLevel level,
            DrabyelMapPlacementCatalog.Placement placement,
            List<DrabyelHubServiceCatalog.Position> chosen
    ) {
        for (var seed : placement.seeds()) {
            for (int[] offset : offsets(placement.searchRadius())) {
                int x = seed.x() + offset[0];
                int z = seed.z() + offset[1];
                if (DrabyelMapPlacementCatalog.excluded(placement, x, z)) continue;

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!standing(level, feet) || crowded(level, feet) || tooCloseToService(chosen, x, z)) continue;

                return new DrabyelHubServiceCatalog.Position(x, y, z);
            }
        }
        return null;
    }

    private static boolean standing(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        if (level.getBlockState(below).isAir()
                || level.getBlockState(below).is(BlockTags.LEAVES)
                || !level.getFluidState(below).isEmpty()) {
            return false;
        }

        for (int dy = 0; dy <= 2; dy++) {
            BlockPos pos = feet.above(dy);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getFluidState(pos).isEmpty()) {
                return false;
            }
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int y = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        feet.getX() + dx,
                        feet.getZ() + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min <= 2;
    }

    private static boolean crowded(ServerLevel level, BlockPos feet) {
        double cx = feet.getX() + 0.5D;
        double cz = feet.getZ() + 0.5D;
        AABB body = new AABB(cx - 0.7D, feet.getY(), cz - 0.7D,
                cx + 0.7D, feet.getY() + 2.2D, cz + 0.7D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, body)) {
            if (entity instanceof ServerPlayer) continue;
            return true;
        }
        return false;
    }

    private static boolean tooCloseToService(List<DrabyelHubServiceCatalog.Position> chosen, int x, int z) {
        for (var position : chosen) {
            double dx = x - position.x();
            double dz = z - position.z();
            if (dx * dx + dz * dz < MIN_SERVICE_SPACING_SQ) return true;
        }
        return false;
    }

    static float yawTo(DrabyelHubServiceCatalog.Position position, DrabyelMapPlacementCatalog.Seed target) {
        if (position == null || target == null) return 0.0F;
        double dx = (target.x() + 0.5D) - (position.x() + 0.5D);
        double dz = (target.z() + 0.5D) - (position.z() + 0.5D);
        if (dx * dx + dz * dz <= 0.000001D) return 0.0F;
        return (float)Math.toDegrees(Math.atan2(-dx, dz));
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
}
