package kr.moonseungjun.earthtostars.ship.combat;

public record TurretProfile(
        String id,
        int ammoCapacity,
        int cooldownTicks,
        double range,
        double halfArcDegrees,
        double projectileSpeed,
        int projectileLifetimeTicks,
        float damage
) {
    public static final TurretProfile P0_AUTOCANNON = new TurretProfile(
            "autocannon_mk1", 120, 4, 48.0D, 85.0D, 3.0D, 24, 4.0F
    );

    public TurretProfile {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (ammoCapacity <= 0) throw new IllegalArgumentException("ammoCapacity must be positive");
        if (cooldownTicks <= 0) throw new IllegalArgumentException("cooldownTicks must be positive");
        if (!Double.isFinite(range) || range <= 0.0D) throw new IllegalArgumentException("range must be positive and finite");
        if (!Double.isFinite(halfArcDegrees) || halfArcDegrees <= 0.0D || halfArcDegrees > 180.0D) {
            throw new IllegalArgumentException("halfArcDegrees must be in (0, 180]");
        }
        if (!Double.isFinite(projectileSpeed) || projectileSpeed <= 0.0D) throw new IllegalArgumentException("projectileSpeed must be positive and finite");
        if (projectileLifetimeTicks <= 0) throw new IllegalArgumentException("projectileLifetimeTicks must be positive");
        if (!Float.isFinite(damage) || damage <= 0.0F) throw new IllegalArgumentException("damage must be positive and finite");
    }
}
