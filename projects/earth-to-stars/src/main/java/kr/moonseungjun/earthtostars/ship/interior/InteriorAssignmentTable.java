package kr.moonseungjun.earthtostars.ship.interior;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InteriorAssignmentTable {
    private final Map<ShipId, Long> assignments = new LinkedHashMap<>();
    private final Map<Long, ShipId> reverse = new LinkedHashMap<>();
    private long nextCandidate;

    public static InteriorAssignmentTable empty() {
        return new InteriorAssignmentTable(Map.of());
    }

    public static InteriorAssignmentTable restore(Map<ShipId, Long> restored) {
        return new InteriorAssignmentTable(restored);
    }

    private InteriorAssignmentTable(Map<ShipId, Long> restored) {
        long max = -1L;
        for (Map.Entry<ShipId, Long> entry : restored.entrySet()) {
            ShipId shipId = Objects.requireNonNull(entry.getKey(), "shipId");
            long slot = Objects.requireNonNull(entry.getValue(), "slot");
            InteriorSlotLayout.requireValidSlot(slot);
            if (assignments.putIfAbsent(shipId, slot) != null) {
                throw new IllegalArgumentException("duplicate interior ship assignment: " + shipId);
            }
            ShipId prior = reverse.putIfAbsent(slot, shipId);
            if (prior != null) {
                throw new IllegalArgumentException("interior slot collision: " + slot + " for " + prior + " and " + shipId);
            }
            max = Math.max(max, slot);
        }
        nextCandidate = max + 1L;
    }

    public Optional<InteriorRef> find(ShipId shipId) {
        Objects.requireNonNull(shipId, "shipId");
        Long slot = assignments.get(shipId);
        return slot == null ? Optional.empty() : Optional.of(new InteriorRef(shipId, slot));
    }

    public InteriorRef getOrAllocate(ShipId shipId) {
        Objects.requireNonNull(shipId, "shipId");
        Optional<InteriorRef> existing = find(shipId);
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        while (reverse.containsKey(nextCandidate)) {
            nextCandidate++;
        }
        if (nextCandidate >= InteriorSlotLayout.MAX_SLOTS) {
            throw new IllegalStateException("ship interior allocation grid exhausted");
        }
        long slot = nextCandidate++;
        assignments.put(shipId, slot);
        reverse.put(slot, shipId);
        return new InteriorRef(shipId, slot);
    }

    public Optional<ShipId> findShip(long slot) {
        InteriorSlotLayout.requireValidSlot(slot);
        return Optional.ofNullable(reverse.get(slot));
    }

    public Map<ShipId, Long> snapshot() {
        return Map.copyOf(assignments);
    }
}
