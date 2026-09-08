package kr.moonseungjun.earthtostars.ship.runtime;

public final class ShipMovementSimulator {
    private ShipMovementSimulator() {
    }

    public static ShipTransform step(ShipTransform current, ShipControlInput input, ShipFlightTuning tuning) {
        double yaw = wrapDegrees(current.yawDegrees() + input.yaw() * tuning.yawDegreesPerTick());
        double pitch = clamp(
                current.pitchDegrees() + input.pitch() * tuning.pitchDegreesPerTick(),
                -tuning.maxPitchDegrees(),
                tuning.maxPitchDegrees()
        );

        ShipTransform oriented = new ShipTransform(current.position(), current.velocity(), yaw, pitch);
        ShipVec3 forward = oriented.forward();
        double currentForwardSpeed = current.velocity().dot(forward);
        double targetSpeed = input.throttle() >= 0.0D
                ? input.throttle() * tuning.maxForwardSpeed()
                : input.throttle() * tuning.maxReverseSpeed();

        boolean braking = Math.signum(currentForwardSpeed) != Math.signum(targetSpeed)
                || Math.abs(targetSpeed) < Math.abs(currentForwardSpeed);
        double rate = braking ? tuning.braking() : tuning.acceleration();
        double nextSpeed = approach(currentForwardSpeed, targetSpeed, rate);
        if (Math.abs(nextSpeed) < 1.0E-7D && Math.abs(targetSpeed) < 1.0E-7D) {
            nextSpeed = 0.0D;
        }

        ShipVec3 velocity = forward.scale(nextSpeed);
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
