package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent server-owned class-switch transaction state.
 *
 * <p>The transaction is prepared before Gold is debited, so a disconnect can resume the same
 * debit and class commit without double-charging or allowing an old debit receipt to be replayed
 * for a later free switch.</p>
 */
public record PlayerClassSwitchState(
        int schemaVersion,
        long transactionSequence,
        Optional<PendingSwitch> pendingSwitch
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<PlayerClassSwitchState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("class_switch_schema_version")
                            .forGetter(PlayerClassSwitchState::schemaVersion),
                    Codec.LONG.fieldOf("transaction_sequence")
                            .forGetter(PlayerClassSwitchState::transactionSequence),
                    PendingSwitch.CODEC.optionalFieldOf("pending_switch")
                            .forGetter(PlayerClassSwitchState::pendingSwitch)
            ).apply(instance, PlayerClassSwitchState::new));

    public PlayerClassSwitchState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported class-switch schema version: "
                            + schemaVersion
            );
        }
        if (transactionSequence < 0L) {
            throw new IllegalArgumentException(
                    "Class-switch transaction sequence must be non-negative."
            );
        }
        pendingSwitch = Objects.requireNonNull(
                pendingSwitch,
                "pendingSwitch"
        );
        PendingSwitch pending = pendingSwitch.orElse(null);
        if (pending != null
                && pending.sequence() != transactionSequence) {
            throw new IllegalArgumentException(
                    "Pending class-switch sequence mismatch."
            );
        }
    }

    public static PlayerClassSwitchState initial() {
        return new PlayerClassSwitchState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty()
        );
    }

    public PlayerClassSwitchState prepare(
            RootClass sourceClass,
            RootClass targetClass,
            long goldCost
    ) {
        Objects.requireNonNull(sourceClass, "sourceClass");
        Objects.requireNonNull(targetClass, "targetClass");
        if (sourceClass == targetClass) {
            throw new IllegalArgumentException(
                    "Class switch requires a different target class."
            );
        }
        if (goldCost <= 0L) {
            throw new IllegalArgumentException(
                    "Class-switch Gold cost must be positive."
            );
        }
        if (pendingSwitch.isPresent()) {
            throw new IllegalStateException(
                    "Another class-switch transaction is already pending."
            );
        }

        long sequence = Math.addExact(transactionSequence, 1L);
        PendingSwitch pending = new PendingSwitch(
                "openworld_rpg:class_switch/" + sequence,
                sequence,
                sourceClass,
                targetClass,
                goldCost
        );
        return new PlayerClassSwitchState(
                schemaVersion,
                sequence,
                Optional.of(pending)
        );
    }

    public PlayerClassSwitchState commit(String transactionId) {
        PendingSwitch pending = requirePending(transactionId);
        return new PlayerClassSwitchState(
                schemaVersion,
                pending.sequence(),
                Optional.empty()
        );
    }

    public PlayerClassSwitchState cancel(String transactionId) {
        PendingSwitch pending = requirePending(transactionId);
        return new PlayerClassSwitchState(
                schemaVersion,
                pending.sequence(),
                Optional.empty()
        );
    }

    private PendingSwitch requirePending(String transactionId) {
        requireStableId(transactionId);
        PendingSwitch pending = pendingSwitch.orElseThrow(
                () -> new IllegalStateException(
                        "No class-switch transaction is pending."
                )
        );
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Cannot mutate a different class-switch transaction."
            );
        }
        return pending;
    }

    public record PendingSwitch(
            String transactionId,
            long sequence,
            RootClass sourceClass,
            RootClass targetClass,
            long goldCost
    ) {
        public static final Codec<PendingSwitch> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingSwitch::transactionId),
                        Codec.LONG.fieldOf("sequence")
                                .forGetter(PendingSwitch::sequence),
                        RootClass.CODEC.fieldOf("source_class")
                                .forGetter(PendingSwitch::sourceClass),
                        RootClass.CODEC.fieldOf("target_class")
                                .forGetter(PendingSwitch::targetClass),
                        Codec.LONG.fieldOf("gold_cost")
                                .forGetter(PendingSwitch::goldCost)
                ).apply(instance, PendingSwitch::new));

        public PendingSwitch {
            requireStableId(transactionId);
            if (sequence <= 0L) {
                throw new IllegalArgumentException(
                        "Class-switch sequence must be positive."
                );
            }
            Objects.requireNonNull(sourceClass, "sourceClass");
            Objects.requireNonNull(targetClass, "targetClass");
            if (sourceClass == targetClass) {
                throw new IllegalArgumentException(
                        "Pending class switch requires different classes."
                );
            }
            if (goldCost <= 0L) {
                throw new IllegalArgumentException(
                        "Pending class-switch Gold cost must be positive."
                );
            }
        }

        public String debitTransactionId() {
            return transactionId + "/gold";
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Class-switch transaction id must be namespaced."
            );
        }
    }
}
