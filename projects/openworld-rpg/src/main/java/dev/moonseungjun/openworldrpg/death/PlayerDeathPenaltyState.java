package dev.moonseungjun.openworldrpg.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Persistent reconnect-safe automatic death-penalty transaction state. */
public record PlayerDeathPenaltyState(
        int schemaVersion,
        long transactionSequence,
        Optional<PendingPenalty> pendingPenalty
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<PenaltyKind> KIND_CODEC =
            Codec.STRING.xmap(
                    value -> PenaltyKind.valueOf(
                            value.toUpperCase(Locale.ROOT)
                    ),
                    value -> value.name().toLowerCase(Locale.ROOT)
            );

    public static final Codec<PlayerDeathPenaltyState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("death_penalty_schema_version")
                            .forGetter(PlayerDeathPenaltyState::schemaVersion),
                    Codec.LONG.fieldOf("transaction_sequence")
                            .forGetter(PlayerDeathPenaltyState::transactionSequence),
                    PendingPenalty.CODEC.optionalFieldOf("pending_penalty")
                            .forGetter(PlayerDeathPenaltyState::pendingPenalty)
            ).apply(instance, PlayerDeathPenaltyState::new));

    public PlayerDeathPenaltyState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported death-penalty schema version: "
                            + schemaVersion
            );
        }
        if (transactionSequence < 0L) {
            throw new IllegalArgumentException(
                    "Death-penalty sequence must be non-negative."
            );
        }
        pendingPenalty = Objects.requireNonNull(
                pendingPenalty,
                "pendingPenalty"
        );
        PendingPenalty pending = pendingPenalty.orElse(null);
        if (pending != null
                && pending.sequence() != transactionSequence) {
            throw new IllegalArgumentException(
                    "Pending death-penalty sequence mismatch."
            );
        }
    }

    public static PlayerDeathPenaltyState initial() {
        return new PlayerDeathPenaltyState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty()
        );
    }

    public PlayerDeathPenaltyState prepare(
            PenaltyKind kind,
            int combatLevel,
            long beforeValue,
            long amount
    ) {
        Objects.requireNonNull(kind, "kind");
        if (pendingPenalty.isPresent()) {
            throw new IllegalStateException(
                    "A death penalty is already pending."
            );
        }
        validateLevelAndAmount(combatLevel, amount);
        long afterValue = Math.subtractExact(beforeValue, amount);
        validateValueShape(kind, beforeValue, afterValue);

        long sequence = Math.addExact(transactionSequence, 1L);
        PendingPenalty pending = new PendingPenalty(
                "openworld_rpg:death_penalty/" + sequence,
                sequence,
                kind,
                combatLevel,
                beforeValue,
                afterValue,
                amount
        );
        return new PlayerDeathPenaltyState(
                schemaVersion,
                sequence,
                Optional.of(pending)
        );
    }

    public PlayerDeathPenaltyState commit(String transactionId) {
        requireTransactionId(transactionId);
        PendingPenalty pending = pendingPenalty.orElseThrow(
                () -> new IllegalStateException(
                        "No death penalty is pending."
                )
        );
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Cannot commit a different death penalty."
            );
        }
        return new PlayerDeathPenaltyState(
                schemaVersion,
                transactionSequence,
                Optional.empty()
        );
    }

    public enum PenaltyKind {
        CURRENT_LEVEL_XP,
        GOLD
    }

    public record PendingPenalty(
            String transactionId,
            long sequence,
            PenaltyKind kind,
            int combatLevel,
            long beforeValue,
            long afterValue,
            long amount
    ) {
        public static final Codec<PendingPenalty> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingPenalty::transactionId),
                        Codec.LONG.fieldOf("sequence")
                                .forGetter(PendingPenalty::sequence),
                        KIND_CODEC.fieldOf("kind")
                                .forGetter(PendingPenalty::kind),
                        Codec.INT.fieldOf("combat_level")
                                .forGetter(PendingPenalty::combatLevel),
                        Codec.LONG.fieldOf("before_value")
                                .forGetter(PendingPenalty::beforeValue),
                        Codec.LONG.fieldOf("after_value")
                                .forGetter(PendingPenalty::afterValue),
                        Codec.LONG.fieldOf("amount")
                                .forGetter(PendingPenalty::amount)
                ).apply(instance, PendingPenalty::new));

        public PendingPenalty {
            requireTransactionId(transactionId);
            Objects.requireNonNull(kind, "kind");
            if (sequence <= 0L) {
                throw new IllegalArgumentException(
                        "Death-penalty sequence must be positive."
                );
            }
            validateLevelAndAmount(combatLevel, amount);
            if (Math.subtractExact(beforeValue, amount) != afterValue) {
                throw new IllegalArgumentException(
                        "Death-penalty before/after values do not match amount."
                );
            }
            validateValueShape(kind, beforeValue, afterValue);
        }

        public String effectTransactionId() {
            return transactionId
                    + (kind == PenaltyKind.GOLD ? "/gold" : "/xp");
        }
    }

    private static void validateLevelAndAmount(
            int combatLevel,
            long amount
    ) {
        if (combatLevel < 1
                || combatLevel > ProjectProgressionRules.MAX_COMBAT_LEVEL
                || amount <= 0L) {
            throw new IllegalArgumentException(
                    "Invalid death-penalty level/amount."
            );
        }
    }

    private static void validateValueShape(
            PenaltyKind kind,
            long beforeValue,
            long afterValue
    ) {
        if (kind == PenaltyKind.CURRENT_LEVEL_XP
                && (beforeValue < 0L || afterValue < 0L)) {
            throw new IllegalArgumentException(
                    "Current-Lv EXP penalty cannot cross below zero."
            );
        }
    }

    private static void requireTransactionId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Death-penalty transaction id must be namespaced."
            );
        }
    }
}
