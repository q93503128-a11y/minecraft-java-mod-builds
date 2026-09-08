package kr.moonseungjun.earthtostars.ship.runtime;

import java.util.Objects;
import java.util.UUID;

public record ShipControlLease(UUID controllerId, UUID sessionId, long expiresAtTick, long lastSequence) {
    public ShipControlLease {
        Objects.requireNonNull(controllerId, "controllerId");
        Objects.requireNonNull(sessionId, "sessionId");
    }

    public boolean expiredAt(long tick) {
        return tick >= expiresAtTick;
    }
}
