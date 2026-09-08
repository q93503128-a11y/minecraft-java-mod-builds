package kr.moonseungjun.earthtostars.ship.combat;

import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;

import java.util.Objects;
import java.util.UUID;

public record SensorContact(UUID targetId, ShipVec3 position, boolean hostile, double threat) {
    public SensorContact {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(position, "position");
        if (!Double.isFinite(threat) || threat < 0.0D) {
            throw new IllegalArgumentException("threat must be finite and non-negative");
        }
    }
}
