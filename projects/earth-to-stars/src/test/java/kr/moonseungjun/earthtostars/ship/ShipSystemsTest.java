package kr.moonseungjun.earthtostars.ship;

import kr.moonseungjun.earthtostars.ship.combat.TurretControlMode;
import kr.moonseungjun.earthtostars.ship.combat.TurretProfile;
import kr.moonseungjun.earthtostars.ship.combat.TurretRuntime;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import kr.moonseungjun.earthtostars.ship.systems.PowerPriority;
import kr.moonseungjun.earthtostars.ship.systems.ShipPowerGrid;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsRuntime;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsSnapshot;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsTuning;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class ShipSystemsTest {
    @Test
    void priorityReservePreventsLowerPriorityBrownout() {
        ShipPowerGrid grid = new ShipPowerGrid(100.0D, 45.0D, 0.0D);

        assertTrue(grid.tryConsume(5.0D, PowerPriority.UTILITY));
        assertEquals(40.0D, grid.stored(), 1.0E-9D);
        assertFalse(grid.tryConsume(0.1D, PowerPriority.UTILITY));

        assertTrue(grid.tryConsume(15.0D, PowerPriority.WEAPONS));
        assertEquals(25.0D, grid.stored(), 1.0E-9D);
        assertFalse(grid.tryConsume(0.1D, PowerPriority.WEAPONS));

        assertTrue(grid.tryConsume(15.0D, PowerPriority.PROPULSION));
        assertEquals(10.0D, grid.stored(), 1.0E-9D);
        assertFalse(grid.tryConsume(0.1D, PowerPriority.PROPULSION));

        assertTrue(grid.tryConsume(10.0D, PowerPriority.ESSENTIAL));
        assertEquals(0.0D, grid.stored(), 1.0E-9D);
    }

    @Test
    void generationIsAppliedOncePerServerTickAndCatchesUpMonotonically() {
        ShipPowerGrid grid = new ShipPowerGrid(100.0D, 10.0D, 4.0D);
        grid.beginTick(100L);
        assertEquals(10.0D, grid.stored(), 1.0E-9D);
        grid.beginTick(101L);
        assertEquals(14.0D, grid.stored(), 1.0E-9D);
        grid.beginTick(101L);
        assertEquals(14.0D, grid.stored(), 1.0E-9D);
        grid.beginTick(104L);
        assertEquals(26.0D, grid.stored(), 1.0E-9D);
        assertThrows(IllegalArgumentException.class, () -> grid.beginTick(103L));
    }

    @Test
    void twoTurretsConsumeOneSharedAmmoPool() {
        UUID owner = UUID.randomUUID();
        ShipState ship = ship(owner);
        ShipSystemsRuntime systems = ShipSystemsRuntime.p0(ship.shipId());
        TurretRuntime left = new TurretRuntime(ship, TurretProfile.P0_AUTOCANNON);
        TurretRuntime right = new TurretRuntime(ship, TurretProfile.P0_AUTOCANNON);
        left.setMode(owner, TurretControlMode.MANUAL);
        right.setMode(owner, TurretControlMode.MANUAL);
        UUID leftSession = left.requestManualControl(owner, 0L).orElseThrow();
        UUID rightSession = right.requestManualControl(owner, 0L).orElseThrow();
        assertTrue(left.acceptManualAim(owner, leftSession, 1L, new ShipVec3(0, 0, 1), 0L));
        assertTrue(right.acceptManualAim(owner, rightSession, 1L, new ShipVec3(0, 0, 1), 0L));

        int before = systems.ammoAmount(TurretProfile.P0_AUTOCANNON.ammoType());
        assertTrue(left.fireManual(owner, leftSession, systems, ShipVec3.ZERO, new ShipVec3(0, 0, 1), 0L).isPresent());
        assertTrue(right.fireManual(owner, rightSession, systems, ShipVec3.ZERO, new ShipVec3(0, 0, 1), 0L).isPresent());
        assertEquals(before - 2, systems.ammoAmount(TurretProfile.P0_AUTOCANNON.ammoType()));
    }

    @Test
    void failedWeaponTransactionConsumesNeitherAmmoNorPower() {
        ShipSystemsTuning lowPower = new ShipSystemsTuning(
                100.0D,
                1.0D,
                0.0D,
                3.0D,
                0.5D,
                "autocannon_round",
                10,
                5,
                64.0D,
                10,
                30
        );
        ShipSystemsRuntime systems = new ShipSystemsRuntime(ShipId.random(), lowPower);
        int ammoBefore = systems.ammoAmount(TurretProfile.P0_AUTOCANNON.ammoType());
        double powerBefore = systems.powerStored();

        assertFalse(systems.tryFire(TurretProfile.P0_AUTOCANNON));
        assertEquals(ammoBefore, systems.ammoAmount(TurretProfile.P0_AUTOCANNON.ammoType()));
        assertEquals(powerBefore, systems.powerStored(), 1.0E-9D);
    }

    @Test
    void propulsionDrawScalesWithInputAndIdleCostsNothing() {
        ShipSystemsRuntime systems = ShipSystemsRuntime.p0(ShipId.random());
        double start = systems.powerStored();
        assertTrue(systems.tryPowerPropulsion(ShipControlInput.ZERO));
        assertEquals(start, systems.powerStored(), 1.0E-9D);

        assertTrue(systems.tryPowerPropulsion(new ShipControlInput(0.5D, 0.0D, 0.0D)));
        assertEquals(start - ShipSystemsTuning.P0.propulsionMaxPowerPerTick() * 0.5D, systems.powerStored(), 1.0E-9D);
    }

    @Test
    void staleSensorContactsAreDropped() {
        ShipSystemsRuntime systems = ShipSystemsRuntime.p0(ShipId.random());
        systems.sensorGrid().update(List.of(
                new kr.moonseungjun.earthtostars.ship.combat.SensorContact(
                        UUID.randomUUID(), new ShipVec3(0, 0, 8), true, 10.0D
                )
        ), 10L);
        systems.sensorGrid().expireOlderThan(40L, 30L);
        assertEquals(1, systems.sensorGrid().contactCount());
        systems.sensorGrid().expireOlderThan(41L, 30L);
        assertEquals(0, systems.sensorGrid().contactCount());
    }

    @Test
    void systemsSnapshotRestoresPowerAndAmmoButNotSensorCache() {
        ShipId shipId = ShipId.random();
        String ammoType = ShipSystemsTuning.P0.primaryAmmoType();
        ShipSystemsSnapshot snapshot = new ShipSystemsSnapshot(
                shipId,
                37.5D,
                Map.of(ammoType, 73)
        );

        ShipSystemsRuntime restored = ShipSystemsRuntime.restore(snapshot, ShipSystemsTuning.P0);
        assertEquals(shipId, restored.shipId());
        assertEquals(37.5D, restored.powerStored(), 1.0E-9D);
        assertEquals(73, restored.ammoAmount(ammoType));
        assertEquals(0, restored.sensorGrid().contactCount());
        assertEquals(snapshot, restored.snapshot());
    }

    @Test
    void systemsSnapshotRejectsResourceStateOutsideCurrentCapacity() {
        ShipId shipId = ShipId.random();
        String ammoType = ShipSystemsTuning.P0.primaryAmmoType();
        ShipSystemsSnapshot tooMuchPower = new ShipSystemsSnapshot(
                shipId,
                ShipSystemsTuning.P0.powerCapacity() + 1.0D,
                Map.of(ammoType, 1)
        );
        ShipSystemsSnapshot tooMuchAmmo = new ShipSystemsSnapshot(
                shipId,
                1.0D,
                Map.of(ammoType, ShipSystemsTuning.P0.primaryAmmoCapacity() + 1)
        );

        assertThrows(IllegalArgumentException.class, () -> ShipSystemsRuntime.restore(tooMuchPower, ShipSystemsTuning.P0));
        assertThrows(IllegalArgumentException.class, () -> ShipSystemsRuntime.restore(tooMuchAmmo, ShipSystemsTuning.P0));
    }

    private static ShipState ship(UUID owner) {
        return ShipState.create(
                ShipId.random(),
                owner,
                List.of(new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1))
        );
    }
}
