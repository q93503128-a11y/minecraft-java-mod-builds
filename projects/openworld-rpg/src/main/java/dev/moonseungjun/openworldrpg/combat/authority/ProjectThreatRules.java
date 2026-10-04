package dev.moonseungjun.openworldrpg.combat.authority;

/**
 * Canonical threat-generation math from COMBAT_BALANCE.
 *
 * <p>This class owns only the numeric conversion from a validated combat result into generated
 * threat. Encounter runtimes remain responsible for proving engagement, actor identity and target
 * eligibility before storing the result.</p>
 */
public final class ProjectThreatRules {
    private static final double DAMAGE_SCALE = 100.0;
    private static final double HEAL_SCALE = 35.0;
    private static final double BARRIER_SCALE = 25.0;
    private static final double GUARD_SCALE = 20.0;
    private static final double PERFECT_GUARD_FLAT = 4.0;

    private ProjectThreatRules() {
    }

    public static double damageThreat(
            double postMitigationDamage,
            double targetMaxHp
    ) {
        return proportionalThreat(
                postMitigationDamage,
                targetMaxHp,
                DAMAGE_SCALE,
                "postMitigationDamage"
        );
    }

    public static double effectiveHealingThreat(
            double effectiveHealing,
            double healedTargetMaxHp
    ) {
        return proportionalThreat(
                effectiveHealing,
                healedTargetMaxHp,
                HEAL_SCALE,
                "effectiveHealing"
        );
    }

    public static double effectiveBarrierThreat(
            double effectiveBarrierGranted,
            double recipientMaxHp
    ) {
        return proportionalThreat(
                effectiveBarrierGranted,
                recipientMaxHp,
                BARRIER_SCALE,
                "effectiveBarrierGranted"
        );
    }

    public static double guardThreat(
            double preventedHpDamage,
            double guardianMaxHp,
            boolean perfectGuard
    ) {
        double result = proportionalThreat(
                preventedHpDamage,
                guardianMaxHp,
                GUARD_SCALE,
                "preventedHpDamage"
        );
        return perfectGuard
                ? result + PERFECT_GUARD_FLAT
                : result;
    }

    private static double proportionalThreat(
            double effectiveAmount,
            double maxHp,
            double scale,
            String amountName
    ) {
        if (!Double.isFinite(effectiveAmount)
                || effectiveAmount < 0.0) {
            throw new IllegalArgumentException(
                    amountName + " must be finite and non-negative."
            );
        }
        if (!Double.isFinite(maxHp)
                || maxHp <= 0.0) {
            throw new IllegalArgumentException(
                    "Threat reference MaxHP must be finite and positive."
            );
        }
        return scale * effectiveAmount / maxHp;
    }
}
