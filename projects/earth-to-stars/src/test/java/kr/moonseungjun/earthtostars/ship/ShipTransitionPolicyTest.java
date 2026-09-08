package kr.moonseungjun.earthtostars.ship;

import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransitionPolicy;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipTransitionPolicyTest {
    @Test
    void upwardEarthBoundaryEntersOrbit() {
        ShipTransform transform = new ShipTransform(
                new ShipVec3(12.0D, ShipTransitionPolicy.EARTH_EXIT_ALTITUDE, -8.0D),
                new ShipVec3(0.2D, 0.1D, 0.3D),
                45.0D,
                -12.0D
        );
        assertEquals(
                ShipTransitionPolicy.Transition.EARTH_TO_ORBIT,
                ShipTransitionPolicy.evaluate(true, false, transform)
        );
        ShipTransform destination = ShipTransitionPolicy.destination(ShipTransitionPolicy.Transition.EARTH_TO_ORBIT, transform);
        assertEquals(ShipTransitionPolicy.ORBIT_ENTRY_ALTITUDE, destination.position().y());
        assertEquals(transform.position().x(), destination.position().x());
        assertEquals(transform.position().z(), destination.position().z());
        assertEquals(transform.yawDegrees(), destination.yawDegrees());
        assertEquals(transform.pitchDegrees(), destination.pitchDegrees());
    }

    @Test
    void crossingBoundaryInWrongDirectionDoesNotTransition() {
        ShipTransform transform = new ShipTransform(
                new ShipVec3(0.0D, ShipTransitionPolicy.EARTH_EXIT_ALTITUDE + 5.0D, 0.0D),
                new ShipVec3(0.0D, -0.1D, 0.0D),
                0.0D,
                0.0D
        );
        assertEquals(ShipTransitionPolicy.Transition.NONE, ShipTransitionPolicy.evaluate(true, false, transform));
    }

    @Test
    void descendingOrbitalBoundaryReturnsToEarth() {
        ShipTransform transform = new ShipTransform(
                new ShipVec3(3.0D, ShipTransitionPolicy.ORBIT_RETURN_ALTITUDE, 4.0D),
                new ShipVec3(0.0D, -0.2D, 0.0D),
                180.0D,
                20.0D
        );
        assertEquals(
                ShipTransitionPolicy.Transition.ORBIT_TO_EARTH,
                ShipTransitionPolicy.evaluate(false, true, transform)
        );
        ShipTransform destination = ShipTransitionPolicy.destination(ShipTransitionPolicy.Transition.ORBIT_TO_EARTH, transform);
        assertEquals(ShipTransitionPolicy.EARTH_REENTRY_ALTITUDE, destination.position().y());
    }
}
