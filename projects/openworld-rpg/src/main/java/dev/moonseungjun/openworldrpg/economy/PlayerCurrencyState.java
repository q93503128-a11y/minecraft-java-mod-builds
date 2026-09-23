package dev.moonseungjun.openworldrpg.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Persistent server-owned currency state.
 *
 * <p>One-time credits carry stable transaction IDs so a reconnect/retry cannot duplicate Gold
 * even when the quest-state commit is retried separately. Ordinary repeatable economy flows may
 * use their own controller-owned transaction keys when they are implemented.</p>
 */
public record PlayerCurrencyState(
        int schemaVersion,
        long gold,
        Set<String> appliedCreditTransactionIds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<PlayerCurrencyState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("currency_schema_version")
                            .forGetter(PlayerCurrencyState::schemaVersion),
                    Codec.LONG.fieldOf("gold").forGetter(PlayerCurrencyState::gold),
                    STRING_SET_CODEC
                            .fieldOf("applied_credit_transaction_ids")
                            .forGetter(PlayerCurrencyState::appliedCreditTransactionIds)
            ).apply(instance, PlayerCurrencyState::new)
    );

    public PlayerCurrencyState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported currency schema version: " + schemaVersion
            );
        }
        if (gold < 0L) {
            throw new IllegalArgumentException("Gold must be non-negative.");
        }
        appliedCreditTransactionIds = Set.copyOf(
                Objects.requireNonNull(
                        appliedCreditTransactionIds,
                        "appliedCreditTransactionIds"
                )
        );
        appliedCreditTransactionIds.forEach(
                value -> requireTransactionId(value, "appliedCreditTransactionId")
        );
    }

    public static PlayerCurrencyState initial() {
        return new PlayerCurrencyState(CURRENT_SCHEMA_VERSION, 0L, Set.of());
    }

    public PlayerCurrencyState creditOnce(String transactionId, long amount) {
        requireTransactionId(transactionId, "transactionId");
        if (amount <= 0L) {
            throw new IllegalArgumentException("Credit amount must be positive.");
        }
        if (appliedCreditTransactionIds.contains(transactionId)) {
            return this;
        }

        long nextGold = Math.addExact(gold, amount);
        Set<String> nextTransactions = new HashSet<>(appliedCreditTransactionIds);
        nextTransactions.add(transactionId);
        return new PlayerCurrencyState(
                schemaVersion,
                nextGold,
                Set.copyOf(nextTransactions)
        );
    }

    public boolean hasAppliedCredit(String transactionId) {
        requireTransactionId(transactionId, "transactionId");
        return appliedCreditTransactionIds.contains(transactionId);
    }

    private static void requireTransactionId(String value, String name) {
        if (value == null || value.isBlank() || !value.contains(":")) {
            throw new IllegalArgumentException(
                    name + " must be a stable namespaced transaction id."
            );
        }
    }
}
