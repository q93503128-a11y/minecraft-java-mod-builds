package kr.moonseungjun.titanbreak.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class AugmentedMobilityService {
    private static final double BASE_MAX_HORIZONTAL_SPEED = 1.15;

    private AugmentedMobilityService() {}

    public static void tick(ServerPlayer player, boolean driveActive, double compensation) {
        if (!driveActive) return;

        Vec3 movement = player.getDeltaMovement();
        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (horizontal <= 0.01) return;

        double desired = Math.min(BASE_MAX_HORIZONTAL_SPEED * compensation,
                Math.max(horizontal, horizontal * Math.min(1.35, compensation)));
        if (desired <= horizontal + 0.001) return;

        double factor = desired / horizontal;
        player.setDeltaMovement(movement.x * factor, movement.y, movement.z * factor);
        player.hurtMarked = true;
    }

    public static void clear(ServerPlayer player) {
        // P0 movement compensation is velocity-based and leaves no persistent modifier behind.
    }
}
