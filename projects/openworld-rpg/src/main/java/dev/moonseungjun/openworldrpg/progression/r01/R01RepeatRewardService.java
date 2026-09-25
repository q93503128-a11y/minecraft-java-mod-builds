package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Reconnect-safe, bounded repeatable reward authority for R01 world events. */
public final class R01RepeatRewardService {
    public static final double ROADSIDE_COMBAT_XP_FRACTION = 0.05;
    public static final double ROADSIDE_CLASS_XP_FRACTION = 0.04;
    public static final long ROADSIDE_GOLD = 20L;

    private R01RepeatRewardService() {
    }

    public static R01RepeatRewardState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01RepeatRewardAttachments.REPEAT_REWARDS,
                R01RepeatRewardState.initial()
        );
    }

    public static RewardResult grantRoadsideTrouble(
            ServerPlayer player,
            long cycle,
            Optional<RootClass> contributionClass
    ) {
        Objects.requireNonNull(player, "player");
        contributionClass = Objects.requireNonNull(
                contributionClass,
                "contributionClass"
        );
        if (cycle <= 0L) {
            throw new IllegalArgumentException("Roadside reward cycle must be positive.");
        }

        R01RepeatRewardState current = state(player);
        if (cycle <= current.lastRoadsideRewardedCycle()) {
            cleanupComponentReceipts(player, cycle);
            return new RewardResult(null, false);
        }

        R01RepeatRewardState.RoadsideRewardPlan plan;
        if (current.pendingRoadsideReward().isPresent()) {
            plan = current.pendingRoadsideReward().orElseThrow();
            if (plan.cycle() != cycle
                    || !plan.rewardClass().equals(contributionClass)) {
                throw new IllegalStateException(
                        "Pending Roadside reward does not match event participation snapshot."
                );
            }
        } else {
            var progression = PlayerProgressionService.state(player);
            long combatXp = progression.combatLevel()
                    >= ProjectProgressionRules.MAX_COMBAT_LEVEL
                    ? 0L
                    : Math.round(
                            ProjectProgressionRules.combatXpToNext(
                                    progression.combatLevel()
                            ) * ROADSIDE_COMBAT_XP_FRACTION
                    );

            long classXp = 0L;
            if (contributionClass.isPresent()) {
                RootClass rootClass = contributionClass.orElseThrow();
                var classProgress = progression.classProgress(rootClass);
                if (classProgress.rank() < ProjectProgressionRules.MAX_CLASS_RANK) {
                    classXp = Math.round(
                            ProjectProgressionRules.classXpToNext(
                                    classProgress.rank()
                            ) * ROADSIDE_CLASS_XP_FRACTION
                    );
                }
            }

            plan = new R01RepeatRewardState.RoadsideRewardPlan(
                    cycle,
                    combatXp,
                    contributionClass,
                    classXp,
                    ROADSIDE_GOLD
            );
            replace(player, current.beginRoadside(plan));
        }

        applyPlan(player, plan);
        replace(player, state(player).completeRoadside(cycle));
        cleanupComponentReceipts(player, cycle);
        return new RewardResult(plan, true);
    }

    private static void applyPlan(
            ServerPlayer player,
            R01RepeatRewardState.RoadsideRewardPlan plan
    ) {
        String prefix = transactionPrefix(plan.cycle());
        if (plan.combatXp() > 0L) {
            PlayerProgressionService.creditCombatXpOnce(
                    player,
                    prefix + "/combat_xp",
                    plan.combatXp()
            );
        }
        if (plan.classXp() > 0L) {
            PlayerProgressionService.creditClassXpOnce(
                    player,
                    prefix + "/class_xp",
                    plan.rewardClass().orElseThrow(),
                    plan.classXp()
            );
        }
        if (plan.gold() > 0L) {
            PlayerCurrencyService.creditOnce(
                    player,
                    prefix + "/gold",
                    plan.gold()
            );
        }
    }

    private static void cleanupComponentReceipts(
            ServerPlayer player,
            long cycle
    ) {
        String prefix = transactionPrefix(cycle);
        PlayerProgressionService.forgetCombatXpTransaction(
                player,
                prefix + "/combat_xp"
        );
        PlayerProgressionService.forgetClassXpTransaction(
                player,
                prefix + "/class_xp"
        );
        PlayerCurrencyService.forgetCreditTransaction(
                player,
                prefix + "/gold"
        );
    }

    private static String transactionPrefix(long cycle) {
        return "openworld_rpg:r01/roadside_trouble/reward/" + cycle;
    }

    private static R01RepeatRewardState replace(
            ServerPlayer player,
            R01RepeatRewardState next
    ) {
        R01RepeatRewardState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(R01RepeatRewardAttachments.REPEAT_REWARDS, next);
        return next;
    }

    public record RewardResult(
            R01RepeatRewardState.RoadsideRewardPlan plan,
            boolean appliedNow
    ) {
    }
}
