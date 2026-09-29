package dev.moonseungjun.openworldrpg.combat.authority;

/** Canonical numeric contract for project-owned player barriers. */
public final class PlayerBarrierAuthority {
    public static final int DEFAULT_BARRIER_DURATION_TICKS = 120;
    public static final double DIFFERENT_SOURCE_STACK_CAP_MAX_HP_FRACTION = 0.40;

    private PlayerBarrierAuthority() {
    }

    public static double barrierReference(
            int casterLevel,
            double effectiveWill,
            double effectiveEndurance
    ) {
        requireFiniteNonNegative("effectiveWill", effectiveWill);
        requireFiniteNonNegative("effectiveEndurance", effectiveEndurance);

        double weightedStat = 0.65 * effectiveWill
                + 0.35 * effectiveEndurance;
        return ProjectCombatRules.baseHp(casterLevel)
                * ProjectCombatRules.attributeDamageMultiplier(weightedStat);
    }

    public static double skillBarrierAmount(
            double barrierReference,
            double barrierCoefficient,
            double applicableOutputBonus
    ) {
        requireFiniteNonNegative("barrierReference", barrierReference);
        if (!Double.isFinite(barrierCoefficient)
                || barrierCoefficient <= 0.0) {
            throw new IllegalArgumentException(
                    "barrierCoefficient must be finite and positive."
            );
        }
        requireFiniteNonNegative(
                "applicableOutputBonus",
                applicableOutputBonus
        );
        return barrierReference
                * barrierCoefficient
                * (1.0 + applicableOutputBonus);
    }

    public static double totalBarrierCap(double recipientMaxHp) {
        if (!Double.isFinite(recipientMaxHp) || recipientMaxHp <= 0.0) {
            throw new IllegalArgumentException(
                    "recipientMaxHp must be finite and positive."
            );
        }
        return recipientMaxHp
                * DIFFERENT_SOURCE_STACK_CAP_MAX_HP_FRACTION;
    }

    private static void requireFiniteNonNegative(
            String name,
            double value
    ) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative."
            );
        }
    }
}
