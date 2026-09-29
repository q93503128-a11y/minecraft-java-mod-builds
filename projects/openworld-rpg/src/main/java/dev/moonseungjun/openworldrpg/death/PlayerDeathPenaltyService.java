package dev.moonseungjun.openworldrpg.death;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative automatic death-penalty transaction.
 *
 * <p>The opening approach remains penalty-free until the first shrine is activated. Later deaths
 * remove only current-Lv EXP when any exists; otherwise they apply the canonical Lv-scaled Gold
 * fallback, which may create a negative balance. Already-earned combat Lv is never reduced.</p>
 */
public final class PlayerDeathPenaltyService {
    private PlayerDeathPenaltyService() {
    }

    public static PlayerDeathPenaltyState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerDeathPenaltyAttachments.DEATH_PENALTY,
                PlayerDeathPenaltyState.initial()
        );
    }

    public static PenaltyResult applyAfterDeathRespawn(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");

        if (state(player).pendingPenalty().isPresent()) {
            return reconcilePending(player);
        }

        if (!R01PlayerStateService.state(player)
                .opening()
                .firstShrineActivated()) {
            return PenaltyResult.openingFree();
        }

        var progression = PlayerProgressionService.state(player);
        long currentXp = progression.combatXp();
        PlayerDeathPenaltyState prepared;

        if (currentXp > 0L) {
            long loss = ProjectDeathPenaltyRules.currentLevelXpLoss(
                    progression.combatLevel(),
                    currentXp
            );
            prepared = state(player).prepare(
                    PlayerDeathPenaltyState.PenaltyKind.CURRENT_LEVEL_XP,
                    progression.combatLevel(),
                    currentXp,
                    loss
            );
        } else {
            long cost = ProjectDeathPenaltyRules.goldFallbackCost(
                    progression.combatLevel()
            );
            prepared = state(player).prepare(
                    PlayerDeathPenaltyState.PenaltyKind.GOLD,
                    progression.combatLevel(),
                    PlayerCurrencyService.state(player).gold(),
                    cost
            );
        }

        replace(player, prepared);
        return reconcilePending(player);
    }

    public static PenaltyResult reconcilePending(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        PlayerDeathPenaltyState current = state(player);
        var pending = current.pendingPenalty().orElse(null);
        if (pending == null) {
            return PenaltyResult.nothingPending();
        }

        return switch (pending.kind()) {
            case CURRENT_LEVEL_XP ->
                    reconcileXpPenalty(player, current, pending);
            case GOLD ->
                    reconcileGoldPenalty(player, current, pending);
        };
    }

    private static PenaltyResult reconcileXpPenalty(
            ServerPlayer player,
            PlayerDeathPenaltyState state,
            PlayerDeathPenaltyState.PendingPenalty pending
    ) {
        var progression = PlayerProgressionService.state(player);
        long currentXp = progression.combatXp();

        if (currentXp == pending.beforeValue()) {
            PlayerProgressionService.applyCurrentLevelXpLoss(
                    player,
                    pending.beforeValue(),
                    pending.afterValue()
            );
        } else if (currentXp != pending.afterValue()) {
            return PenaltyResult.diverged(pending);
        }

        replace(
                player,
                state.commit(pending.transactionId())
        );
        return new PenaltyResult(
                PenaltyStatus.CURRENT_LEVEL_XP_REMOVED,
                pending.kind(),
                pending.amount(),
                pending.afterValue(),
                pending.transactionId()
        );
    }

    private static PenaltyResult reconcileGoldPenalty(
            ServerPlayer player,
            PlayerDeathPenaltyState state,
            PlayerDeathPenaltyState.PendingPenalty pending
    ) {
        var currency = PlayerCurrencyService.state(player);
        boolean alreadyApplied = currency.hasAppliedDebit(
                pending.effectTransactionId()
        );

        if (!alreadyApplied
                && currency.gold() != pending.beforeValue()) {
            return PenaltyResult.diverged(pending);
        }

        var debit = PlayerCurrencyService.debitIntoDebtOnce(
                player,
                pending.effectTransactionId(),
                pending.amount()
        );
        if (!debit.success()) {
            throw new IllegalStateException(
                    "Death Gold debit must never reject for insufficient Gold."
            );
        }

        replace(
                player,
                state.commit(pending.transactionId())
        );
        return new PenaltyResult(
                PenaltyStatus.GOLD_DEDUCTED,
                pending.kind(),
                pending.amount(),
                debit.state().gold(),
                pending.transactionId()
        );
    }

    private static PlayerDeathPenaltyState replace(
            ServerPlayer player,
            PlayerDeathPenaltyState next
    ) {
        PlayerDeathPenaltyState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(
                    PlayerDeathPenaltyAttachments.DEATH_PENALTY,
                    next
            );
        }
        return next;
    }

    public enum PenaltyStatus {
        CURRENT_LEVEL_XP_REMOVED,
        GOLD_DEDUCTED,
        OPENING_FREE,
        NOTHING_PENDING,
        STATE_DIVERGED
    }

    public record PenaltyResult(
            PenaltyStatus status,
            PlayerDeathPenaltyState.PenaltyKind kind,
            long amount,
            long resultingValue,
            String transactionId
    ) {
        public PenaltyResult {
            Objects.requireNonNull(status, "status");
            if (amount < 0L) {
                throw new IllegalArgumentException(
                        "Death-penalty amount cannot be negative."
                );
            }
            if (status == PenaltyStatus.CURRENT_LEVEL_XP_REMOVED
                    || status == PenaltyStatus.GOLD_DEDUCTED) {
                Objects.requireNonNull(kind, "kind");
                if (amount <= 0L
                        || transactionId == null
                        || transactionId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Applied death penalty requires payload."
                    );
                }
            } else if (transactionId != null) {
                throw new IllegalArgumentException(
                        "Non-applied death penalty cannot expose transaction id."
                );
            }
        }

        public static PenaltyResult openingFree() {
            return new PenaltyResult(
                    PenaltyStatus.OPENING_FREE,
                    null,
                    0L,
                    0L,
                    null
            );
        }

        public static PenaltyResult nothingPending() {
            return new PenaltyResult(
                    PenaltyStatus.NOTHING_PENDING,
                    null,
                    0L,
                    0L,
                    null
            );
        }

        public static PenaltyResult diverged(
                PlayerDeathPenaltyState.PendingPenalty pending
        ) {
            return new PenaltyResult(
                    PenaltyStatus.STATE_DIVERGED,
                    pending.kind(),
                    pending.amount(),
                    pending.beforeValue(),
                    null
            );
        }
    }
}
