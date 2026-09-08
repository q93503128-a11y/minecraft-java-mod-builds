package kr.moonseungjun.earthtostars.ship.systems;

import kr.moonseungjun.earthtostars.ship.combat.ShipSensorGrid;
import kr.moonseungjun.earthtostars.ship.combat.TurretProfile;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;

import java.util.Map;
import java.util.Objects;

public final class ShipSystemsRuntime {
    private static final double AXIS_EPSILON = 1.0E-6D;

    private final ShipId shipId;
    private final ShipSystemsTuning tuning;
    private final ShipPowerGrid powerGrid;
    private final ShipAmmoPool ammoPool;
    private final ShipSensorGrid sensorGrid = new ShipSensorGrid();

    public ShipSystemsRuntime(ShipId shipId, ShipSystemsTuning tuning) {
        this.shipId = Objects.requireNonNull(shipId, "shipId");
        this.tuning = Objects.requireNonNull(tuning, "tuning");
        this.powerGrid = new ShipPowerGrid(tuning.powerCapacity(), tuning.initialPower(), tuning.generationPerTick());
        this.ammoPool = new ShipAmmoPool(
                Map.of(tuning.primaryAmmoType(), tuning.primaryAmmoCapacity()),
                Map.of(tuning.primaryAmmoType(), tuning.initialPrimaryAmmo())
        );
    }

    public static ShipSystemsRuntime p0(ShipId shipId) {
        return new ShipSystemsRuntime(shipId, ShipSystemsTuning.P0);
    }

    public void beginTick(long tick) {
        powerGrid.beginTick(tick);
    }

    public boolean tryPowerPropulsion(ShipControlInput input) {
        Objects.requireNonNull(input, "input");
        double activity = Math.max(Math.abs(input.throttle()), Math.max(Math.abs(input.yaw()), Math.abs(input.pitch())));
        if (activity <= AXIS_EPSILON) {
            return true;
        }
        return powerGrid.tryConsume(tuning.propulsionMaxPowerPerTick() * activity, PowerPriority.PROPULSION);
    }

    public boolean tryPowerSensorScan() {
        return powerGrid.tryConsume(tuning.sensorScanPower(), PowerPriority.ESSENTIAL);
    }

    public synchronized boolean tryFire(TurretProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (!ammoPool.canConsume(profile.ammoType(), 1)) {
            return false;
        }
        if (!powerGrid.canConsume(profile.powerPerShot(), PowerPriority.WEAPONS)) {
            return false;
        }
        if (!powerGrid.tryConsume(profile.powerPerShot(), PowerPriority.WEAPONS)) {
            throw new IllegalStateException("power availability changed during authoritative weapon transaction");
        }
        if (!ammoPool.tryConsume(profile.ammoType(), 1)) {
            throw new IllegalStateException("ammo availability changed during authoritative weapon transaction");
        }
        return true;
    }

    public ShipSensorGrid sensorGrid() {
        return sensorGrid;
    }

    public ShipId shipId() {
        return shipId;
    }

    public ShipSystemsTuning tuning() {
        return tuning;
    }

    public double powerStored() {
        return powerGrid.stored();
    }

    public double powerCapacity() {
        return powerGrid.capacity();
    }

    public double generationPerTick() {
        return powerGrid.generationPerTick();
    }

    public int ammoAmount(String ammoType) {
        return ammoPool.amount(ammoType);
    }

    public int ammoCapacity(String ammoType) {
        return ammoPool.capacity(ammoType);
    }
}
