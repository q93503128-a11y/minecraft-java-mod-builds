package dev.moonseungjun.openworldrpg.progression.reward;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-only coordinator for reconnect-safe one-time reward packages. */
public final class PlayerRewardTransactionService {
    private PlayerRewardTransactionService() {
    }

    public static PlayerRewardTransactionState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerRewardTransactionAttachments.REWARD_TRANSACTIONS,
                PlayerRewardTransactionState.initial()
        );
    }

    public static RewardResult grantPercentageRewardOnce(
            ServerPlayer player,
            String transactionId,
            RootClass rewardClass,
            double combatRequirementFraction,
            double classRequirementFraction,
            long gold
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rewardClass, "rewardClass");
        validateFraction(combatRequirementFraction, "combatRequirementFraction");
        validateFraction(classRequirementFraction, "classRequirementFraction");
        if (gold < 0L) {
            throw new IllegalArgumentException("Gold reward cannot be negative.");
        }

        PlayerRewardTransactionState transactions = state(player);
        if (transactions.isCompleted(transactionId)) {
            return new RewardResult(null, false);
        }

        var pending = transactions.pendingPlan(transactionId);
        PlayerRewardTransactionState.RewardPlan plan;
        if (pending.isPresent()) {
            plan = pending.get();
            if (plan.rewardClass() != rewardClass) {
                throw new IllegalStateException(
                        "Pending reward class changed for transaction " + transactionId
                );
            }
        } else {
            var progression = PlayerProgressionService.state(player);
            long combatXp = progression.combatLevel() >= ProjectProgressionRules.MAX_COMBAT_LEVEL
                    ? 0L
                    : Math.round(
                            ProjectProgressionRules.combatXpToNext(progression.combatLevel())
                                    * combatRequirementFraction
                    );
            var classProgress = progression.classProgress(rewardClass);
            long classXp = classProgress.rank() >= ProjectProgressionRules.MAX_CLASS_RANK
                    ? 0L
                    : Math.round(
                            ProjectProgressionRules.classXpToNext(classProgress.rank())
                                    * classRequirementFraction
                    );

            plan = new PlayerRewardTransactionState.RewardPlan(
                    combatXp,
                    rewardClass,
                    classXp,
                    gold
            );
            transactions = transactions.begin(transactionId, plan);
            player.setAttached(
                    PlayerRewardTransactionAttachments.REWARD_TRANSACTIONS,
                    transactions
            );
        }

        applyPlan(player, transactionId, plan);
        PlayerRewardTransactionState completed = state(player).complete(transactionId);
        player.setAttached(
                PlayerRewardTransactionAttachments.REWARD_TRANSACTIONS,
                completed
        );
        return new RewardResult(plan, true);
    }

    public static int resumePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        int resumed = 0;
        for (String transactionId : new ArrayList<>(state(player).pendingPlans().keySet())) {
            PlayerRewardTransactionState.RewardPlan plan = state(player)
                    .pendingPlan(transactionId)
                    .orElse(null);
            if (plan == null) {
                continue;
            }
            applyPlan(player, transactionId, plan);
            player.setAttached(
                    PlayerRewardTransactionAttachments.REWARD_TRANSACTIONS,
                    state(player).complete(transactionId)
            );
            resumed++;
        }
        return resumed;
    }

    private static void applyPlan(
            ServerPlayer player,
            String transactionId,
            PlayerRewardTransactionState.RewardPlan plan
    ) {
        if (plan.combatXp() > 0L) {
            PlayerProgressionService.creditCombatXpOnce(
                    player,
                    transactionId + "/combat_xp",
                    plan.combatXp()
            );
        }
        if (plan.classXp() > 0L) {
            PlayerProgressionService.creditClassXpOnce(
                    player,
                    transactionId + "/class_xp",
                    plan.rewardClass(),
                    plan.classXp()
            );
        }
        if (plan.gold() > 0L) {
            PlayerCurrencyService.creditOnce(
                    player,
                    transactionId + "/gold",
                    plan.gold()
            );
        }
    }

    private static void validateFraction(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }

    public record RewardResult(
            PlayerRewardTransactionState.RewardPlan plan,
            boolean appliedNow
    ) {
    }
}
