package kr.moonseungjun.earthtostars.ship.combat;

import java.util.UUID;

public record TurretControlLease(UUID controllerId, UUID sessionId, long expiresAtTick, long lastSequence) {
    public TurretControlLease renew(long newExpiry, long sequence) {
        return new TurretControlLease(controllerId, sessionId, newExpiry, sequence);
    }
}
