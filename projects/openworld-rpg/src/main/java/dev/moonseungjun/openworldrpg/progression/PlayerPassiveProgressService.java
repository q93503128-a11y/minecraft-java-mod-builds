package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerPassiveProgressService {
    private static final ClassPassiveCatalog CATALOG = ClassPassiveCatalog.bundled();

    private PlayerPassiveProgressService() {
    }

    public static PlayerPassiveProgressState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerPassiveProgressState state = player.getAttachedOrSet(
                PlayerPassiveProgressAttachments.PASSIVE_PROGRESS,
                PlayerPassiveProgressState.initial()
        );
        state.validateAgainst(CATALOG);
        return state;
    }

    public static int availablePoints(ServerPlayer player, RootClass rootClass) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rootClass, "rootClass");
        int rank = PlayerProgressionService.state(player).classProgress(rootClass).rank();
        return ClassPassiveAllocationRules.availablePoints(
                rank,
                state(player).completedInsightCount(rootClass)
        );
    }

    public static AllocationResult allocatePoint(
            ServerPlayer player,
            String nodeId,
            boolean outOfCombat
    ) {
        Objects.requireNonNull(player, "player");
        if (!outOfCombat) {
            return new AllocationResult(AllocationStatus.COMBAT_LOCKED, nodeId, 0);
        }
        RootClass activeRoot = PlayerProgressionService.state(player).activeClass().orElse(null);
        if (activeRoot == null) {
            return new AllocationResult(AllocationStatus.NO_ACTIVE_CLASS, nodeId, 0);
        }

        int rank = PlayerProgressionService.state(player).classProgress(activeRoot).rank();
        PlayerPassiveProgressState state = state(player);
        ClassPassiveAllocationRules.Decision decision =
                ClassPassiveAllocationRules.evaluateAllocation(
                        CATALOG,
                        state,
                        PlayerClassAdvancementService.state(player),
                        PlayerClassMilestoneService.state(player),
                        activeRoot,
                        rank,
                        nodeId
                );
        if (decision != ClassPassiveAllocationRules.Decision.ALLOWED) {
            return new AllocationResult(
                    AllocationStatus.valueOf(decision.name()),
                    nodeId,
                    state.allocationRank(nodeId)
            );
        }

        ClassPassiveNodeSpec node = CATALOG.require(nodeId);
        int nextRank = state.allocationRank(node.id()) + 1;
        replace(player, state.withAllocationRank(node.id(), nextRank));
        return new AllocationResult(AllocationStatus.ALLOCATED, node.id(), nextRank);
    }

    public static InsightResult completeInsight(ServerPlayer player, ClassInsight insight) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(insight, "insight");
        RootClass activeRoot = PlayerProgressionService.state(player).activeClass().orElse(null);
        if (activeRoot != insight.rootClass()) {
            return new InsightResult(InsightStatus.WRONG_ACTIVE_ROOT, insight, false);
        }

        PlayerPassiveProgressState state = state(player);
        if (state.completedInsightIds().contains(insight.id())) {
            return new InsightResult(InsightStatus.ALREADY_COMPLETED, insight, false);
        }
        if (!state.pendingInsightIds().contains(insight.id())) {
            replace(player, state.beginInsight(insight));
        }
        return reconcileInsight(player, insight);
    }

    public static int reconcilePendingInsights(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        int completed = 0;
        for (String id : new ArrayList<>(state(player).pendingInsightIds())) {
            ClassInsight insight = ClassInsight.byId(id).orElseThrow(
                    () -> new IllegalStateException("Unknown pending Class Insight: " + id)
            );
            if (reconcileInsight(player, insight).status() == InsightStatus.COMPLETED) {
                completed++;
            }
        }
        return completed;
    }

    public static long currentRespecCost(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return ProjectProgressionRules.passiveRespecGoldCost(
                PlayerProgressionService.state(player).combatLevel()
        );
    }

    public static RespecResult requestRespec(ServerPlayer player, RespecContext context) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");
        if (!context.atApprovedFacility()) {
            return RespecResult.rejected(
                    RespecStatus.FACILITY_REQUIRED, 0L, PlayerCurrencyService.state(player).gold()
            );
        }
        if (!context.outOfCombat()) {
            return RespecResult.rejected(
                    RespecStatus.COMBAT_LOCKED, 0L, PlayerCurrencyService.state(player).gold()
            );
        }

        RootClass activeRoot = PlayerProgressionService.state(player).activeClass().orElse(null);
        if (activeRoot == null) {
            return RespecResult.rejected(
                    RespecStatus.NO_ACTIVE_CLASS, 0L, PlayerCurrencyService.state(player).gold()
            );
        }
        Optional<ClassSpecialization> activeBranch =
                PlayerClassAdvancementService.state(player).rootState(activeRoot).active();

        PlayerPassiveProgressState state = state(player);
        if (state.pendingRespec().isPresent()) {
            return reconcilePendingRespec(player);
        }
        if (!hasAllocationsFor(state, activeRoot, activeBranch)) {
            return RespecResult.rejected(
                    RespecStatus.NOTHING_TO_RESPEC, 0L, PlayerCurrencyService.state(player).gold()
            );
        }

        long cost = currentRespecCost(player);
        long gold = PlayerCurrencyService.state(player).gold();
        if (gold < cost) {
            return RespecResult.rejected(RespecStatus.INSUFFICIENT_GOLD, cost, gold);
        }

        replace(player, state.prepareRespec(activeRoot, activeBranch, cost));
        return reconcilePendingRespec(player);
    }

    public static RespecResult reconcilePendingRespec(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerPassiveProgressState state = state(player);
        var pending = state.pendingRespec().orElse(null);
        if (pending == null) {
            return RespecResult.rejected(
                    RespecStatus.NOTHING_PENDING, 0L, PlayerCurrencyService.state(player).gold()
            );
        }

        boolean debitApplied = PlayerCurrencyService.state(player)
                .hasAppliedDebit(pending.debitTransactionId());
        RootClass currentRoot = PlayerProgressionService.state(player).activeClass().orElse(null);
        Optional<ClassSpecialization> currentBranch =
                currentRoot == pending.rootClass()
                        ? PlayerClassAdvancementService.state(player).rootState(currentRoot).active()
                        : Optional.empty();

        if (!debitApplied
                && (currentRoot != pending.rootClass()
                || !currentBranch.equals(pending.activeBranch()))) {
            replace(player, state.cancelRespec(pending.transactionId()));
            return RespecResult.rejected(
                    RespecStatus.BUILD_CHANGED_BEFORE_DEBIT,
                    pending.goldCost(),
                    PlayerCurrencyService.state(player).gold()
            );
        }

        var debit = PlayerCurrencyService.debitOnce(
                player, pending.debitTransactionId(), pending.goldCost()
        );
        if (!debit.success()) {
            replace(player, state(player).cancelRespec(pending.transactionId()));
            return RespecResult.rejected(
                    RespecStatus.INSUFFICIENT_GOLD,
                    pending.goldCost(),
                    debit.state().gold()
            );
        }

        PlayerPassiveProgressState latest = state(player);
        Map<String, Integer> next = new HashMap<>(latest.allocationRanks());
        next.entrySet().removeIf(entry -> {
            ClassPassiveNodeSpec node = CATALOG.require(entry.getKey());
            if (node.rootClass() != pending.rootClass()) {
                return false;
            }
            if (node.rootNode()) {
                return true;
            }
            return pending.activeBranch().isPresent()
                    && node.specialization().equals(pending.activeBranch());
        });

        replace(
                player,
                latest.commitRespec(pending.transactionId(), Map.copyOf(next))
        );
        return new RespecResult(
                RespecStatus.RESPEC_COMPLETE,
                pending.goldCost(),
                PlayerCurrencyService.state(player).gold(),
                pending.transactionId()
        );
    }

    public static void reconcilePending(ServerPlayer player) {
        reconcilePendingInsights(player);
        if (state(player).pendingRespec().isPresent()) {
            reconcilePendingRespec(player);
        }
    }

    private static InsightResult reconcileInsight(ServerPlayer player, ClassInsight insight) {
        PlayerPassiveProgressState before = state(player);
        if (before.completedInsightIds().contains(insight.id())) {
            return new InsightResult(InsightStatus.ALREADY_COMPLETED, insight, false);
        }
        if (!before.pendingInsightIds().contains(insight.id())) {
            return new InsightResult(InsightStatus.NOT_PENDING, insight, false);
        }

        int completedBefore = before.completedInsightCount(insight.rootClass());
        PlayerRewardTransactionService.grantPercentageRewardOnce(
                player,
                insight.rewardTransactionId(),
                insight.rootClass(),
                0.0,
                0.08,
                0L
        );

        PlayerPassiveProgressState afterReward = state(player);
        if (!afterReward.completedInsightIds().contains(insight.id())) {
            replace(player, afterReward.commitInsight(insight));
        }
        return new InsightResult(
                InsightStatus.COMPLETED,
                insight,
                completedBefore < ClassPassiveAllocationRules.MAX_INSIGHT_POINTS
        );
    }

    private static boolean hasAllocationsFor(
            PlayerPassiveProgressState state,
            RootClass rootClass,
            Optional<ClassSpecialization> activeBranch
    ) {
        for (var entry : state.allocationRanks().entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            ClassPassiveNodeSpec node = CATALOG.require(entry.getKey());
            if (node.rootClass() != rootClass) {
                continue;
            }
            if (node.rootNode()
                    || (activeBranch.isPresent() && node.specialization().equals(activeBranch))) {
                return true;
            }
        }
        return false;
    }

    private static void replace(ServerPlayer player, PlayerPassiveProgressState next) {
        next.validateAgainst(CATALOG);
        if (!state(player).equals(next)) {
            player.setAttached(PlayerPassiveProgressAttachments.PASSIVE_PROGRESS, next);
        }
    }

    public enum AllocationStatus {
        ALLOCATED,
        COMBAT_LOCKED,
        NO_ACTIVE_CLASS,
        WRONG_ACTIVE_ROOT,
        BRANCH_INACTIVE,
        MAX_RANK_REACHED,
        BRANCH_I_LOCKED,
        BRANCH_II_LOCKED,
        BRANCH_III_LOCKED,
        CAPSTONE_LOCKED,
        INSUFFICIENT_POINTS
    }

    public record AllocationResult(AllocationStatus status, String nodeId, int rankAfter) {
    }

    public enum InsightStatus {
        COMPLETED,
        ALREADY_COMPLETED,
        WRONG_ACTIVE_ROOT,
        NOT_PENDING
    }

    public record InsightResult(
            InsightStatus status,
            ClassInsight insight,
            boolean passivePointAwarded
    ) {
        public InsightResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(insight, "insight");
            if (passivePointAwarded && status != InsightStatus.COMPLETED) {
                throw new IllegalArgumentException(
                        "Only a newly completed Insight can award a Passive Point."
                );
            }
        }
    }

    public record RespecContext(boolean atApprovedFacility, boolean outOfCombat) {
    }

    public enum RespecStatus {
        RESPEC_COMPLETE,
        FACILITY_REQUIRED,
        COMBAT_LOCKED,
        NO_ACTIVE_CLASS,
        NOTHING_TO_RESPEC,
        INSUFFICIENT_GOLD,
        BUILD_CHANGED_BEFORE_DEBIT,
        NOTHING_PENDING
    }

    public record RespecResult(
            RespecStatus status,
            long goldCost,
            long goldAfter,
            String transactionId
    ) {
        public RespecResult {
            Objects.requireNonNull(status, "status");
            if (goldCost < 0L) {
                throw new IllegalArgumentException("Passive respec cost cannot be negative.");
            }
            if (status == RespecStatus.RESPEC_COMPLETE) {
                if (goldCost <= 0L || transactionId == null || transactionId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Completed respec requires cost and transaction id."
                    );
                }
            } else if (transactionId != null) {
                throw new IllegalArgumentException(
                        "Rejected respec cannot expose transaction id."
                );
            }
        }

        public static RespecResult rejected(
                RespecStatus status,
                long goldCost,
                long goldAfter
        ) {
            if (status == RespecStatus.RESPEC_COMPLETE) {
                throw new IllegalArgumentException(
                        "Use successful respec result for RESPEC_COMPLETE."
                );
            }
            return new RespecResult(status, goldCost, goldAfter, null);
        }
    }
}
