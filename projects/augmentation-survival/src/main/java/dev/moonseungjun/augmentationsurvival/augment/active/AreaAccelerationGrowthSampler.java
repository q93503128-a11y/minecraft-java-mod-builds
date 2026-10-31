package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Smooths Area Acceleration crop growth across time instead of multiplying only the rare vanilla
 * random-tick event that happened to select a crop. The long-run configured multiplier is preserved.
 */
public final class AreaAccelerationGrowthSampler {
    private static final String AREA_ACCELERATION_ID = AugmentationSurvival.MOD_ID + ":area_acceleration";
    private static final int SECTION_VOLUME = 16 * 16 * 16;
    private static final List<BlockPos> SPHERE_OFFSETS = buildSphereOffsets();
    private static final Map<UUID, CursorState> CURSORS = new HashMap<>();
    private static boolean initialized;

    private AreaAccelerationGrowthSampler() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(AreaAccelerationGrowthSampler::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CURSORS.clear());
    }

    public static int spherePointCount() {
        return SPHERE_OFFSETS.size();
    }

    public static double exactExtraSamplesPerTick(int multiplier, int randomTickSpeed) {
        if (multiplier <= 1 || randomTickSpeed <= 0) return 0.0D;
        return SPHERE_OFFSETS.size() / (double) SECTION_VOLUME
                * randomTickSpeed * (multiplier - 1);
    }

    private static void tick(MinecraftServer server) {
        Set<UUID> active = new HashSet<>();
        for (ServerPlayer owner : server.getPlayerList().getPlayers()) {
            if (AreaAccelerationService.remainingTicks(owner) <= 0) continue;
            int level = AugmentationPlayerStateService.state(owner).level(AREA_ACCELERATION_ID);
            int multiplier = AreaAccelerationService.multiplierForLevel(level);
            if (multiplier <= 1) continue;

            active.add(owner.getUUID());
            ServerLevel levelWorld = owner.level();
            int randomTickSpeed = levelWorld.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
            if (randomTickSpeed <= 0) continue;

            CursorState cursor = CURSORS.computeIfAbsent(owner.getUUID(), ignored -> new CursorState());
            double exact = exactExtraSamplesPerTick(multiplier, randomTickSpeed) + cursor.credit;
            int samples = (int) Math.floor(exact);
            cursor.credit = exact - samples;
            BlockPos center = owner.blockPosition();

            for (int sample = 0; sample < samples; sample++) {
                BlockPos offset = SPHERE_OFFSETS.get(cursor.nextIndex());
                BlockPos pos = center.offset(offset);
                if (pos.getY() < levelWorld.getMinY() || pos.getY() >= levelWorld.getMaxY()) continue;
                if (!levelWorld.hasChunkAt(pos)) continue;
                if (!isPrimaryOwner(server, owner, pos, multiplier)) continue;

                BlockState state = levelWorld.getBlockState(pos);
                if (!AreaAccelerationService.isCompatibleGrowthBlock(state)) continue;
                state.randomTick(levelWorld, pos, levelWorld.getRandom());
            }
        }
        CURSORS.keySet().removeIf(uuid -> !active.contains(uuid));
    }

    /** Highest multiplier wins in overlaps; equal-strength fields use UUID order so they never stack. */
    private static boolean isPrimaryOwner(
            MinecraftServer server,
            ServerPlayer candidate,
            BlockPos pos,
            int candidateMultiplier
    ) {
        double radiusSq = AreaAccelerationService.radius() * (double) AreaAccelerationService.radius();
        Vec3 point = Vec3.atCenterOf(pos);
        UUID winner = candidate.getUUID();
        int winnerMultiplier = candidateMultiplier;

        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == candidate || other.level() != candidate.level()) continue;
            if (AreaAccelerationService.remainingTicks(other) <= 0) continue;
            if (other.position().distanceToSqr(point) > radiusSq) continue;
            int otherLevel = AugmentationPlayerStateService.state(other).level(AREA_ACCELERATION_ID);
            int otherMultiplier = AreaAccelerationService.multiplierForLevel(otherLevel);
            if (otherMultiplier > winnerMultiplier
                    || (otherMultiplier == winnerMultiplier
                    && other.getUUID().compareTo(winner) < 0)) {
                winner = other.getUUID();
                winnerMultiplier = otherMultiplier;
            }
        }
        return winner.equals(candidate.getUUID());
    }

    private static List<BlockPos> buildSphereOffsets() {
        int radius = AreaAccelerationService.radius();
        int radiusSq = radius * radius;
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
                for (int x = -radius; x <= radius; x++) {
                    if (x * x + y * y + z * z <= radiusSq) offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        Collections.shuffle(offsets, new Random(0xA66E1E5EL));
        return List.copyOf(offsets);
    }

    private static final class CursorState {
        private int index;
        private double credit;

        private int nextIndex() {
            int current = index;
            index = (index + 1) % SPHERE_OFFSETS.size();
            return current;
        }
    }
}
