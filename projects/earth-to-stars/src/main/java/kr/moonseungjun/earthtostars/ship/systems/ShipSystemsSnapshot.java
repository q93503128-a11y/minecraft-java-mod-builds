package kr.moonseungjun.earthtostars.ship.systems;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ShipSystemsSnapshot(
        ShipId shipId,
        double powerStored,
        Map<String, Integer> ammoAmounts,
        double propellantStored,
        double oxygenStored
) {
    public ShipSystemsSnapshot {
        Objects.requireNonNull(shipId, "shipId");
        requireNonNegativeFinite(powerStored, "powerStored");
        requireNonNegativeFinite(propellantStored, "propellantStored");
        requireNonNegativeFinite(oxygenStored, "oxygenStored");
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

    public ShipSystemsSnapshot(ShipId shipId, double powerStored, Map<String, Integer> ammoAmounts) {
        this(shipId, powerStored, ammoAmounts, 0.0D, 0.0D);
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }
}
