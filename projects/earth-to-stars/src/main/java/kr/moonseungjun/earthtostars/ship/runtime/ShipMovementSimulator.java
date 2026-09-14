package kr.moonseungjun.earthtostars.ship.runtime;

public final class ShipMovementSimulator {
    private static final double EPSILON = 1.0E-7D;

    private ShipMovementSimulator() {
    }

    public static ShipTransform step(ShipTransform current, ShipControlInput input, ShipFlightTuning tuning) {
        // Input semantics are explicit: positive yaw is left, positive lift is up.
        double yaw = wrapDegrees(current.yawDegrees() - input.yaw() * tuning.yawDegreesPerTick());
        double pitch = approach(current.pitchDegrees(), 0.0D, tuning.pitchDegreesPerTick());
        pitch = clamp(pitch, -tuning.maxPitchDegrees(), tuning.maxPitchDegrees());

        ShipTransform heading = new ShipTransform(current.position(), current.velocity(), yaw, 0.0D);
        ShipVec3 forward = heading.forward();
        ShipVec3 right = heading.right();

        double forwardSpeed = current.velocity().dot(forward);
        double lateralSpeed = current.velocity().dot(right);
        double verticalSpeed = current.velocity().y();

        if (Math.abs(input.throttle()) > EPSILON) {
            boolean reversingDirection = Math.abs(forwardSpeed) > EPSILON
                    && Math.signum(forwardSpeed) != Math.signum(input.throttle());
            double rate = reversingDirection ? tuning.braking() : tuning.acceleration();
            forwardSpeed += input.throttle() * rate;
        } else {
            forwardSpeed *= tuning.coastDrag();
        }
        forwardSpeed = clamp(forwardSpeed, -tuning.maxReverseSpeed(), tuning.maxForwardSpeed());

        // Turning does not erase momentum. Side-slip is damped progressively so the craft has weight
        // without feeling like an ice block.
        lateralSpeed *= tuning.lateralDamping();

        if (Math.abs(input.lift()) > EPSILON) {
            verticalSpeed += input.lift() * tuning.verticalAcceleration();
        } else {
            verticalSpeed *= tuning.verticalDamping();
        }
        verticalSpeed = clamp(verticalSpeed, -tuning.maxVerticalSpeed(), tuning.maxVerticalSpeed());

        if (Math.abs(forwardSpeed) < EPSILON) forwardSpeed = 0.0D;
        if (Math.abs(lateralSpeed) < EPSILON) lateralSpeed = 0.0D;
        if (Math.abs(verticalSpeed) < EPSILON) verticalSpeed = 0.0D;

        ShipVec3 velocity = forward.scale(forwardSpeed)
                .add(right.scale(lateralSpeed))
                .add(new ShipVec3(0.0D, verticalSpeed, 0.0D));
        ShipVec3 position = current.position().add(velocity);
        return new ShipTransform(position, velocity, yaw, pitch);
    }

    private static double approach(double current, double target, double maxDelta) {
        if (current < target) {
            return Math.min(current + maxDelta, target);
        }
        if (current > target) {
            return Math.max(current - maxDelta, target);
        }
        return current;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double wrapDegrees(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        } else if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }
}
