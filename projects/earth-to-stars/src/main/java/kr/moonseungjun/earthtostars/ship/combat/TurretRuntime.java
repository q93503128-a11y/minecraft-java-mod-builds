package kr.moonseungjun.earthtostars.ship.combat;

import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsRuntime;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TurretRuntime {
    private static final long LEASE_TTL_TICKS = 60L;

    private final ShipState ship;
    private final TurretProfile profile;
    private TurretControlMode mode = TurretControlMode.OFF;
    private long nextReadyTick;
    private TurretControlLease lease;
    private ShipVec3 manualAim = new ShipVec3(0.0D, 0.0D, 1.0D);

    public TurretRuntime(ShipState ship, TurretProfile profile) {
        this.ship = Objects.requireNonNull(ship, "ship");
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public void setMode(UUID actorId, TurretControlMode newMode) {
        requireWeaponPermission(actorId);
        mode = Objects.requireNonNull(newMode, "newMode");
        if (mode != TurretControlMode.MANUAL) lease = null;
    }

    public Optional<UUID> requestManualControl(UUID playerId, long tick) {
        requireWeaponPermission(playerId);
        if (mode != TurretControlMode.MANUAL) return Optional.empty();
        expireLease(tick);
        if (lease != null && !lease.controllerId().equals(playerId)) return Optional.empty();
        UUID sessionId = UUID.randomUUID();
        lease = new TurretControlLease(playerId, sessionId, tick + LEASE_TTL_TICKS, -1L);
        return Optional.of(sessionId);
    }

    public boolean acceptManualAim(UUID playerId, UUID sessionId, long sequence, ShipVec3 direction, long tick) {
        expireLease(tick);
        if (lease == null || mode != TurretControlMode.MANUAL) return false;
        if (!lease.controllerId().equals(playerId) || !lease.sessionId().equals(sessionId)) return false;
        if (sequence <= lease.lastSequence()) return false;
        ShipVec3 normalized = Objects.requireNonNull(direction, "direction").normalized();
        if (normalized.lengthSquared() < 1.0E-12D) return false;
        manualAim = normalized;
        lease = lease.renew(tick + LEASE_TTL_TICKS, sequence);
        return true;
    }

    public Optional<TurretFireSolution> fireManual(
            UUID playerId,
            UUID sessionId,
            ShipSystemsRuntime systems,
            ShipVec3 origin,
            ShipVec3 shipForward,
            long tick
    ) {
        expireLease(tick);
        if (lease == null || mode != TurretControlMode.MANUAL) return Optional.empty();
        if (!lease.controllerId().equals(playerId) || !lease.sessionId().equals(sessionId)) return Optional.empty();
        return fire(systems, origin, shipForward, manualAim, Optional.empty(), tick);
    }

    public Optional<TurretFireSolution> tickAuto(
            ShipSystemsRuntime systems,
            ShipVec3 origin,
            ShipVec3 shipForward,
            long tick
    ) {
        if (mode != TurretControlMode.AUTO_DEFENSE || tick < nextReadyTick) return Optional.empty();
        Optional<SensorContact> target = systems.sensorGrid().bestHostile(origin, shipForward, profile);
        if (target.isEmpty()) return Optional.empty();
        SensorContact contact = target.orElseThrow();
        ShipVec3 direction = subtract(contact.position(), origin).normalized();
        return fire(systems, origin, shipForward, direction, Optional.of(contact.targetId()), tick);
    }

    public boolean releaseManualControl(UUID playerId) {
        if (lease == null || !lease.controllerId().equals(playerId)) return false;
        lease = null;
        return true;
    }

    public void expireLease(long tick) {
        if (lease != null && tick > lease.expiresAtTick()) lease = null;
    }

    public ShipState ship() { return ship; }
    public TurretControlMode mode() { return mode; }
    public Optional<TurretControlLease> lease() { return Optional.ofNullable(lease); }
    public TurretProfile profile() { return profile; }

    private Optional<TurretFireSolution> fire(
            ShipSystemsRuntime systems,
            ShipVec3 origin,
            ShipVec3 shipForward,
            ShipVec3 direction,
            Optional<UUID> targetId,
            long tick
    ) {
        Objects.requireNonNull(systems, "systems");
        if (!systems.shipId().equals(ship.shipId())) {
            throw new IllegalArgumentException("weapon cannot consume systems from another ship");
        }
        if (tick < nextReadyTick || !insideArc(shipForward, direction)) return Optional.empty();
        if (!systems.tryFire(profile)) return Optional.empty();
        nextReadyTick = tick + profile.cooldownTicks();
        return Optional.of(new TurretFireSolution(
                origin,
                direction.normalized(),
                targetId,
                profile.projectileSpeed(),
                profile.projectileLifetimeTicks(),
                profile.damage()
        ));
    }

    private boolean insideArc(ShipVec3 forward, ShipVec3 direction) {
        ShipVec3 a = forward.normalized();
        ShipVec3 b = direction.normalized();
        if (a.lengthSquared() < 1.0E-12D || b.lengthSquared() < 1.0E-12D) return false;
        return a.dot(b) >= Math.cos(Math.toRadians(profile.halfArcDegrees()));
    }

    private void requireWeaponPermission(UUID playerId) {
        if (!ship.can(playerId, ShipPermission.WEAPON_CONTROL)) {
            throw new SecurityException("player lacks weapon control permission for ship " + ship.shipId());
        }
    }

    private static ShipVec3 subtract(ShipVec3 left, ShipVec3 right) {
        return new ShipVec3(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }
}
