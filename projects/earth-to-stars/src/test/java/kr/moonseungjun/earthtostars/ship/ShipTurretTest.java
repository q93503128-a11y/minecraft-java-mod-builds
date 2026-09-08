package kr.moonseungjun.earthtostars.ship;

import kr.moonseungjun.earthtostars.ship.combat.SensorContact;
import kr.moonseungjun.earthtostars.ship.combat.ShipSensorGrid;
import kr.moonseungjun.earthtostars.ship.combat.TurretControlMode;
import kr.moonseungjun.earthtostars.ship.combat.TurretProfile;
import kr.moonseungjun.earthtostars.ship.combat.TurretRuntime;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class ShipTurretTest {
    @Test
    void manualLeaseIsExclusiveAndRejectsReplay() {
        UUID owner = UUID.randomUUID();
        UUID crew = UUID.randomUUID();
        ShipState ship = ship(owner);
        ship.assignRole(owner, crew, kr.moonseungjun.earthtostars.ship.domain.CrewRole.CREW);
        TurretRuntime turret = new TurretRuntime(ship, TurretProfile.P0_AUTOCANNON);
        turret.setMode(owner, TurretControlMode.MANUAL);

        UUID session = turret.requestManualControl(owner, 10L).orElseThrow();
        assertTrue(turret.requestManualControl(crew, 11L).isEmpty());
        assertTrue(turret.acceptManualAim(owner, session, 1L, new ShipVec3(0, 0, 1), 11L));
        assertFalse(turret.acceptManualAim(owner, session, 1L, new ShipVec3(1, 0, 0), 12L));
    }

    @Test
    void manualFireConsumesAmmoHonorsCooldownAndArc() {
        UUID owner = UUID.randomUUID();
        TurretRuntime turret = new TurretRuntime(ship(owner), TurretProfile.P0_AUTOCANNON);
        turret.setMode(owner, TurretControlMode.MANUAL);
        UUID session = turret.requestManualControl(owner, 0L).orElseThrow();
        assertTrue(turret.acceptManualAim(owner, session, 1L, new ShipVec3(0, 0, 1), 0L));

        int before = turret.ammo();
        assertTrue(turret.fireManual(owner, session, ShipVec3.ZERO, new ShipVec3(0, 0, 1), 0L).isPresent());
        assertEquals(before - 1, turret.ammo());
        assertTrue(turret.fireManual(owner, session, ShipVec3.ZERO, new ShipVec3(0, 0, 1), 1L).isEmpty());

        assertTrue(turret.acceptManualAim(owner, session, 2L, new ShipVec3(0, 0, -1), 5L));
        assertTrue(turret.fireManual(owner, session, ShipVec3.ZERO, new ShipVec3(0, 0, 1), 5L).isEmpty());
    }

    @Test
    void autoDefenseUsesSharedContactsAndIgnoresNeutralTargets() {
        UUID owner = UUID.randomUUID();
        TurretRuntime turret = new TurretRuntime(ship(owner), TurretProfile.P0_AUTOCANNON);
        turret.setMode(owner, TurretControlMode.AUTO_DEFENSE);
        ShipSensorGrid grid = new ShipSensorGrid();
        UUID neutral = UUID.randomUUID();
        UUID hostile = UUID.randomUUID();
        grid.update(List.of(
                new SensorContact(neutral, new ShipVec3(0, 0, 6), false, 100),
                new SensorContact(hostile, new ShipVec3(0, 0, 12), true, 10)
        ), 20L);

        var shot = turret.tickAuto(grid, ShipVec3.ZERO, new ShipVec3(0, 0, 1), 20L).orElseThrow();
        assertEquals(Optional.of(hostile), shot.targetId());
        assertEquals(2, grid.contactCount());
        assertEquals(20L, grid.lastScanTick());
    }

    @Test
    void leavingManualModeRevokesLease() {
        UUID owner = UUID.randomUUID();
        TurretRuntime turret = new TurretRuntime(ship(owner), TurretProfile.P0_AUTOCANNON);
        turret.setMode(owner, TurretControlMode.MANUAL);
        assertTrue(turret.requestManualControl(owner, 0L).isPresent());
        turret.setMode(owner, TurretControlMode.AUTO_DEFENSE);
        assertTrue(turret.lease().isEmpty());
    }

    private static ShipState ship(UUID owner) {
        return ShipState.create(
                ShipId.random(), owner,
                List.of(new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1))
        );
    }
}
