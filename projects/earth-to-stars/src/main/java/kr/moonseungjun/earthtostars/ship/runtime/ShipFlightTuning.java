package kr.moonseungjun.earthtostars.ship.runtime;

public record ShipFlightTuning(
        double maxForwardSpeed,
        double maxReverseSpeed,
        double acceleration,
        double braking,
        double yawDegreesPerTick,
        double pitchDegreesPerTick,
        double maxPitchDegrees
) {
    public static final ShipFlightTuning P0 = new ShipFlightTuning(
            0.75D,
            0.25D,
            0.035D,
            0.055D,
            2.0D,
            1.5D,
            75.0D
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
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and > 0");
        }
    }
}
