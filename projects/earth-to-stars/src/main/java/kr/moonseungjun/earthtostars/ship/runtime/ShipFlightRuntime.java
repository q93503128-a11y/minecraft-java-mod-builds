package kr.moonseungjun.earthtostars.ship.runtime;

import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ShipFlightRuntime {
    public static final long CONTROL_LEASE_TTL_TICKS = 40L;

    private final ShipState ship;
    private final ShipFlightTuning tuning;
    private ShipTransform transform;
    private ShipControlInput input = ShipControlInput.ZERO;
    private ShipControlLease lease;

    public ShipFlightRuntime(ShipState ship, ShipTransform transform, ShipFlightTuning tuning) {
        this.ship = Objects.requireNonNull(ship, "ship");
        this.transform = Objects.requireNonNull(transform, "transform");
        this.tuning = Objects.requireNonNull(tuning, "tuning");
    }

    public ShipState ship() {
        return ship;
    }

    public ShipTransform transform() {
        return transform;
    }

    public ShipControlInput currentInput() {
        return input;
    }

    public Optional<ShipControlLease> lease() {
        return Optional.ofNullable(lease);
    }

    public void relocate(ShipTransform nextTransform) {
        this.transform = Objects.requireNonNull(nextTransform, "nextTransform");
        this.input = ShipControlInput.ZERO;
    }

    public Optional<UUID> requestControl(UUID playerId, long tick) {
        Objects.requireNonNull(playerId, "playerId");
        expireIfNeeded(tick);
        if (!ship.can(playerId, ShipPermission.PILOT)) {
            return Optional.empty();
        }
        if (lease != null && !lease.controllerId().equals(playerId)) {
            return Optional.empty();
        }

        UUID session = UUID.randomUUID();
        lease = new ShipControlLease(playerId, session, tick + CONTROL_LEASE_TTL_TICKS, -1L);
        input = ShipControlInput.ZERO;
        return Optional.of(session);
    }

    public boolean acceptInput(UUID playerId, UUID sessionId, long sequence, ShipControlInput nextInput, long tick) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(nextInput, "nextInput");
        expireIfNeeded(tick);
        if (lease == null
                || !lease.controllerId().equals(playerId)
                || !lease.sessionId().equals(sessionId)
                || sequence <= lease.lastSequence()) {
            return false;
        }
        lease = new ShipControlLease(playerId, sessionId, tick + CONTROL_LEASE_TTL_TICKS, sequence);
        input = nextInput;
        return true;
    }

    public boolean releaseControl(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (lease == null || !lease.controllerId().equals(playerId)) {
            return false;
        }
        lease = null;
        input = ShipControlInput.ZERO;
        return true;
    }

    public Optional<UUID> tick(long tick) {
        return tick(tick, true);
    }

    public Optional<UUID> tick(long tick, boolean propulsionPowered) {
        Optional<UUID> expiredController = expireIfNeeded(tick);
        ShipControlInput appliedInput = propulsionPowered ? input : ShipControlInput.ZERO;
        transform = ShipMovementSimulator.step(transform, appliedInput, tuning);
        return expiredController;
    }

    private Optional<UUID> expireIfNeeded(long tick) {
        if (lease == null || !lease.expiredAt(tick)) {
            return Optional.empty();
        }
        UUID controller = lease.controllerId();
        lease = null;
        input = ShipControlInput.ZERO;
        return Optional.of(controller);
    }
}
