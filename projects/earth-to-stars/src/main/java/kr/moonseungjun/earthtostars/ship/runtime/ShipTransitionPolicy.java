package kr.moonseungjun.earthtostars.ship.runtime;

import java.util.Objects;

public final class ShipTransitionPolicy {
    public static final double EARTH_EXIT_ALTITUDE = 300.0D;
    public static final double ORBIT_RETURN_ALTITUDE = 16.0D;
    public static final double ORBIT_ENTRY_ALTITUDE = 128.0D;
    public static final double EARTH_REENTRY_ALTITUDE = 296.0D;
    private static final double MIN_VERTICAL_TRANSITION_SPEED = 0.01D;

    private ShipTransitionPolicy() {
    }

    public static Transition evaluate(boolean inEarth, boolean inOrbit, ShipTransform transform) {
        Objects.requireNonNull(transform, "transform");
        if (inEarth
                && transform.position().y() >= EARTH_EXIT_ALTITUDE
                && transform.velocity().y() > MIN_VERTICAL_TRANSITION_SPEED) {
            return Transition.EARTH_TO_ORBIT;
        }
        if (inOrbit
                && transform.position().y() <= ORBIT_RETURN_ALTITUDE
                && transform.velocity().y() < -MIN_VERTICAL_TRANSITION_SPEED) {
            return Transition.ORBIT_TO_EARTH;
        }
        return Transition.NONE;
    }

    public static ShipTransform destination(Transition transition, ShipTransform current) {
        Objects.requireNonNull(transition, "transition");
        Objects.requireNonNull(current, "current");
        if (transition == Transition.NONE) {
            return current;
        }
        double y = transition == Transition.EARTH_TO_ORBIT ? ORBIT_ENTRY_ALTITUDE : EARTH_REENTRY_ALTITUDE;
        ShipVec3 position = new ShipVec3(current.position().x(), y, current.position().z());
        ShipVec3 velocity = new ShipVec3(
                current.velocity().x(),
                Math.copySign(Math.min(Math.abs(current.velocity().y()), 0.35D), current.velocity().y()),
                current.velocity().z()
        );
        return new ShipTransform(position, velocity, current.yawDegrees(), current.pitchDegrees());
    }

    public enum Transition {
        NONE,
        EARTH_TO_ORBIT,
        ORBIT_TO_EARTH
    }
}
