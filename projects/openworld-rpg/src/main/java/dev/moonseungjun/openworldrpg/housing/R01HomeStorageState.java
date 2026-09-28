package dev.moonseungjun.openworldrpg.housing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.inventory.ProjectBackpackState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Logical Home Storage for the currently owned residence.
 *
 * <p>The temporary Moving section is intentionally outside ordinary capacity so downsizing never
 * destroys items. It is migration overflow only, not extra permanent house capacity.</p>
 */
public record R01HomeStorageState(
        int capacity,
        List<ProjectBackpackState.SlotEntry> occupied,
        List<ProjectInventoryItem> moving
) {
    public static final int MIN_CAPACITY = 54;
    public static final int MAX_CAPACITY = 144;

    public static final Codec<R01HomeStorageState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(MIN_CAPACITY, MAX_CAPACITY)
                            .fieldOf("capacity")
                            .forGetter(R01HomeStorageState::capacity),
                    ProjectBackpackState.SlotEntry.CODEC.listOf()
                            .fieldOf("occupied")
                            .forGetter(R01HomeStorageState::occupied),
                    ProjectInventoryItem.CODEC.listOf()
                            .fieldOf("moving")
                            .forGetter(R01HomeStorageState::moving)
            ).apply(instance, R01HomeStorageState::new));

    public R01HomeStorageState {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                    "Home Storage capacity outside supported housing range."
            );
        }
        occupied = Objects.requireNonNull(occupied, "occupied").stream()
                .sorted(Comparator.comparingInt(ProjectBackpackState.SlotEntry::slot))
                .toList();
        moving = List.copyOf(Objects.requireNonNull(moving, "moving"));
        Set<Integer> seen = new HashSet<>();
        for (ProjectBackpackState.SlotEntry entry : occupied) {
            Objects.requireNonNull(entry, "occupied entry");
            if (entry.slot() < 0
                    || entry.slot() >= capacity
                    || !seen.add(entry.slot())) {
                throw new IllegalArgumentException(
                        "Invalid/duplicate Home Storage slot " + entry.slot()
                );
            }
        }
        moving.forEach(value -> Objects.requireNonNull(value, "moving item"));
    }

    public static R01HomeStorageState empty(int capacity) {
        return new R01HomeStorageState(capacity, List.of(), List.of());
    }

    public int usedSlots() {
        return occupied.size();
    }

    public Optional<ProjectInventoryItem> itemAt(int slot) {
        return occupied.stream()
                .filter(entry -> entry.slot() == slot)
                .map(ProjectBackpackState.SlotEntry::item)
                .findFirst();
    }

    public R01HomeStorageState resizeForMove(int newCapacity) {
        if (newCapacity < MIN_CAPACITY || newCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                    "Home Storage capacity outside supported housing range."
            );
        }
        if (newCapacity == capacity) {
            return this;
        }

        List<ProjectBackpackState.SlotEntry> nextOccupied = new ArrayList<>();
        List<ProjectInventoryItem> nextMoving = new ArrayList<>(moving);
        int nextSlot = 0;
        for (ProjectBackpackState.SlotEntry entry : occupied) {
            if (nextSlot < newCapacity) {
                nextOccupied.add(
                        new ProjectBackpackState.SlotEntry(
                                nextSlot++,
                                entry.item()
                        )
                );
            } else {
                nextMoving.add(entry.item());
            }
        }
        return new R01HomeStorageState(
                newCapacity,
                List.copyOf(nextOccupied),
                List.copyOf(nextMoving)
        );
    }

    public InsertResult insert(ProjectInventoryItem incoming) {
        Objects.requireNonNull(incoming, "incoming");
        ArrayList<ProjectBackpackState.SlotEntry> next =
                new ArrayList<>(occupied);
        int remaining = incoming.quantity();

        for (int i = 0; i < next.size() && remaining > 0; i++) {
            var entry = next.get(i);
            if (!entry.item().canMerge(incoming)
                    || entry.item().quantity() >= entry.item().stackCap()) {
                continue;
            }
            int room = entry.item().stackCap() - entry.item().quantity();
            int moved = Math.min(room, remaining);
            next.set(
                    i,
                    new ProjectBackpackState.SlotEntry(
                            entry.slot(),
                            entry.item().withQuantity(
                                    entry.item().quantity() + moved
                            )
                    )
            );
            remaining -= moved;
        }

        Set<Integer> used = new HashSet<>();
        for (var entry : next) {
            used.add(entry.slot());
        }
        for (int slot = 0; slot < capacity && remaining > 0; slot++) {
            if (used.contains(slot)) {
                continue;
            }
            int moved = Math.min(incoming.stackCap(), remaining);
            next.add(
                    new ProjectBackpackState.SlotEntry(
                            slot,
                            incoming.withQuantity(moved)
                    )
            );
            used.add(slot);
            remaining -= moved;
        }

        return new InsertResult(
                new R01HomeStorageState(capacity, next, moving),
                remaining == 0
                        ? Optional.empty()
                        : Optional.of(incoming.withQuantity(remaining))
        );
    }

    public record InsertResult(
            R01HomeStorageState state,
            Optional<ProjectInventoryItem> remainder
    ) {
        public InsertResult {
            Objects.requireNonNull(state, "state");
            remainder = Objects.requireNonNull(remainder, "remainder");
        }
    }
}
