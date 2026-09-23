package dev.moonseungjun.openworldrpg.progression.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Durable coordinator for multi-domain one-time rewards.
 *
 * <p>The plan is persisted before Gold / combat XP / Class XP are mutated. Each domain has its own
 * idempotent transaction key, so reconnect can safely finish an interrupted reward without
 * rerolling percentage-derived amounts or duplicating already-applied components.</p>
 */
public record PlayerRewardTransactionState(
        int schemaVersion,
        Map<String, RewardPlan> pendingPlans,
        Set<String> completedTransactionIds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<PlayerRewardTransactionState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("reward_transaction_schema_version")
                            .forGetter(PlayerRewardTransactionState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, RewardPlan.CODEC)
                            .fieldOf("pending_plans")
                            .forGetter(PlayerRewardTransactionState::pendingPlans),
                    STRING_SET_CODEC
                            .fieldOf("completed_transaction_ids")
                            .forGetter(PlayerRewardTransactionState::completedTransactionIds)
            ).apply(instance, PlayerRewardTransactionState::new));

    public PlayerRewardTransactionState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported reward-transaction schema version: " + schemaVersion
            );
        }
        pendingPlans = Map.copyOf(Objects.requireNonNull(pendingPlans, "pendingPlans"));
        completedTransactionIds = Set.copyOf(
                Objects.requireNonNull(completedTransactionIds, "completedTransactionIds")
        );
        pendingPlans.keySet().forEach(PlayerRewardTransactionState::requireTransactionId);
        completedTransactionIds.forEach(PlayerRewardTransactionState::requireTransactionId);
        for (String id : pendingPlans.keySet()) {
            if (completedTransactionIds.contains(id)) {
                throw new IllegalArgumentException(
                        "Reward transaction cannot be pending and completed: " + id
                );
            }
        }
    }

    public static PlayerRewardTransactionState initial() {
        return new PlayerRewardTransactionState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Set.of()
        );
    }

    public boolean isCompleted(String transactionId) {
        requireTransactionId(transactionId);
        return completedTransactionIds.contains(transactionId);
    }

    public Optional<RewardPlan> pendingPlan(String transactionId) {
        requireTransactionId(transactionId);
        return Optional.ofNullable(pendingPlans.get(transactionId));
    }

    public PlayerRewardTransactionState begin(
            String transactionId,
            RewardPlan plan
    ) {
        requireTransactionId(transactionId);
        Objects.requireNonNull(plan, "plan");

        if (completedTransactionIds.contains(transactionId)) {
            return this;
        }

        RewardPlan existing = pendingPlans.get(transactionId);
        if (existing != null) {
            if (!existing.equals(plan)) {
                throw new IllegalStateException(
                        "Pending reward plan cannot be rerolled/recomputed for " + transactionId
                );
            }
            return this;
        }

        Map<String, RewardPlan> next = new HashMap<>(pendingPlans);
        next.put(transactionId, plan);
        return new PlayerRewardTransactionState(
                schemaVersion,
                Map.copyOf(next),
                completedTransactionIds
        );
    }

    public PlayerRewardTransactionState complete(String transactionId) {
        requireTransactionId(transactionId);
        if (completedTransactionIds.contains(transactionId)) {
            return this;
        }
        if (!pendingPlans.containsKey(transactionId)) {
            throw new IllegalStateException(
                    "Cannot complete reward transaction without a persisted plan: " + transactionId
            );
        }

        Map<String, RewardPlan> nextPending = new HashMap<>(pendingPlans);
        nextPending.remove(transactionId);
        Set<String> nextCompleted = new HashSet<>(completedTransactionIds);
        nextCompleted.add(transactionId);
        return new PlayerRewardTransactionState(
                schemaVersion,
                Map.copyOf(nextPending),
                Set.copyOf(nextCompleted)
        );
    }

    public record RewardPlan(
            long combatXp,
            RootClass rewardClass,
            long classXp,
            long gold
    ) {
        public static final Codec<RewardPlan> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("combat_xp").forGetter(RewardPlan::combatXp),
                        RootClass.CODEC.fieldOf("reward_class").forGetter(RewardPlan::rewardClass),
                        Codec.LONG.fieldOf("class_xp").forGetter(RewardPlan::classXp),
                        Codec.LONG.fieldOf("gold").forGetter(RewardPlan::gold)
                ).apply(instance, RewardPlan::new)
        );

        public RewardPlan {
            if (combatXp < 0L || classXp < 0L || gold < 0L) {
                throw new IllegalArgumentException("Reward amounts cannot be negative.");
            }
            Objects.requireNonNull(rewardClass, "rewardClass");
            if (combatXp == 0L && classXp == 0L && gold == 0L) {
                throw new IllegalArgumentException("Reward plan must contain at least one reward.");
            }
        }
    }

    private static void requireTransactionId(String transactionId) {
        if (transactionId == null
                || transactionId.isBlank()
                || !transactionId.contains(":")) {
            throw new IllegalArgumentException(
                    "Reward transaction ID must be a stable namespaced ID."
            );
        }
    }
}
