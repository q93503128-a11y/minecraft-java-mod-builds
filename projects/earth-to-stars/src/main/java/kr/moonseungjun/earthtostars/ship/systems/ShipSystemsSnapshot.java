package kr.moonseungjun.earthtostars.ship.systems;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ShipSystemsSnapshot(
        ShipId shipId,
        double powerStored,
        Map<String, Integer> ammoAmounts
) {
    public ShipSystemsSnapshot {
        Objects.requireNonNull(shipId, "shipId");
        if (!Double.isFinite(powerStored) || powerStored < 0.0D) {
            throw new IllegalArgumentException("powerStored must be finite and >= 0");
        }
        Objects.requireNonNull(ammoAmounts, "ammoAmounts");
        Map<String, Integer> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : ammoAmounts.entrySet()) {
            String type = entry.getKey();
            Integer amount = entry.getValue();
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("ammo type must not be blank");
            }
            if (amount == null || amount < 0) {
                throw new IllegalArgumentException("ammo amount must be >= 0 for " + type);
            }
            copy.put(type, amount);
        }
        ammoAmounts = Map.copyOf(copy);
    }
}
