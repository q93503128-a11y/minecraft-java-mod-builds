package dev.moonseungjun.openworldrpg.combat.authority;

/**
 * Server-owned decision point for melee damage that arrives through external presentation backends.
 *
 * <p>The M0 bridge deliberately treats Better Combat's already server-rebuilt vanilla melee amount as a proposal.
 * R01 stat/defense/poise/status resolution will replace the neutral amount policy behind this class without moving
 * the authority seam back into the dependency.</p>
 */
public final class CombatDamageAuthority {
    private CombatDamageAuthority() {
    }

    public static DamageDecision authorizeBetterCombatMelee(float proposedDamage, int comboCount) {
        if (comboCount < 0 || !Float.isFinite(proposedDamage) || proposedDamage <= 0.0F) {
            return DamageDecision.rejected();
        }

        return DamageDecision.accepted(proposedDamage);
    }

    public record DamageDecision(boolean accepted, float amount) {
        public static DamageDecision accepted(float amount) {
            return new DamageDecision(true, amount);
        }

        public static DamageDecision rejected() {
            return new DamageDecision(false, 0.0F);
        }
    }
}
