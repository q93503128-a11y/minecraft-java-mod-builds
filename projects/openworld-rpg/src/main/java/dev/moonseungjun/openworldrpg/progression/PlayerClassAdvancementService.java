package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineProjectSkillAccess;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerClassAdvancementService {
    private PlayerClassAdvancementService() {
    }

    public static PlayerClassAdvancementState state(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerClassAdvancementAttachments.CLASS_ADVANCEMENT,
                PlayerClassAdvancementState.initial()
        );
    }

    public static TrialResult completeSpecializationTrial(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(specialization, "specialization");

        var progression = PlayerProgressionService.state(player);
        RootClass rootClass = specialization.rootClass();
        if (!progression.activeClass().equals(Optional.of(rootClass))) {
            return new TrialResult(
                    TrialStatus.WRONG_ACTIVE_ROOT,
                    specialization,
                    progression.classProgress(rootClass).rank(),
                    false
            );
        }

        int rank = progression.classProgress(rootClass).rank();
        if (rank < 10) {
            return new TrialResult(
                    TrialStatus.RANK_10_REQUIRED,
                    specialization,
                    rank,
                    false
            );
        }

        PlayerClassAdvancementState current = state(player);
        RootClassSpecializationState root = current.rootState(rootClass);
        if (root.isUnlocked(specialization)) {
            return new TrialResult(
                    TrialStatus.ALREADY_UNLOCKED,
                    specialization,
                    rank,
                    false
            );
        }

        if (!root.unlocked().isEmpty() && rank < 20) {
            return new TrialResult(
                    TrialStatus.RANK_20_REQUIRED_FOR_SIBLING,
                    specialization,
                    rank,
                    false
            );
        }

        boolean firstBranch = root.unlocked().isEmpty();
        PlayerClassAdvancementState next =
                current.unlockSpecialization(specialization);
        replace(player, next);
        SpellEngineProjectSkillAccess.refreshPublishedSkills(player);

        return new TrialResult(
                firstBranch
                        ? TrialStatus.UNLOCKED_AND_ACTIVATED
                        : TrialStatus.SIBLING_UNLOCKED,
                specialization,
                rank,
                firstBranch
        );
    }

    public static long currentBranchSwitchCost(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        int level = PlayerProgressionService.state(player)
                .combatLevel();
        return ProjectProgressionRules.branchSwitchGoldCost(level);
    }

    public static BranchSwitchResult requestBranchSwitch(
            ServerPlayer player,
            ClassSpecialization target,
            BranchSwitchContext context
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(context, "context");

        PlayerClassAdvancementState current = state(player);
        var pending = current.pendingBranchSwitch().orElse(null);
        if (pending != null) {
            if (pending.target() != target) {
                return BranchSwitchResult.rejected(
                        BranchSwitchStatus.OTHER_SWITCH_PENDING,
                        target,
                        pending.goldCost(),
                        PlayerCurrencyService.state(player).gold()
                );
            }
            return reconcilePendingBranchSwitch(player);
        }

        if (!context.atApprovedFacility()) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.FACILITY_REQUIRED,
                    target,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }
        if (!context.outOfCombat()) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.COMBAT_LOCKED,
                    target,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        var progression = PlayerProgressionService.state(player);
        if (!progression.activeClass().equals(
                Optional.of(target.rootClass())
        )) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.WRONG_ACTIVE_ROOT,
                    target,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        RootClassSpecializationState root =
                current.rootState(target.rootClass());
        ClassSpecialization source = root.active().orElse(null);
        if (source == null) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.NO_ACTIVE_SPECIALIZATION,
                    target,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }
        if (source == target) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.ALREADY_ACTIVE,
                    target,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }
        if (!root.isUnlocked(target)) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.TARGET_LOCKED,
                    target,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        long cost = currentBranchSwitchCost(player);
        long gold = PlayerCurrencyService.state(player).gold();
        if (gold < cost) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.INSUFFICIENT_GOLD,
                    target,
                    cost,
                    gold
            );
        }

        replace(
                player,
                current.prepareBranchSwitch(target, cost)
        );
        return reconcilePendingBranchSwitch(player);
    }

    public static BranchSwitchResult reconcilePendingBranchSwitch(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        PlayerClassAdvancementState current = state(player);
        var pending = current.pendingBranchSwitch().orElse(null);
        if (pending == null) {
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.NOTHING_PENDING,
                    null,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        boolean debitAlreadyApplied =
                PlayerCurrencyService.state(player)
                        .hasAppliedDebit(
                                pending.debitTransactionId()
                        );

        RootClass activeRoot = PlayerProgressionService
                .state(player)
                .activeClass()
                .orElse(null);
        if (activeRoot != pending.rootClass()
                && !debitAlreadyApplied) {
            replace(
                    player,
                    current.cancelBranchSwitch(
                            pending.transactionId()
                    )
            );
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.WRONG_ACTIVE_ROOT,
                    pending.target(),
                    pending.goldCost(),
                    PlayerCurrencyService.state(player).gold()
            );
        }

        RootClassSpecializationState root =
                current.rootState(pending.rootClass());
        if (root.active().equals(Optional.of(pending.target()))) {
            replace(
                    player,
                    current.commitBranchSwitch(
                            pending.transactionId()
                    )
            );
            SpellEngineProjectSkillAccess.refreshPublishedSkills(player);
            return new BranchSwitchResult(
                    BranchSwitchStatus.SWITCHED,
                    pending.target(),
                    pending.goldCost(),
                    PlayerCurrencyService.state(player).gold(),
                    pending.transactionId()
            );
        }

        if (!root.active().equals(Optional.of(pending.source()))
                && !debitAlreadyApplied) {
            replace(
                    player,
                    current.cancelBranchSwitch(
                            pending.transactionId()
                    )
            );
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.SOURCE_SPECIALIZATION_CHANGED,
                    pending.target(),
                    pending.goldCost(),
                    PlayerCurrencyService.state(player).gold()
            );
        }

        var debit = PlayerCurrencyService.debitOnce(
                player,
                pending.debitTransactionId(),
                pending.goldCost()
        );
        if (!debit.success()) {
            replace(
                    player,
                    state(player).cancelBranchSwitch(
                            pending.transactionId()
                    )
            );
            return BranchSwitchResult.rejected(
                    BranchSwitchStatus.INSUFFICIENT_GOLD,
                    pending.target(),
                    pending.goldCost(),
                    debit.state().gold()
            );
        }

        PlayerClassAdvancementState committed =
                state(player).commitBranchSwitch(
                        pending.transactionId()
                );
        replace(player, committed);
        SpellEngineProjectSkillAccess.refreshPublishedSkills(player);
        return new BranchSwitchResult(
                BranchSwitchStatus.SWITCHED,
                pending.target(),
                pending.goldCost(),
                PlayerCurrencyService.state(player).gold(),
                pending.transactionId()
        );
    }

    private static PlayerClassAdvancementState replace(
            ServerPlayer player,
            PlayerClassAdvancementState next
    ) {
        PlayerClassAdvancementState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(
                    PlayerClassAdvancementAttachments.CLASS_ADVANCEMENT,
                    next
            );
        }
        return next;
    }

    public enum TrialStatus {
        UNLOCKED_AND_ACTIVATED,
        SIBLING_UNLOCKED,
        ALREADY_UNLOCKED,
        WRONG_ACTIVE_ROOT,
        RANK_10_REQUIRED,
        RANK_20_REQUIRED_FOR_SIBLING
    }

    public record TrialResult(
            TrialStatus status,
            ClassSpecialization specialization,
            int classRank,
            boolean activated
    ) {
        public TrialResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(
                    specialization,
                    "specialization"
            );
            if (classRank < 1
                    || classRank
                            > ProjectProgressionRules.MAX_CLASS_RANK) {
                throw new IllegalArgumentException(
                        "Trial result Class Rank is invalid."
                );
            }
            if (activated
                    != (status
                            == TrialStatus.UNLOCKED_AND_ACTIVATED)) {
                throw new IllegalArgumentException(
                        "Only the first specialization unlock activates immediately."
                );
            }
        }

        public boolean unlocked() {
            return status == TrialStatus.UNLOCKED_AND_ACTIVATED
                    || status == TrialStatus.SIBLING_UNLOCKED
                    || status == TrialStatus.ALREADY_UNLOCKED;
        }
    }

    public record BranchSwitchContext(
            boolean atApprovedFacility,
            boolean outOfCombat
    ) {
    }

    public enum BranchSwitchStatus {
        SWITCHED,
        FACILITY_REQUIRED,
        COMBAT_LOCKED,
        WRONG_ACTIVE_ROOT,
        NO_ACTIVE_SPECIALIZATION,
        TARGET_LOCKED,
        ALREADY_ACTIVE,
        INSUFFICIENT_GOLD,
        OTHER_SWITCH_PENDING,
        SOURCE_SPECIALIZATION_CHANGED,
        NOTHING_PENDING
    }

    public record BranchSwitchResult(
            BranchSwitchStatus status,
            ClassSpecialization target,
            long goldCost,
            long goldAfter,
            String transactionId
    ) {
        public BranchSwitchResult {
            Objects.requireNonNull(status, "status");
            if (goldCost < 0L) {
                throw new IllegalArgumentException(
                        "Branch-switch cost must be non-negative."
                );
            }
            if (status == BranchSwitchStatus.SWITCHED) {
                Objects.requireNonNull(target, "target");
                if (goldCost <= 0L
                        || transactionId == null
                        || transactionId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Successful branch switch requires target, cost and transaction id."
                    );
                }
            } else if (transactionId != null) {
                throw new IllegalArgumentException(
                        "Rejected branch switch cannot expose transaction id."
                );
            }
        }

        public static BranchSwitchResult rejected(
                BranchSwitchStatus status,
                ClassSpecialization target,
                long goldCost,
                long goldAfter
        ) {
            if (status == BranchSwitchStatus.SWITCHED) {
                throw new IllegalArgumentException(
                        "Use successful result constructor for SWITCHED."
                );
            }
            return new BranchSwitchResult(
                    status,
                    target,
                    goldCost,
                    goldAfter,
                    null
            );
        }

        public boolean switched() {
            return status == BranchSwitchStatus.SWITCHED;
        }
    }
}
