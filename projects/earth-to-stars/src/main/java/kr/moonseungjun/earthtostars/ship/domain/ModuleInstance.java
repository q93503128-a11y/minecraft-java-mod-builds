package kr.moonseungjun.earthtostars.ship.domain;

import java.util.Objects;
import java.util.UUID;

public record ModuleInstance(UUID instanceId, String definitionId, String slotId, double condition) {
    public ModuleInstance {
        Objects.requireNonNull(instanceId, "instanceId");
        if (definitionId == null || definitionId.isBlank()) {
            throw new IllegalArgumentException("definitionId must not be blank");
        }
        if (slotId == null || slotId.isBlank()) {
            throw new IllegalArgumentException("slotId must not be blank");
        }
        if (!Double.isFinite(condition) || condition < 0.0D || condition > 1.0D) {
            throw new IllegalArgumentException("condition must be in [0, 1]");
        }
    }

    public static ModuleInstance pristine(String definitionId, String slotId) {
        return new ModuleInstance(UUID.randomUUID(), definitionId, slotId, 1.0D);
    }
}
