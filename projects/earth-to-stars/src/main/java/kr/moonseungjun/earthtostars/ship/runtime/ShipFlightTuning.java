package kr.moonseungjun.earthtostars.ship.runtime;

public record ShipFlightTuning(
        double maxForwardSpeed,
        double maxReverseSpeed,
        double acceleration,
        double braking,
        double yawDegreesPerTick,
        double pitchDegreesPerTick,
        double maxPitchDegrees,
        double maxVerticalSpeed,
        double verticalAcceleration,
        double coastDrag,
        double lateralDamping,
        double verticalDamping
) {
    public static final ShipFlightTuning P0 = new ShipFlightTuning(
            0.62D,
            0.22D,
            0.025D,
            0.055D,
            1.6D,
            2.5D,
            20.0D,
            0.38D,
            0.032D,
            0.965D,
            0.84D,
            0.90D
    );

    public ShipFlightTuning {
        requirePositive(maxForwardSpeed, "maxForwardSpeed");
        requirePositive(maxReverseSpeed, "maxReverseSpeed");
        requirePositive(acceleration, "acceleration");
        requirePositive(braking, "braking");
        requirePositive(yawDegreesPerTick, "yawDegreesPerTick");
        requirePositive(pitchDegreesPerTick, "pitchDegreesPerTick");
        if (!Double.isFinite(maxPitchDegrees) || maxPitchDegrees <= 0.0D || maxPitchDegrees > 89.0D) {
            throw new IllegalArgumentException("maxPitchDegrees must be in (0, 89]");
        }
        requirePositive(maxVerticalSpeed, "maxVerticalSpeed");
        requirePositive(verticalAcceleration, "verticalAcceleration");
        requireUnitInterval(coastDrag, "coastDrag");
        requireUnitInterval(lateralDamping, "lateralDamping");
        requireUnitInterval(verticalDamping, "verticalDamping");
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and > 0");
        }
    }

    private static void requireUnitInterval(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and within (0, 1]");
        }
    }
}
