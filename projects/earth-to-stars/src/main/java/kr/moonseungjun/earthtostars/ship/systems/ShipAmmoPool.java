package kr.moonseungjun.earthtostars.ship.systems;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShipAmmoPool {
    private final Map<String, Integer> capacities = new LinkedHashMap<>();
    private final Map<String, Integer> amounts = new LinkedHashMap<>();

    public ShipAmmoPool(Map<String, Integer> capacities, Map<String, Integer> initialAmounts) {
        if (capacities == null || capacities.isEmpty()) {
            throw new IllegalArgumentException("at least one ammo type is required");
        }
        for (Map.Entry<String, Integer> entry : capacities.entrySet()) {
            String type = requireType(entry.getKey());
            int capacity = requirePositive(entry.getValue(), "capacity");
            int initial = initialAmounts.getOrDefault(type, 0);
            if (initial < 0 || initial > capacity) {
                throw new IllegalArgumentException("initial ammo must be within capacity for " + type);
            }
            this.capacities.put(type, capacity);
            this.amounts.put(type, initial);
        }
        for (String type : initialAmounts.keySet()) {
            if (!this.capacities.containsKey(type)) {
                throw new IllegalArgumentException("initial ammo references unknown type: " + type);
            }
        }
    }

    public synchronized boolean canConsume(String type, int amount) {
        String ammoType = requireKnown(type);
        requirePositive(amount, "amount");
        return amounts.get(ammoType) >= amount;
    }

    public synchronized boolean tryConsume(String type, int amount) {
        if (!canConsume(type, amount)) {
            return false;
        }
        amounts.put(type, amounts.get(type) - amount);
        return true;
    }

    public synchronized int add(String type, int amount) {
        String ammoType = requireKnown(type);
        requirePositive(amount, "amount");
        int current = amounts.get(ammoType);
        int next = Math.min(capacities.get(ammoType), current + amount);
        amounts.put(ammoType, next);
        return next - current;
    }

    public synchronized int amount(String type) {
        return amounts.get(requireKnown(type));
    }

    public synchronized int capacity(String type) {
        return capacities.get(requireKnown(type));
    }

    public synchronized Map<String, Integer> snapshotAmounts() {
        return Map.copyOf(amounts);
    }

    private String requireKnown(String type) {
        String normalized = requireType(type);
        if (!capacities.containsKey(normalized)) {
            throw new IllegalArgumentException("unknown ammo type: " + normalized);
        }
        return normalized;
    }

    private static String requireType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("ammo type must not be blank");
        }
        return type;
    }

    private static int requirePositive(Integer value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
