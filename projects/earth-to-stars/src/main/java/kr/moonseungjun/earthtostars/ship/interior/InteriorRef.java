package kr.moonseungjun.earthtostars.ship.interior;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;

import java.util.Objects;

public record InteriorRef(ShipId shipId, long slot) {
    public InteriorRef {
        Objects.requireNonNull(shipId, "shipId");
        InteriorSlotLayout.requireValidSlot(slot);
    }

    public InteriorAnchor anchor() {
        return InteriorSlotLayout.anchor(slot);
    }
}
