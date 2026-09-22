package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.Objects;

/**
 * Server-owned active-defense timing state for one player.
 *
 * <p>Input/animation adapters request actions here. They may predict presentation locally, but only
 * this state accepts dodge i-frames, perfect-guard windows, Stamina costs and guard breaks.</p>
 */
public final class PlayerDefenseRuntimeState {
    public static final double DODGE_STAMINA_COST = 30.0;
    public static final long DODGE_REGEN_DELAY_TICKS = 12L;
    public static final long DODGE_INVULNERABILITY_TICKS = 6L;
    public static final long DODGE_ACTION_TICKS = 9L;
    public static final long DODGE_REENTRY_TICKS = 11L;

    public static final long PERFECT_GUARD_WINDOW_TICKS = 4L;
    public static final long PERFECT_GUARD_REENTRY_TICKS = 10L;
    public static final long PERFECT_GUARD_REGEN_DELAY_TICKS = 7L;
    public static final long BLOCKED_IMPACT_REGEN_DELAY_TICKS = 15L;
    public static final long GUARD_BREAK_REGEN_DELAY_TICKS = 22L;
    public static final long GUARD_BREAK_REACTION_TICKS = 17L;
    public static final long GUARD_BREAK_DEFENSE_LOCK_TICKS = 9L;

    private long dodgeInvulnerableUntilTick = Long.MIN_VALUE / 4;
    private long dodgeActionUntilTick = Long.MIN_VALUE / 4;
    private long nextDodgeAllowedTick = Long.MIN_VALUE / 4;

    private boolean guardHeld;
    private long perfectGuardUntilTick = Long.MIN_VALUE / 4;
    private long nextPerfectGuardAllowedTick = Long.MIN_VALUE / 4;
    private long guardBreakReactionUntilTick = Long.MIN_VALUE / 4;
    private long guardRestartAllowedTick = Long.MIN_VALUE / 4;

    public boolean tryBeginDodge(
            PlayerCombatState resources,
            long nowTick,
            boolean rootedHardStaggeredOrDowned
    ) {
        Objects.requireNonNull(resources, "resources");
        if (rootedHardStaggeredOrDowned
                || nowTick < nextDodgeAllowedTick
                || nowTick < guardRestartAllowedTick) {
            return false;
        }
        if (!resources.spendStamina(
                DODGE_STAMINA_COST,
                DODGE_REGEN_DELAY_TICKS,
                nowTick
        )) {
            return false;
        }

        dodgeInvulnerableUntilTick = nowTick + DODGE_INVULNERABILITY_TICKS;
        dodgeActionUntilTick = nowTick + DODGE_ACTION_TICKS;
        nextDodgeAllowedTick = nowTick + DODGE_REENTRY_TICKS;
        return true;
    }

    public GuardPressResult pressGuard(long nowTick) {
        if (nowTick < guardRestartAllowedTick) {
            return new GuardPressResult(false, false);
        }
        if (guardHeld) {
            return new GuardPressResult(true, false);
        }

        guardHeld = true;
        if (nowTick >= nextPerfectGuardAllowedTick) {
            perfectGuardUntilTick = nowTick + PERFECT_GUARD_WINDOW_TICKS;
            nextPerfectGuardAllowedTick = nowTick + PERFECT_GUARD_REENTRY_TICKS;
            return new GuardPressResult(true, true);
        }

        perfectGuardUntilTick = Long.MIN_VALUE / 4;
        return new GuardPressResult(true, false);
    }

    public void releaseGuard() {
        guardHeld = false;
        perfectGuardUntilTick = Long.MIN_VALUE / 4;
    }

    public boolean isDodgeInvulnerable(long nowTick) {
        return nowTick < dodgeInvulnerableUntilTick;
    }

    public boolean isDodgeActionActive(long nowTick) {
        return nowTick < dodgeActionUntilTick;
    }

    public boolean isGuardHeld() {
        return guardHeld;
    }

    public boolean isPerfectGuardWindow(long nowTick) {
        return guardHeld && nowTick < perfectGuardUntilTick;
    }

    public boolean isGuardBreakReaction(long nowTick) {
        return nowTick < guardBreakReactionUntilTick;
    }

    public long nextDodgeAllowedTick() {
        return nextDodgeAllowedTick;
    }

