package kr.moonseungjun.earthtostars.ship.gameplay;

import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class LaunchReadinessPolicyTest {
    @Test
    void atmosphereBandsProgressWithoutUsingMountainHeightAsOrbit() {
        assertEquals(LaunchReadinessPolicy.AtmosphereBand.DENSE, LaunchReadinessPolicy.band(true, false, 255.0D));
        assertEquals(LaunchReadinessPolicy.AtmosphereBand.THIN, LaunchReadinessPolicy.band(true, false, 256.0D));
        assertEquals(LaunchReadinessPolicy.AtmosphereBand.UPPER, LaunchReadinessPolicy.band(true, false, 384.0D));
        assertEquals(LaunchReadinessPolicy.AtmosphereBand.ORBIT, LaunchReadinessPolicy.band(false, true, 128.0D));
        assertEquals(512.0D, LaunchReadinessPolicy.EARTH_EXIT_ALTITUDE, 1.0E-9D);
    }

    @Test
    void upperAtmosphereCostsMorePropellantThanDenseAirAndIdleCostsNothing() {
        ShipControlInput full = new ShipControlInput(1.0D, 0.0D, 0.0D);
        double dense = LaunchReadinessPolicy.propellantPerTick(LaunchReadinessPolicy.AtmosphereBand.DENSE, full);
        double upper = LaunchReadinessPolicy.propellantPerTick(LaunchReadinessPolicy.AtmosphereBand.UPPER, full);
        assertTrue(upper > dense);
        assertEquals(0.0D, LaunchReadinessPolicy.propellantPerTick(LaunchReadinessPolicy.AtmosphereBand.UPPER, ShipControlInput.ZERO), 1.0E-9D);
    }

    @Test
    void oxygenOnlyBecomesMeaningfulAsAtmosphereThins() {
        assertEquals(0.0D, LaunchReadinessPolicy.oxygenPerTick(LaunchReadinessPolicy.AtmosphereBand.DENSE, 1), 1.0E-9D);
        assertTrue(LaunchReadinessPolicy.oxygenPerTick(LaunchReadinessPolicy.AtmosphereBand.THIN, 1) > 0.0D);
        assertTrue(LaunchReadinessPolicy.oxygenPerTick(LaunchReadinessPolicy.AtmosphereBand.ORBIT, 2)
                > LaunchReadinessPolicy.oxygenPerTick(LaunchReadinessPolicy.AtmosphereBand.ORBIT, 1));
    }

    @Test
    void orbitReserveRequiresFuelOxygenAndLifeSupport() {
        assertTrue(LaunchReadinessPolicy.hasOrbitReserve(
                LaunchReadinessPolicy.MIN_ORBIT_PROPELLANT,
                LaunchReadinessPolicy.MIN_ORBIT_OXYGEN,
                true
        ));
        assertFalse(LaunchReadinessPolicy.hasOrbitReserve(
                LaunchReadinessPolicy.MIN_ORBIT_PROPELLANT - 0.01D,
                LaunchReadinessPolicy.MIN_ORBIT_OXYGEN,
                true
        ));
        assertFalse(LaunchReadinessPolicy.hasOrbitReserve(
                LaunchReadinessPolicy.MIN_ORBIT_PROPELLANT,
                LaunchReadinessPolicy.MIN_ORBIT_OXYGEN - 0.01D,
                true
        ));
        assertFalse(LaunchReadinessPolicy.hasOrbitReserve(100.0D, 100.0D, false));
    }
}
