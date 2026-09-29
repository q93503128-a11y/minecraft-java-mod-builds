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
 * <p>The authored map is not edited. The map-backed Capital Valley roadhead chooses the intended route zone, then
 * the live Minecraft 26.2 terrain chooses a nearby safe standing block. Players who are already elsewhere are never
 * moved.</p>
 */
final class DrehmalStartArrival {
    static final String ARRIVAL_FLAG = "DREHMAL_FIRST_ROUTE_ENTRY_V3";
    static final String LEGACY_DIRECT_HUB_FLAG = "DREHMAL_NEW_DRABYEL_ENTRY_V2";
    static final String PRIMAL_CAVERNS = "turnbound:landmark/primal_caverns";
    private static final String ROADHEAD_SITE = "turnbound:site/capital_valley/roadhead";
    private static final double LEGACY_SETUP_X = 26520.0;
    private static final double LEGACY_SETUP_Z = -136.0;
    private static final double LEGACY_SETUP_RADIUS_SQR = 220.0 * 220.0;

    private DrehmalStartArrival() {}

    static boolean moveOutOfLegacySetupIfNeeded(ServerPlayer player, ExternalWorldSavedData saved) {
        if (player == null || saved == null) return false;

        // Physical presence in Drehmal's setup terminal is stronger evidence than a previous arrival flag.
        // Older TURNBOUND builds sent some test saves directly to New Drabyel. Only the explicit V2
        // direct-arrival flag is migration provenance; normal HUB_REACHED progress must never move a player backward.
        boolean setupTerminal = legacySetupZone(player.getX(), player.getY(), player.getZ());
        boolean legacyHubArrival = DrehmalStartMigrationRules.shouldMigrateLegacyHubArrival(
                saved.onboardingFlag(player.getUUID(), ARRIVAL_FLAG),
                saved.onboardingFlag(player.getUUID(), LEGACY_DIRECT_HUB_FLAG),
                legacyHubZone(player.getX(), player.getZ()));
        if (!setupTerminal && !legacyHubArrival) return false;

        ServerLevel level = (ServerLevel) player.level();
        DrehmalMapPlacementCatalog.Placement roadhead = DrehmalMapPlacementCatalog.placement(ROADHEAD_SITE);
        if (roadhead == null || roadhead.siteSeeds().isEmpty()) {
            Turnbound.LOGGER.error("TURNBOUND first-route roadhead placement is missing");
            return false;
        }

        DrehmalMapPlacementCatalog.Seed seed = roadhead.siteSeeds().getFirst();
        int seedY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, seed.x(), seed.z());
        BlockPos destination = findSafeArrival(level, new BlockPos(seed.x(), seedY, seed.z()),
                Math.max(6, roadhead.searchRadius()));
        if (destination == null) {
            Turnbound.LOGGER.warn("TURNBOUND could not find a safe Capital Valley roadhead near {}, {}",
                    seed.x(), seed.z());
            return false;
        }

        DrehmalMapPlacementCatalog.Seed facingSeed = roadhead.siteSeeds().size() >= 2
                ? roadhead.siteSeeds().get(1)
                : seed;
        double dx = facingSeed.x() + 0.5D - (destination.getX() + 0.5D);
        double dz = facingSeed.z() + 0.5D - (destination.getZ() + 0.5D);
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
        if (legacyHubArrival) {
            saved.clearOnboardingFlag(player.getUUID(), DrehmalFirstRouteProgress.TOWER_REACHED);
            saved.clearOnboardingFlag(player.getUUID(), DrehmalFirstRouteProgress.CAMP_REACHED);
            saved.clearOnboardingFlag(player.getUUID(), DrehmalFirstRouteProgress.APPROACH_REACHED);
            saved.clearOnboardingFlag(player.getUUID(), DrehmalFirstRouteProgress.HUB_REACHED);
            saved.clearOnboardingFlag(player.getUUID(), DrehmalContextualOnboarding.HUB_MENU_VIEWED);
            saved.clearOnboardingFlag(player.getUUID(), DrehmalContextualOnboarding.HUB_ROUTE_REVIEWED);
        }
        saved.markOnboardingFlag(player.getUUID(), ARRIVAL_FLAG);
        Turnbound.LOGGER.info(
                "TURNBOUND moved {} into the Capital Valley first-route roadhead {}, {}, {}",
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

        // Prefer an authored road/path surface around the map-backed routehead seed.
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

        // Fallback only when the seed's local vertical band is obstructed after migration.
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
