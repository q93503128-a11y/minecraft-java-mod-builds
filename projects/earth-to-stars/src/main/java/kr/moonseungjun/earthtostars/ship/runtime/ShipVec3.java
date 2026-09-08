package kr.moonseungjun.earthtostars.ship.runtime;

public record ShipVec3(double x, double y, double z) {
    public static final ShipVec3 ZERO = new ShipVec3(0.0D, 0.0D, 0.0D);

    public ShipVec3 {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public ShipVec3 add(ShipVec3 other) {
        return new ShipVec3(x + other.x, y + other.y, z + other.z);
    }

    public ShipVec3 scale(double scalar) {
        requireFinite(scalar, "scalar");
        return new ShipVec3(x * scalar, y * scalar, z * scalar);
    }

    public double dot(ShipVec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public ShipVec3 cross(ShipVec3 other) {
        return new ShipVec3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    public double lengthSquared() {
        return dot(this);
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public ShipVec3 normalized() {
        double length = length();
        return length < 1.0E-12D ? ZERO : scale(1.0D / length);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
