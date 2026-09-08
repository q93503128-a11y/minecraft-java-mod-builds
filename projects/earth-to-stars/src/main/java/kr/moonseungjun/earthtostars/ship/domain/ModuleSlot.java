package kr.moonseungjun.earthtostars.ship.domain;

import java.util.Objects;

public record ModuleSlot(String id, ModuleSlotType type, int sizeClass) {
    public ModuleSlot {
        requireId(id);
        Objects.requireNonNull(type, "type");
        if (sizeClass < 1) {
            throw new IllegalArgumentException("sizeClass must be >= 1");
        }
    }

    private static void requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("slot id must not be blank");
        }
    }
}
