package kr.moonseungjun.riftfrontier.combat;

/**
 * Development-only targeting geometry for the Region 01 boss field harness.
 *
 * <p>The policy exists so the field actor can commit an attack lane toward an actual nearby player instead of
 * repeatedly attacking its spawn-facing direction. The yaw is selected only when a new authoritative attack begins;
 * callers must not continuously retarget during TELEGRAPH/ACTIVE/RECOVERY because that would invalidate the authored
 * sidestep/read-the-lane counterplay.</p>
 */
public final class Region01BossFieldAimPolicy {
    /** Provisional field-harness acquisition radius, not production aggro range. */
    public static final double TARGET_ACQUISITION_RADIUS = 24.0D;

    private Region01BossFieldAimPolicy() {}

    public static float committedYawDegrees(
        double attackerX,
        double attackerZ,
        double targetX,
        double targetZ
    ) {
        if (!Double.isFinite(attackerX) || !Double.isFinite(attackerZ)
            || !Double.isFinite(targetX) || !Double.isFinite(targetZ)) {
            throw new IllegalArgumentException("field-test aim coordinates must be finite");
        }
        double dx = targetX - attackerX;
        double dz = targetZ - attackerZ;
        if (Math.hypot(dx, dz) < 1.0E-6D) {
            throw new IllegalArgumentException("field-test aim target must not share the attacker's horizontal position");
        }
        return normalizeYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D));
    }

    public static boolean withinAcquisitionRadius(double squaredDistance) {
        if (!Double.isFinite(squaredDistance) || squaredDistance < 0.0D) {
            return false;
        }
        return squaredDistance <= TARGET_ACQUISITION_RADIUS * TARGET_ACQUISITION_RADIUS;
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        if (normalized >= 180.0F) normalized -= 360.0F;
        if (normalized < -180.0F) normalized += 360.0F;
        return normalized;
    }
}