    public long nextPerfectGuardAllowedTick() {
        return nextPerfectGuardAllowedTick;
    }

    public IncomingDefenseResult resolveIncoming(
            PlayerCombatState resources,
            PlayerDefenseAuthority.DefenseSnapshot defense,
            PlayerDefenseAuthority.IncomingHit hit,
            long nowTick
    ) {
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(defense, "defense");
        Objects.requireNonNull(hit, "hit");

        double mitigated = PlayerDefenseAuthority.mitigatedDamageBeforeActiveDefense(
                hit,
                defense
        );
        if (hit.rawDamage() <= 0.0) {
            return IncomingDefenseResult.unopposed(0.0);
        }

        if (hit.dodgeable() && isDodgeInvulnerable(nowTick)) {
            return new IncomingDefenseResult(
                    mitigated,
                    0.0,
                    0.0,
                    true,
                    false,
                    false,
                    false
            );
        }

        var guardType = defense.guardType();
        if (!guardHeld
                || guardType.isEmpty()
                || (!hit.guardable() && !hit.perfectGuardable())) {
            return IncomingDefenseResult.unopposed(
                    ProjectCombatRules.roundFinal(mitigated)
            );
        }

        double normalGuardCost = PlayerDefenseAuthority.guardStaminaCost(
                hit.guardPressure(),
                defense.guardRating(),
                resources.endurance(),
                hit.attackerLevel()
        );

        /*
         * perfect_guardable is intentionally independent from ordinary guardable. Canon contains
         * committed charges such as Iron Rush / Crown Charge / Quarry Rush that can be just-guarded
         * without granting a safe hold-block answer.
         */
        if (hit.perfectGuardable() && isPerfectGuardWindow(nowTick)) {
            double perfectCost = Math.max(2.0, normalGuardCost * 0.25);
            if (resources.spendStamina(
                    perfectCost,
                    PERFECT_GUARD_REGEN_DELAY_TICKS,
                    nowTick
            )) {
                return new IncomingDefenseResult(
                        mitigated,
                        0.0,
                        perfectCost,
                        false,
                        true,
                        true,
                        false
                );
            }
        }

        if (!hit.guardable()) {
            return IncomingDefenseResult.unopposed(
                    ProjectCombatRules.roundFinal(mitigated)
            );
        }

        double absorption = PlayerDefenseAuthority.guardAbsorption(
                guardType.orElseThrow(),
                hit.school()
        );

        if (resources.spendStamina(
                normalGuardCost,
                BLOCKED_IMPACT_REGEN_DELAY_TICKS,
                nowTick
        )) {
            double finalDamage = ProjectCombatRules.roundFinal(
                    mitigated * (1.0 - absorption)
            );
            return new IncomingDefenseResult(
                    mitigated,
                    finalDamage,
                    normalGuardCost,
                    false,
                    true,
                    false,
                    false
            );
        }

        double remaining = resources.stamina(nowTick);
        resources.spendStamina(
                remaining,
                GUARD_BREAK_REGEN_DELAY_TICKS,
                nowTick
        );
        guardHeld = false;
        perfectGuardUntilTick = Long.MIN_VALUE / 4;
        guardBreakReactionUntilTick = nowTick + GUARD_BREAK_REACTION_TICKS;
        guardRestartAllowedTick = nowTick + GUARD_BREAK_DEFENSE_LOCK_TICKS;

        double finalDamage = ProjectCombatRules.roundFinal(
                mitigated * (1.0 - absorption * 0.5)
        );
        return new IncomingDefenseResult(
                mitigated,
                finalDamage,
                remaining,
                false,
                true,
                false,
                true
        );
    }

    public record GuardPressResult(
            boolean accepted,
            boolean perfectWindowStarted
    ) {
    }

    public record IncomingDefenseResult(
            double mitigatedBeforeActiveDefense,
            double finalDamage,
            double staminaSpent,
            boolean dodged,
            boolean guarded,
            boolean perfectGuarded,
            boolean guardBroken
    ) {
        private static IncomingDefenseResult unopposed(double finalDamage) {
            return new IncomingDefenseResult(
                    finalDamage,
                    finalDamage,
                    0.0,
                    false,
                    false,
                    false,
                    false
            );
        }
    }
}
