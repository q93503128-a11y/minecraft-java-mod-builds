package kr.moonseungjun.earthtostars.ship.runtime;

public record ShipControlInput(double throttle, double yaw, double pitch) {
    public static final ShipControlInput ZERO = new ShipControlInput(0.0D, 0.0D, 0.0D);

    public ShipControlInput {
        requireAxis(throttle, "throttle");
        requireAxis(yaw, "yaw");
        requireAxis(pitch, "pitch");
    }

    private static void requireAxis(double value, String name) {
        if (!Double.isFinite(value) || value < -1.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and within [-1, 1]");
        }
    }
}
