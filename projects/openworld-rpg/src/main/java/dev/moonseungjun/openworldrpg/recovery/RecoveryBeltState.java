package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Persistent server-owned four-slot Recovery Belt state. */
public record RecoveryBeltState(
        int schemaVersion,
        List<RecoveryBeltSlot> slots,
        int selectedSlot,
        long sharedLockoutUntilTick,
        Set<String> appliedLoadTransactionIds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<RecoveryBeltState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("recovery_belt_schema_version")
                            .forGetter(RecoveryBeltState::schemaVersion),
                    RecoveryBeltSlot.CODEC.listOf()
                            .fieldOf("slots")
                            .forGetter(RecoveryBeltState::slots),
                    Codec.intRange(0, RecoveryActionRules.BELT_CAPACITY - 1)
                            .fieldOf("selected_slot")
                            .forGetter(RecoveryBeltState::selectedSlot),
                    Codec.LONG.fieldOf("shared_lockout_until_tick")
                            .forGetter(RecoveryBeltState::sharedLockoutUntilTick),
                    STRING_SET_CODEC.fieldOf("applied_load_transaction_ids")
                            .forGetter(RecoveryBeltState::appliedLoadTransactionIds)
            ).apply(instance, RecoveryBeltState::new)
    );

    public RecoveryBeltState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Recovery Belt schema version: " + schemaVersion
            );
        }
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        if (slots.size() != RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException("Recovery Belt must contain exactly four slots.");
        }
        if (selectedSlot < 0 || selectedSlot >= RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException("selectedSlot must be inside [0, 3].");
        }
        if (sharedLockoutUntilTick < 0L) {
            throw new IllegalArgumentException("sharedLockoutUntilTick must be non-negative.");
        }
        appliedLoadTransactionIds = Set.copyOf(
                Objects.requireNonNull(appliedLoadTransactionIds, "appliedLoadTransactionIds")
        );
        appliedLoadTransactionIds.forEach(RecoveryBeltState::requireTransactionId);
    }

    public static RecoveryBeltState empty() {
        return new RecoveryBeltState(
                CURRENT_SCHEMA_VERSION,
                List.of(
                        RecoveryBeltSlot.EMPTY,
                        RecoveryBeltSlot.EMPTY,
                        RecoveryBeltSlot.EMPTY,
                        RecoveryBeltSlot.EMPTY
                ),
                0,
                0L,
                Set.of()
        );
    }

    public Optional<RecoveryConsumable> selectedConsumable() {
        return consumableAt(selectedSlot);
    }

    public Optional<RecoveryConsumable> consumableAt(int slot) {
        if (slot < 0 || slot >= RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException("slot must be inside [0, 3].");
        }
        return slots.get(slot).consumable();
    }

    public int loadedCount() {
        return (int) slots.stream().filter(slot -> slot != RecoveryBeltSlot.EMPTY).count();
    }

    public boolean isLockedOut(long nowTick) {
        return nowTick < sharedLockoutUntilTick;
    }

    public boolean hasAppliedLoad(String transactionId) {
        requireTransactionId(transactionId);
        return appliedLoadTransactionIds.contains(transactionId);
    }

    public RecoveryBeltState withSelectedSlot(int slot) {
        if (slot < 0 || slot >= RecoveryActionRules.BELT_CAPACITY) {
            throw new IllegalArgumentException("slot must be inside [0, 3].");
        }
        if (slot == selectedSlot) {
            return this;
        }
        return new RecoveryBeltState(
                schemaVersion,
                slots,
                slot,
                sharedLockoutUntilTick,
                appliedLoadTransactionIds
        );
    }

    /**
     * Loads a caller-proven reserve dose once.
     *
     * <p>Inventory/merchant/alchemy code must first commit the real reserve-item transaction. The
     * stable ID makes reconnect retries safe and prevents loading the same reserved item twice.</p>
     */
    public RecoveryBeltState loadCommittedReserveDoseOnce(
            String transactionId,
            RecoveryConsumable consumable
    ) {
        requireTransactionId(transactionId);
        Objects.requireNonNull(consumable, "consumable");
        if (appliedLoadTransactionIds.contains(transactionId)) {
            return this;
        }

        int emptyIndex = slots.indexOf(RecoveryBeltSlot.EMPTY);
        if (emptyIndex < 0) {
            throw new IllegalStateException("Recovery Belt is already full.");
        }

        ArrayList<RecoveryBeltSlot> nextSlots = new ArrayList<>(slots);
        nextSlots.set(emptyIndex, RecoveryBeltSlot.loaded(consumable));
        Set<String> nextTransactions = new HashSet<>(appliedLoadTransactionIds);
        nextTransactions.add(transactionId);
        return new RecoveryBeltState(
                schemaVersion,
                nextSlots,
                selectedSlot,
                sharedLockoutUntilTick,
                Set.copyOf(nextTransactions)
        );
    }

    public Resolution consumeSelectedAtResolution(long nowTick) {
        RecoveryConsumable expected = selectedConsumable().orElseThrow(
                () -> new IllegalStateException("Selected Recovery Belt slot is empty.")
        );
        return consumeSlotAtResolution(selectedSlot, expected, nowTick);
    }

    public Resolution consumeSlotAtResolution(
            int slot,
            RecoveryConsumable expectedConsumable,
            long nowTick
    ) {
        Objects.requireNonNull(expectedConsumable, "expectedConsumable");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        if (isLockedOut(nowTick)) {
            throw new IllegalStateException("Recovery Belt is still in shared lockout.");
        }
        RecoveryConsumable actual = consumableAt(slot).orElseThrow(
                () -> new IllegalStateException("Recovery Belt action slot is empty.")
        );
        if (actual != expectedConsumable) {
            throw new IllegalStateException(
                    "Recovery Belt action payload changed before resolution."
            );
        }

        ArrayList<RecoveryBeltSlot> next = new ArrayList<>(slots);
        next.set(slot, RecoveryBeltSlot.EMPTY);
        RecoveryBeltState state = new RecoveryBeltState(
                schemaVersion,
                next,
                selectedSlot,
                Math.addExact(nowTick, RecoveryActionRules.SHARED_LOCKOUT_TICKS),
                appliedLoadTransactionIds
        );
        return new Resolution(state, actual);
    }

    public record Resolution(
            RecoveryBeltState state,
            RecoveryConsumable consumable
    ) {
        public Resolution {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(consumable, "consumable");
        }
    }

    private static void requireTransactionId(String transactionId) {
        if (transactionId == null
                || transactionId.isBlank()
                || !transactionId.contains(":")) {
            throw new IllegalArgumentException(
                    "Recovery Belt transaction ID must be a stable namespaced ID."
            );
        }
    }
}
