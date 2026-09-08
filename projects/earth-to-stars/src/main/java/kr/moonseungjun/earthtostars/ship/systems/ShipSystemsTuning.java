package kr.moonseungjun.earthtostars.ship.systems;

public record ShipSystemsTuning(
        double powerCapacity,
        double initialPower,
        double generationPerTick,
        double propulsionMaxPowerPerTick,
        double sensorScanPower,
        String primaryAmmoType,
        int primaryAmmoCapacity,
        int initialPrimaryAmmo,
        double sensorRange,
        int sensorIntervalTicks,
        int sensorStaleTicks
) {
    public static final ShipSystemsTuning P0 = new ShipSystemsTuning(
            100.0D,
            80.0D,
            4.0D,
            3.0D,
            0.5D,
            "autocannon_round",
            240,
            120,
            64.0D,
            10,
            30
    );

    public ShipSystemsTuning {
        requirePositive(powerCapacity, "powerCapacity");
        requireNonNegative(initialPower, "initialPower");
        requireNonNegative(generationPerTick, "generationPerTick");
        requireNonNegative(propulsionMaxPowerPerTick, "propulsionMaxPowerPerTick");
        requireNonNegative(sensorScanPower, "sensorScanPower");
        if (initialPower > powerCapacity) throw new IllegalArgumentException("initialPower exceeds capacity");
        if (primaryAmmoType == null || primaryAmmoType.isBlank()) throw new IllegalArgumentException("primaryAmmoType must not be blank");
        if (primaryAmmoCapacity <= 0) throw new IllegalArgumentException("primaryAmmoCapacity must be positive");
        if (initialPrimaryAmmo < 0 || initialPrimaryAmmo > primaryAmmoCapacity) throw new IllegalArgumentException("initialPrimaryAmmo must be within capacity");
        requirePositive(sensorRange, "sensorRange");
        if (sensorIntervalTicks <= 0) throw new IllegalArgumentException("sensorIntervalTicks must be positive");
        if (sensorStaleTicks < sensorIntervalTicks) throw new IllegalArgumentException("sensorStaleTicks must be >= sensorIntervalTicks");
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) throw new IllegalArgumentException(name + " must be finite and > 0");
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) throw new IllegalArgumentException(name + " must be finite and >= 0");
    }
}
