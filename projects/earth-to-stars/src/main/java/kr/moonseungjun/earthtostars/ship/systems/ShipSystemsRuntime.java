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
    private final ShipResourceTank propellantTank;
    private final ShipResourceTank oxygenTank;

    public ShipSystemsRuntime(ShipId shipId, ShipSystemsTuning tuning) {
        this(
                shipId,
                tuning,
                tuning.initialPower(),
                Map.of(tuning.primaryAmmoType(), tuning.initialPrimaryAmmo()),
                tuning.initialPropellant(),
                tuning.initialOxygen()
        );
    }

    private ShipSystemsRuntime(
            ShipId shipId,
            ShipSystemsTuning tuning,
            double initialPower,
            Map<String, Integer> initialAmmo,
            double initialPropellant,
            double initialOxygen
    ) {
        this.shipId = Objects.requireNonNull(shipId, "shipId");
        this.tuning = Objects.requireNonNull(tuning, "tuning");
        this.powerGrid = new ShipPowerGrid(tuning.powerCapacity(), initialPower, tuning.generationPerTick());
        this.ammoPool = new ShipAmmoPool(
                Map.of(tuning.primaryAmmoType(), tuning.primaryAmmoCapacity()),
                Objects.requireNonNull(initialAmmo, "initialAmmo")
        );
        this.propellantTank = new ShipResourceTank(tuning.propellantCapacity(), initialPropellant);
        this.oxygenTank = new ShipResourceTank(tuning.oxygenCapacity(), initialOxygen);
    }

    public static ShipSystemsRuntime p0(ShipId shipId) {
        return new ShipSystemsRuntime(shipId, ShipSystemsTuning.P0);
    }

    public static ShipSystemsRuntime restore(ShipSystemsSnapshot snapshot, ShipSystemsTuning tuning) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(tuning, "tuning");
        if (snapshot.powerStored() > tuning.powerCapacity()) {
            throw new IllegalArgumentException("persisted power exceeds current capacity for ship " + snapshot.shipId());
        }
        if (snapshot.propellantStored() > tuning.propellantCapacity()) {
            throw new IllegalArgumentException("persisted propellant exceeds current capacity for ship " + snapshot.shipId());
        }
        if (snapshot.oxygenStored() > tuning.oxygenCapacity()) {
            throw new IllegalArgumentException("persisted oxygen exceeds current capacity for ship " + snapshot.shipId());
        }
        return new ShipSystemsRuntime(
                snapshot.shipId(),
                tuning,
                snapshot.powerStored(),
                snapshot.ammoAmounts(),
                snapshot.propellantStored(),
                snapshot.oxygenStored()
        );
    }

    public void beginTick(long tick) {
        powerGrid.beginTick(tick);
    }

    public boolean tryPowerPropulsion(ShipControlInput input) {
        return tryPowerPropulsion(input, 0.0D);
    }

    public synchronized boolean tryPowerPropulsion(ShipControlInput input, double propellantCost) {
        Objects.requireNonNull(input, "input");
        if (!Double.isFinite(propellantCost) || propellantCost < 0.0D) {
            throw new IllegalArgumentException("propellantCost must be finite and >= 0");
        }
        double activity = Math.max(Math.abs(input.throttle()), Math.max(Math.abs(input.yaw()), Math.abs(input.pitch())));
        if (activity <= AXIS_EPSILON) {
            return true;
        }
        double powerCost = tuning.propulsionMaxPowerPerTick() * activity;
        if (!powerGrid.canConsume(powerCost, PowerPriority.PROPULSION) || !propellantTank.canConsume(propellantCost)) {
            return false;
        }
        if (!powerGrid.tryConsume(powerCost, PowerPriority.PROPULSION)) {
            throw new IllegalStateException("power availability changed during propulsion transaction");
        }
        if (!propellantTank.tryConsume(propellantCost)) {
            throw new IllegalStateException("propellant availability changed during propulsion transaction");
        }
        return true;
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

    public synchronized double drainPowerFromHostile(double amount) {
        return powerGrid.drain(amount);
    }

    public synchronized double consumeOxygen(double amount) {
        return oxygenTank.drain(amount);
    }

    public synchronized double loadPropellantCell() {
        return propellantTank.fill(tuning.propellantPerCell());
    }

    public synchronized double loadOxygenCartridge() {
        return oxygenTank.fill(tuning.oxygenPerCartridge());
    }

    public synchronized ShipSystemsSnapshot snapshot() {
        return new ShipSystemsSnapshot(
                shipId,
                powerGrid.stored(),
                ammoPool.snapshotAmounts(),
                propellantTank.stored(),
                oxygenTank.stored()
        );
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

    public double propellantStored() {
        return propellantTank.stored();
    }

    public double propellantCapacity() {
        return propellantTank.capacity();
    }

    public double oxygenStored() {
        return oxygenTank.stored();
    }

    public double oxygenCapacity() {
        return oxygenTank.capacity();
    }
}
