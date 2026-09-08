package kr.moonseungjun.earthtostars.ship.domain;

import java.util.Objects;
import java.util.UUID;

public record ShipId(UUID value) {
    public ShipId {
        Objects.requireNonNull(value, "value");
    }

    public static ShipId random() {
        return new ShipId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
