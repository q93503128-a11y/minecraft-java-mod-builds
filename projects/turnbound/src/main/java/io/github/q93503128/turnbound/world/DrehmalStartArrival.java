package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * One-time escape from Drehmal's original 1.20.1 setup terminal into TURNBOUND's outdoor first-route topology.
 *
 * <p>The authored map is not edited. A safe surface block is selected at runtime between the source-backed Stasis
 * Facility and Primal Caverns anchors, preferring existing path/road blocks. Players who are already elsewhere are
 * never moved.</p>
 */
final class DrehmalStartArrival {
    static final String ARRIVAL_FLAG = "DREHMAL_NEW_DRABYEL_ENTRY_V2";
    static final String PRIMAL_CAVERNS = "turnbound:landmark/primal_caverns";
    private static final double LEGACY_SETUP_X = 26520.0;
    private static final double LEGACY_SETUP_Z = -136.0;
    private static final double LEGACY_SETUP_RADIUS_SQR = 220.0 * 220.0;

    private DrehmalStartArrival() {}

    static boolean moveOutOfLegacySetupIfNeeded(ServerPlayer player, ExternalWorldSavedData saved) {
        if (player == null || saved == null) return false;
        if (saved.onboardingFlag(player.getUUID(), ARRIVAL_FLAG)) return false;

        // Do not mark this complete just because login began elsewhere. Drehmal's original datapack can
        // teleport the host into the legacy setup terminal a tick or two after TURNBOUND's login hook.
        // The flag is written only after TURNBOUND actually performs the outdoor arrival.
        if (!legacySetupZone(player.getX(), player.getY(), player.getZ())) return false;

        BlockPos hub = DrehmalWorldBinding.hubSeed();
        BlockPos region = DrehmalWorldBinding.firstRegionSeed();

        ServerLevel level = (ServerLevel) player.level();
        BlockPos destination = findSafeHubArrival(level, hub, 36);
        if (destination == null) {
            Turnbound.LOGGER.warn("TURNBOUND could not find a safe New Drabyel arrival near {}, {}, {}", hub.getX(), hub.getY(), hub.getZ());
            return false;
        }

        double dx = region.getX() + 0.5D - (destination.getX() + 0.5D);
        double dz = region.getZ() + 0.5D - (destination.getZ() + 0.5D);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        boolean teleported = player.teleportTo(
                level,
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D,
                Set.of(),
                yaw,
                0.0F,
                true);
        if (!teleported) {
            Turnbound.LOGGER.warn("TURNBOUND failed to move player from Drehmal's legacy setup terminal");
            return false;
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        saved.markOnboardingFlag(player.getUUID(), ARRIVAL_FLAG);
        Turnbound.LOGGER.info(
                "TURNBOUND moved {} from the legacy Drehmal setup terminal to New Drabyel arrival {}, {}, {}",
                player.getUUID(), destination.getX(), destination.getY(), destination.getZ());
        return true;
    }

    static boolean legacySetupZone(double x, double y, double z) {
        double dx = x - LEGACY_SETUP_X;
        double dz = z - LEGACY_SETUP_Z;
        return y >= 120.0 && dx * dx + dz * dz <= LEGACY_SETUP_RADIUS_SQR;
    }

    private static BlockPos findSafeHubArrival(ServerLevel level, BlockPos seed, int radius) {
        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;

        // Prefer the authored hub's street-level neighborhood instead of a heightmap roof or a distant surface.
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq > radius * radius) continue;
                for (int dy = -10; dy <= 10; dy++) {
                    BlockPos feet = new BlockPos(seed.getX() + dx, seed.getY() + dy, seed.getZ() + dz);
                    if (!safeStandingColumn(level, feet)) continue;
                    int priority = surfacePriority(level.getBlockState(feet.below()));
                    long score = priority * 1_000_000L + Math.abs(dy) * 4_000L + distanceSq;
                    if (score < bestScore) {
                        bestScore = score;
                        best = feet;
                    }
                }
            }
        }
        if (best != null) return best;

        // Fallback only when the integration seed's local vertical band is obstructed after migration.
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq > radius * radius) continue;
                int x = seed.getX() + dx;
                int z = seed.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!safeStandingColumn(level, feet)) continue;
                long score = surfacePriority(level.getBlockState(feet.below())) * 1_000_000L + distanceSq;
                if (score < bestScore) {
                    bestScore = score;
                    best = feet;
                }
            }
        }
        return best;
    }

    private static boolean safeStandingColumn(ServerLevel level, BlockPos feet) {
        BlockPos groundPos = feet.below();
        BlockState ground = level.getBlockState(groundPos);
        if (ground.isAir() || ground.is(BlockTags.LEAVES) || !ground.getFluidState().isEmpty()) return false;
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        return body.getFluidState().isEmpty()
                && head.getFluidState().isEmpty()
                && body.getCollisionShape(level, feet).isEmpty()
                && head.getCollisionShape(level, feet.above()).isEmpty();
    }

    private static int surfacePriority(BlockState state) {
        if (state.is(Blocks.DIRT_PATH)) return 0;
        if (state.is(Blocks.GRAVEL)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.POLISHED_ANDESITE)
                || state.is(Blocks.OAK_PLANKS)
                || state.is(Blocks.SPRUCE_PLANKS)) return 1;
        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.STONE)) return 2;
        return 3;
    }
}
