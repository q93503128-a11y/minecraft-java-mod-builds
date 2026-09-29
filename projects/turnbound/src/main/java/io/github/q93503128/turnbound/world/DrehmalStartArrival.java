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
 * Safe entry into TURNBOUND's authored Capital Valley opening.
 *
 * <p>The old roadhead and later explorer-camp starts still made travel dominate the tutorial. The production entry
 * now begins at New Drabyel's source-backed north entrance. The first outdoor combat is a short excursion outside
 * the gate; older Capital Valley roadhead/tower/cave/camp content remains optional regional exploration.</p>
 */
final class DrehmalStartArrival {
    static final String ARRIVAL_FLAG = "DREHMAL_FIRST_ROUTE_ENTRY_V4";
    static final String LEGACY_DIRECT_HUB_FLAG = "DREHMAL_NEW_DRABYEL_ENTRY_V2";
    static final String PRIMAL_CAVERNS = "turnbound:landmark/primal_caverns";
    static final String ENTRY_SITE = "turnbound:site/capital_valley/new_drabyel";
    static final int ENTRY_SEED_INDEX = 2;
    private static final double LEGACY_SETUP_X = 26520.0;
    private static final double LEGACY_SETUP_Z = -136.0;
    private static final double LEGACY_SETUP_RADIUS_SQR = 220.0 * 220.0;

    private DrehmalStartArrival() {}

    static boolean moveOutOfLegacySetupIfNeeded(ServerPlayer player, ExternalWorldSavedData saved) {
        if (player == null || saved == null) return false;

        boolean setupTerminal = legacySetupZone(player.getX(), player.getY(), player.getZ());
        boolean currentArrival = saved.onboardingFlag(player.getUUID(), ARRIVAL_FLAG);
        boolean insideHub = legacyHubZone(player.getX(), player.getZ());
        boolean legacyHubArrival = DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(
                currentArrival,
                saved.onboardingFlag(player.getUUID(), LEGACY_DIRECT_HUB_FLAG),
                insideHub);
        boolean missingRouteArrival = DrehmalStartMigrationRules.shouldRepairMissingRouteArrival(currentArrival, insideHub);
        if (!setupTerminal && !legacyHubArrival && !missingRouteArrival) return false;
        return moveToEntry(player, saved, legacyHubArrival || missingRouteArrival);
    }

    static boolean forceFreshEntry(ServerPlayer player, ExternalWorldSavedData saved) {
        if (player == null || saved == null) return false;
        return moveToEntry(player, saved, true);
    }

    private static boolean moveToEntry(ServerPlayer player, ExternalWorldSavedData saved, boolean clearProgress) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        DrehmalMapPlacementCatalog.Placement entry = DrehmalMapPlacementCatalog.placement(ENTRY_SITE);
        if (entry == null || entry.siteSeeds().isEmpty()) {
            Turnbound.LOGGER.error("TURNBOUND opening entry placement is missing: {}", ENTRY_SITE);
            return false;
        }

        int seedIndex = Math.min(ENTRY_SEED_INDEX, entry.siteSeeds().size() - 1);
        DrehmalMapPlacementCatalog.Seed seed = entry.siteSeeds().get(seedIndex);
        int seedY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, seed.x(), seed.z());
        BlockPos destination = findSafeArrival(level, new BlockPos(seed.x(), seedY, seed.z()),
                Math.max(6, entry.searchRadius()));
        if (destination == null) {
            Turnbound.LOGGER.warn("TURNBOUND could not find a safe opening entry near {}, {}", seed.x(), seed.z());
            return false;
        }

        // Face from the entrance toward the town rather than away into the old tutorial road.
        DrehmalMapPlacementCatalog.Seed facingSeed = entry.siteSeeds().getFirst();
        double dx = facingSeed.x() + 0.5D - (destination.getX() + 0.5D);
        double dz = facingSeed.z() + 0.5D - (destination.getZ() + 0.5D);
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        boolean teleported = player.teleportTo(
                level,
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D,
                Set.of(),
                yaw,
                0.0F,
                true);
        if (!teleported) return false;

        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        if (clearProgress) {
            for (String flag : Set.of(
                    DrehmalFirstRouteProgress.TOWER_REACHED,
                    DrehmalFirstRouteProgress.CAMP_REACHED,
                    DrehmalFirstRouteProgress.APPROACH_REACHED,
                    DrehmalFirstRouteProgress.HUB_REACHED,
                    DrehmalContextualOnboarding.HUB_MENU_VIEWED,
                    DrehmalContextualOnboarding.HUB_ROUTE_REVIEWED,
                    ARRIVAL_FLAG,
                    LEGACY_DIRECT_HUB_FLAG)) {
                saved.clearOnboardingFlag(player.getUUID(), flag);
            }
        }
        saved.markOnboardingFlag(player.getUUID(), ARRIVAL_FLAG);
        Turnbound.LOGGER.info("TURNBOUND moved {} to opening entry {}, {}, {}",
                player.getUUID(), destination.getX(), destination.getY(), destination.getZ());
        return true;
    }

    private static boolean legacyHubZone(double x, double z) {
        var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        if (hub == null) return false;
        double dx = x - (hub.x() + 0.5D);
        double dz = z - (hub.z() + 0.5D);
        return dx * dx + dz * dz <= 96.0D * 96.0D;
    }

    static boolean legacySetupZone(double x, double y, double z) {
        double dx = x - LEGACY_SETUP_X;
        double dz = z - LEGACY_SETUP_Z;
        return y >= 120.0 && dx * dx + dz * dz <= LEGACY_SETUP_RADIUS_SQR;
    }

    private static BlockPos findSafeArrival(ServerLevel level, BlockPos seed, int radius) {
        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq > radius * radius) continue;
                for (int dy = -10; dy <= 10; dy++) {
                    BlockPos feet = new BlockPos(seed.getX() + dx, seed.getY() + dy, seed.getZ() + dz);
                    if (!safeStandingColumn(level, feet)) continue;
                    if (!DrehmalAdaptiveRoutePlacement.sourceContentClear(
                            level, feet.getX(), feet.getY(), feet.getZ(), 3.0D)) continue;
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

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq > radius * radius) continue;
                int x = seed.getX() + dx;
                int z = seed.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!safeStandingColumn(level, feet)) continue;
                if (!DrehmalAdaptiveRoutePlacement.sourceContentClear(level, x, y, z, 3.0D)) continue;
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
