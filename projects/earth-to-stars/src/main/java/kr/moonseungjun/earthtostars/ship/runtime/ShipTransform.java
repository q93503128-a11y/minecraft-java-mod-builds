package kr.moonseungjun.earthtostars.ship.runtime;

import java.util.Objects;

public record ShipTransform(ShipVec3 position, ShipVec3 velocity, double yawDegrees, double pitchDegrees) {
    public ShipTransform {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(velocity, "velocity");
        if (!Double.isFinite(yawDegrees) || !Double.isFinite(pitchDegrees)) {
            throw new IllegalArgumentException("ship rotation must be finite");
        }
        if (pitchDegrees < -89.0D || pitchDegrees > 89.0D) {
            throw new IllegalArgumentException("pitch must be within [-89, 89]");
        }
    }

    public ShipVec3 forward() {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cosPitch = Math.cos(pitch);
        return new ShipVec3(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch
        ).normalized();
    }

    public ShipVec3 right() {
        double yaw = Math.toRadians(yawDegrees);
        return new ShipVec3(Math.cos(yaw), 0.0D, Math.sin(yaw)).normalized();
    }

    public ShipVec3 up() {
        return forward().cross(right()).normalized();
    }
}
