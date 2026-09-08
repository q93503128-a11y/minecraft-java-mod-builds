package kr.moonseungjun.earthtostars.ship;

import kr.moonseungjun.earthtostars.ship.domain.CrewRole;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightRuntime;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightTuning;
import kr.moonseungjun.earthtostars.ship.runtime.ShipMovementSimulator;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipMovementTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void orientationBasisIsNormalizedAndOrthogonal() {
        ShipTransform transform = new ShipTransform(ShipVec3.ZERO, ShipVec3.ZERO, 37.0D, -28.0D);

        assertEquals(1.0D, transform.forward().length(), EPSILON);
        assertEquals(1.0D, transform.right().length(), EPSILON);
        assertEquals(1.0D, transform.up().length(), EPSILON);
        assertEquals(0.0D, transform.forward().dot(transform.right()), EPSILON);
        assertEquals(0.0D, transform.forward().dot(transform.up()), EPSILON);
        assertEquals(0.0D, transform.right().dot(transform.up()), EPSILON);
    }

    @Test
    void movementAcceleratesBrakesAndClampsPitch() {
        ShipTransform transform = new ShipTransform(ShipVec3.ZERO, ShipVec3.ZERO, 0.0D, 0.0D);
        for (int i = 0; i < 100; i++) {
            transform = ShipMovementSimulator.step(transform, new ShipControlInput(1.0D, 0.0D, -1.0D), ShipFlightTuning.P0);
        }

        assertEquals(ShipFlightTuning.P0.maxForwardSpeed(), transform.velocity().length(), EPSILON);
        assertEquals(-ShipFlightTuning.P0.maxPitchDegrees(), transform.pitchDegrees(), EPSILON);

        double movingSpeed = transform.velocity().length();
        ShipTransform braking = ShipMovementSimulator.step(transform, ShipControlInput.ZERO, ShipFlightTuning.P0);
        assertTrue(braking.velocity().length() < movingSpeed);
    }

    @Test
    void leaseRejectsGuestsStaleSessionsAndReplayThenExpiresCleanly() {
        UUID owner = UUID.randomUUID();
        UUID crew = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        ShipState ship = newShip(owner);
        ship.assignRole(owner, crew, CrewRole.CREW);
        ShipFlightRuntime runtime = new ShipFlightRuntime(
                ship,
                new ShipTransform(ShipVec3.ZERO, ShipVec3.ZERO, 0.0D, 0.0D),
                ShipFlightTuning.P0
        );

        assertTrue(runtime.requestControl(guest, 10L).isEmpty());
        UUID ownerSession = runtime.requestControl(owner, 10L).orElseThrow();
        assertTrue(runtime.acceptInput(owner, ownerSession, 1L, new ShipControlInput(1.0D, 0.0D, 0.0D), 11L));
        assertFalse(runtime.acceptInput(owner, ownerSession, 1L, ShipControlInput.ZERO, 12L));
        assertFalse(runtime.acceptInput(crew, ownerSession, 2L, ShipControlInput.ZERO, 12L));

        runtime.tick(11L + ShipFlightRuntime.CONTROL_LEASE_TTL_TICKS);
        assertTrue(runtime.lease().isEmpty());
        assertFalse(runtime.acceptInput(owner, ownerSession, 2L, ShipControlInput.ZERO, 100L));

        UUID freshSession = runtime.requestControl(owner, 101L).orElseThrow();
        assertNotEquals(ownerSession, freshSession);
        assertTrue(runtime.releaseControl(owner));
        assertTrue(runtime.lease().isEmpty());
    }

    private static ShipState newShip(UUID owner) {
        return ShipState.create(ShipId.random(), owner, List.of(
                new ModuleSlot("core", ModuleSlotType.CORE, 1),
                new ModuleSlot("engine", ModuleSlotType.PROPULSION, 1),
                new ModuleSlot("power", ModuleSlotType.POWER, 1),
                new ModuleSlot("cargo", ModuleSlotType.CARGO, 1),
                new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1)
        ));
    }
}
