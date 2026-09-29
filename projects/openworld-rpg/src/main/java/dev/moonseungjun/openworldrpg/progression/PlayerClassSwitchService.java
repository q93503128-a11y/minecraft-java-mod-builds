package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative paid class-switch transaction boundary.
 *
 * <p>The first root-class choice is not routed here; R01 grants that choice for free. This service
 * owns later paid switching only. A pending transaction is persisted before the Gold debit and is
 * reconciled on reconnect, preventing both double charges and replay of an old debit receipt.</p>
 */
public final class PlayerClassSwitchService {
    private PlayerClassSwitchService() {
    }

    public static PlayerClassSwitchState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerClassSwitchAttachments.CLASS_SWITCH,
                PlayerClassSwitchState.initial()
        );
    }

    public static long currentSwitchCost(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        int level = PlayerProgressionService.state(player).combatLevel();
        return ProjectProgressionRules.classSwitchGoldCost(level);
    }

    public static SwitchResult requestSwitch(
            ServerPlayer player,
            RootClass targetClass
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetClass, "targetClass");

        var progression = PlayerProgressionService.state(player);
        RootClass currentClass = progression.activeClass().orElse(null);
        if (currentClass == null) {
            return SwitchResult.rejected(
                    SwitchStatus.FIRST_CLASS_REQUIRED,
                    targetClass,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }
        if (currentClass == targetClass) {
            return SwitchResult.rejected(
                    SwitchStatus.ALREADY_ACTIVE,
                    targetClass,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        PlayerClassSwitchState current = state(player);
        var pending = current.pendingSwitch().orElse(null);
        if (pending != null) {
            if (pending.targetClass() != targetClass) {
                return SwitchResult.rejected(
                        SwitchStatus.OTHER_SWITCH_PENDING,
                        targetClass,
                        pending.goldCost(),
                        PlayerCurrencyService.state(player).gold()
                );
            }
            return reconcilePending(player);
        }

        long cost = ProjectProgressionRules.classSwitchGoldCost(
                progression.combatLevel()
        );
        if (PlayerCurrencyService.state(player).gold() < cost) {
            return SwitchResult.rejected(
                    SwitchStatus.INSUFFICIENT_GOLD,
                    targetClass,
                    cost,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        replace(
                player,
                current.prepare(currentClass, targetClass, cost)
        );
        return reconcilePending(player);
    }

    /**
     * Resumes an in-flight paid switch after disconnect/crash.
     *
     * <p>If an external administrative path changed class before Gold was debited, the stale
     * pending switch is cancelled. Once the debit is durably recorded, the prepared transaction
     * owns completion and will finish its target rather than lose paid Gold.</p>
     */
    public static SwitchResult reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerClassSwitchState current = state(player);
        var pending = current.pendingSwitch().orElse(null);
        if (pending == null) {
            return SwitchResult.rejected(
                    SwitchStatus.NOTHING_PENDING,
                    null,
                    0L,
                    PlayerCurrencyService.state(player).gold()
            );
        }

        var progression = PlayerProgressionService.state(player);
        RootClass active = progression.activeClass().orElse(null);
        if (active == null) {
            return SwitchResult.rejected(
                    SwitchStatus.FIRST_CLASS_REQUIRED,
                    pending.targetClass(),
                    pending.goldCost(),
                    PlayerCurrencyService.state(player).gold()
            );
        }

        var currency = PlayerCurrencyService.state(player);
        boolean debitAlreadyApplied = currency.hasAppliedDebit(
                pending.debitTransactionId()
        );

        if (active == pending.targetClass()) {
            replace(
                    player,
                    current.commit(pending.transactionId())
            );
            return new SwitchResult(
                    SwitchStatus.SWITCHED,
                    pending.targetClass(),
                    pending.goldCost(),
                    PlayerCurrencyService.state(player).gold(),
                    pending.transactionId()
            );
        }

        if (active != pending.sourceClass() && !debitAlreadyApplied) {
            replace(
                    player,
                    current.cancel(pending.transactionId())
            );
            return SwitchResult.rejected(
                    SwitchStatus.SOURCE_CLASS_CHANGED,
                    pending.targetClass(),
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
                    state(player).cancel(pending.transactionId())
            );
            return SwitchResult.rejected(
                    SwitchStatus.INSUFFICIENT_GOLD,
                    pending.targetClass(),
                    pending.goldCost(),
                    debit.state().gold()
            );
        }

        PlayerProgressionService.selectClass(
                player,
                pending.targetClass()
        );
        replace(
                player,
                state(player).commit(pending.transactionId())
        );
        return new SwitchResult(
                SwitchStatus.SWITCHED,
                pending.targetClass(),
                pending.goldCost(),
                PlayerCurrencyService.state(player).gold(),
                pending.transactionId()
        );
    }

    private static PlayerClassSwitchState replace(
            ServerPlayer player,
            PlayerClassSwitchState next
    ) {
        PlayerClassSwitchState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(
                    PlayerClassSwitchAttachments.CLASS_SWITCH,
                    next
            );
        }
        return next;
    }

    public enum SwitchStatus {
        SWITCHED,
        FIRST_CLASS_REQUIRED,
        ALREADY_ACTIVE,
        INSUFFICIENT_GOLD,
        OTHER_SWITCH_PENDING,
        SOURCE_CLASS_CHANGED,
        NOTHING_PENDING
    }

    public record SwitchResult(
            SwitchStatus status,
            RootClass targetClass,
            long goldCost,
            long goldAfter,
            String transactionId
    ) {
        public SwitchResult {
            Objects.requireNonNull(status, "status");
            if (goldCost < 0L) {
                throw new IllegalArgumentException(
                        "Class-switch cost must be non-negative."
                );
            }
            if (status == SwitchStatus.SWITCHED) {
                Objects.requireNonNull(targetClass, "targetClass");
                if (goldCost <= 0L
                        || transactionId == null
                        || transactionId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Successful class switch requires target, cost and transaction id."
                    );
                }
            } else if (transactionId != null) {
                throw new IllegalArgumentException(
                        "Rejected class switch cannot expose transaction id."
                );
            }
        }

        public static SwitchResult rejected(
                SwitchStatus status,
                RootClass targetClass,
                long goldCost,
                long goldAfter
        ) {
            if (status == SwitchStatus.SWITCHED) {
                throw new IllegalArgumentException(
                        "Use successful result constructor for SWITCHED."
                );
            }
            return new SwitchResult(
                    status,
                    targetClass,
                    goldCost,
                    goldAfter,
                    null
            );
        }

        public boolean switched() {
            return status == SwitchStatus.SWITCHED;
        }
    }
}
