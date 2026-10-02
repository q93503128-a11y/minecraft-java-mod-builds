package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent desired Recovery Belt layout.
 *
 * <p>EMPTY means that slot has no saved auto-reload request. The project deliberately does not
 * invent a universal 4-potion default; legacy saves are migrated only from doses that are already
 * physically loaded.</p>
 */
public record RecoveryBeltSetupState(
        int schemaVersion,
        List<RecoveryBeltSlot> slots
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<RecoveryBeltSetupState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("recovery_belt_setup_schema_version")
                            .forGetter(RecoveryBeltSetupState::schemaVersion),
                    RecoveryBeltSlot.CODEC.listOf()
                            .fieldOf("slots")
                            .forGetter(RecoveryBeltSetupState::slots)
            ).apply(instance, RecoveryBeltSetupState::new));

    public RecoveryBeltSetupState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Recovery Belt setup schema version: "
                            + schemaVersion
            );
        }
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        if (slots.size() != RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException(
                    "Recovery Belt setup must contain exactly four slots."
            );
        }
    }

    public static RecoveryBeltSetupState empty() {
        return new RecoveryBeltSetupState(
                CURRENT_SCHEMA_VERSION,
                List.of(
                        RecoveryBeltSlot.EMPTY,
                        RecoveryBeltSlot.EMPTY,
                        RecoveryBeltSlot.EMPTY,
                        RecoveryBeltSlot.EMPTY
                )
        );
    }

    public static RecoveryBeltSetupState fromLoadedBelt(
            RecoveryBeltState belt
    ) {
        Objects.requireNonNull(belt, "belt");
        return new RecoveryBeltSetupState(
                CURRENT_SCHEMA_VERSION,
                belt.slots()
        );
    }

    public RecoveryBeltSetupState withSlot(
            int slot,
            RecoveryBeltSlot desired
    ) {
        Objects.requireNonNull(desired, "desired");
        requireSlot(slot);
        if (slots.get(slot) == desired) {
            return this;
        }
        ArrayList<RecoveryBeltSlot> next = new ArrayList<>(slots);
        next.set(slot, desired);
        return new RecoveryBeltSetupState(
                schemaVersion,
                next
        );
    }

    public Optional<RecoveryConsumable> desiredConsumableAt(int slot) {
        requireSlot(slot);
        return slots.get(slot).consumable();
    }

    public int configuredCount() {
        return (int) slots.stream()
                .filter(slot -> slot != RecoveryBeltSlot.EMPTY)
                .count();
    }

    public boolean isEmpty() {
        return configuredCount() == 0;
    }

    private static void requireSlot(int slot) {
        if (slot < 0 || slot >= RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException("slot must be inside [0, 3].");
        }
    }
}
