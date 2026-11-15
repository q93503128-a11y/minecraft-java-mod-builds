package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Shared deterministic movement contract for the real inhabited original Mob.
 * Input packets contain keys and angles only; clients never send positions,
 * velocities, teleport coordinates or attack targets.
 */
public final class ParasiteMotionRules {
    private static final int KEY_MASK = 63;
    private static final int MAX_INPUT_AGE = 10;
    private ParasiteMotionRules() {}

    public static boolean validInput(int mask, float yaw, float pitch) {
        return mask >= 0 && (mask & ~KEY_MASK) == 0
                && Float.isFinite(yaw) && Float.isFinite(pitch)
                && pitch >= -90.0F && pitch <= 90.0F;
    }

    /** Never resurrect an old input packet after a tick counter reset. */
    public static boolean fresh(long now, long lastInput) {
        return lastInput >= 0 && now >= lastInput && now - lastInput <= MAX_INPUT_AGE;
    }

    public static float yaw(float yaw) {
        return Mth.wrapDegrees(yaw);
    }

    public static boolean sprint(int mask) {
        return (mask & 32) != 0 && (mask & 1) != 0 && (mask & 2) == 0;
    }

    /** 2D unit input; diagonal movement is not 41% faster than straight. */
    public static Vec3 planar(int mask, float yaw) {
        double forward = ((mask & 1) != 0 ? 1 : 0) - ((mask & 2) != 0 ? 1 : 0);
        double strafe = ((mask & 8) != 0 ? 1 : 0) - ((mask & 4) != 0 ? 1 : 0);
        double length = Math.hypot(forward, strafe);
        if (length < 1.0e-8) return Vec3.ZERO;
        forward /= length;
        strafe /= length;
        double rad = Math.toRadians(yaw);
        return new Vec3(-Math.sin(rad) * forward + Math.cos(rad) * strafe,
                0.0,
                Math.cos(rad) * forward + Math.sin(rad) * strafe);
    }

    /** Positive pitch means look down, negative pitch means swim upward. */
    public static double swimVertical(float pitch, double speed) {
        return Mth.clamp(-Math.sin(Math.toRadians(pitch)) * speed, -0.24, 0.24);
    }
}
