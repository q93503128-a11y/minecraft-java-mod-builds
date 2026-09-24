package dev.moonseungjun.openworldrpg.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Sparse immutable state for the canonical 9-column RPG Backpack/Personal Storage grids. */
public record ProjectBackpackState(
        int rows,
        List<SlotEntry> occupied
) {
    public static final int COLUMNS = 9;
    public static final int STARTING_ROWS = 4;
    public static final int MAX_BACKPACK_ROWS = 7;

    public static final Codec<ProjectBackpackState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(STARTING_ROWS, MAX_BACKPACK_ROWS)
                            .fieldOf("rows")
                            .forGetter(ProjectBackpackState::rows),
                    SlotEntry.CODEC.listOf()
                            .fieldOf("occupied")
                            .forGetter(ProjectBackpackState::occupied)
            ).apply(instance, ProjectBackpackState::new)
    );

    public ProjectBackpackState {
        occupied = occupied.stream()
                .sorted(Comparator.comparingInt(SlotEntry::slot))
                .toList();
        int capacity = rows * COLUMNS;
        Set<Integer> seen = new HashSet<>();
        for (SlotEntry entry : occupied) {
            if (entry.slot() < 0 || entry.slot() >= capacity || !seen.add(entry.slot())) {
                throw new IllegalArgumentException(
                        "Backpack contains invalid/duplicate slot " + entry.slot()
                );
            }
        }
    }

    public static ProjectBackpackState startingBackpack() {
        return new ProjectBackpackState(STARTING_ROWS, List.of());
    }

    public static ProjectBackpackState personalStorage() {
        return new ProjectBackpackState(STARTING_ROWS, List.of());
    }

    public int capacity() {
        return rows * COLUMNS;
    }

    public int usedSlots() {
        return occupied.size();
    }

    public Optional<ProjectInventoryItem> itemAt(int slot) {
        return occupied.stream()
                .filter(entry -> entry.slot() == slot)
                .map(SlotEntry::item)
                .findFirst();
    }

    public InsertResult insert(ProjectInventoryItem incoming) {
        Objects.requireNonNull(incoming, "incoming");
        ArrayList<SlotEntry> next = new ArrayList<>(occupied);
        int remaining = incoming.quantity();

        for (int i = 0; i < next.size() && remaining > 0; i++) {
            SlotEntry entry = next.get(i);
            if (!entry.item().canMerge(incoming)
                    || entry.item().quantity() >= entry.item().stackCap()) {
                continue;
            }
            int room = entry.item().stackCap() - entry.item().quantity();
            int moved = Math.min(room, remaining);
            next.set(
                    i,
                    new SlotEntry(
                            entry.slot(),
                            entry.item().withQuantity(entry.item().quantity() + moved)
                    )
            );
            remaining -= moved;
        }

        Set<Integer> used = new HashSet<>();
        for (SlotEntry entry : next) {
            used.add(entry.slot());
        }

        for (int slot = 0; slot < capacity() && remaining > 0; slot++) {
            if (used.contains(slot)) {
                continue;
            }
            int moved = Math.min(incoming.stackCap(), remaining);
            next.add(new SlotEntry(slot, incoming.withQuantity(moved)));
            used.add(slot);
            remaining -= moved;
        }

        ProjectBackpackState state = new ProjectBackpackState(rows, next);
        Optional<ProjectInventoryItem> remainder = remaining == 0
                ? Optional.empty()
                : Optional.of(incoming.withQuantity(remaining));
        return new InsertResult(state, remainder);
    }

    public WithdrawResult withdrawStackable(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            throw new IllegalArgumentException("withdraw requires itemId and positive amount.");
        }

        ArrayList<SlotEntry> next = new ArrayList<>(occupied);
        int remaining = amount;
        for (int i = next.size() - 1; i >= 0 && remaining > 0; i--) {
            SlotEntry entry = next.get(i);
            ProjectInventoryItem item = entry.item();
            if (!item.itemId().equals(itemId) || item.equipmentProjection().isPresent()) {
                continue;
            }
            int removed = Math.min(item.quantity(), remaining);
            remaining -= removed;
            if (removed == item.quantity()) {
                next.remove(i);
            } else {
                next.set(
                        i,
                        new SlotEntry(
                                entry.slot(),
                                item.withQuantity(item.quantity() - removed)
                        )
                );
            }
        }
        return new WithdrawResult(
                new ProjectBackpackState(rows, next),
                amount - remaining
        );
    }

    public record SlotEntry(
            int slot,
            ProjectInventoryItem item
    ) {
        public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("slot").forGetter(SlotEntry::slot),
                        ProjectInventoryItem.CODEC.fieldOf("item").forGetter(SlotEntry::item)
                ).apply(instance, SlotEntry::new)
        );

        public SlotEntry {
            Objects.requireNonNull(item, "item");
        }
    }

    public record InsertResult(
            ProjectBackpackState state,
            Optional<ProjectInventoryItem> remainder
    ) {
        public InsertResult {
            Objects.requireNonNull(state, "state");
            remainder = Objects.requireNonNull(remainder, "remainder");
        }
    }

    public record WithdrawResult(
            ProjectBackpackState state,
            int removed
    ) {
        public WithdrawResult {
            Objects.requireNonNull(state, "state");
            if (removed < 0) {
                throw new IllegalArgumentException("removed must be non-negative.");
            }
        }
    }
}
