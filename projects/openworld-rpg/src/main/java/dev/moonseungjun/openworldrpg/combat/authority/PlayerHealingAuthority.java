package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentLoadoutState;

/** Server-owned numeric order for incoming project healing. */
public final class PlayerHealingAuthority {
    private PlayerHealingAuthority() {
    }

    public static double healingReference(
            int casterLevel,
            double effectiveWill,
            double effectiveIntelligence
    ) {
        if (!Double.isFinite(effectiveWill) || effectiveWill < 0.0
                || !Double.isFinite(effectiveIntelligence)
                || effectiveIntelligence < 0.0) {
            throw new IllegalArgumentException(
                    "Healing stats must be finite and non-negative."
            );
        }
        double weightedStat = 0.80 * effectiveWill
                + 0.20 * effectiveIntelligence;
        return ProjectCombatRules.baseHp(casterLevel)
                * ProjectCombatRules.attributeDamageMultiplier(weightedStat);
    }

    /**
     * Canonical project skill-healing order:
     * HealingReference × HealCoefficient × Healing Done × target Healing Received.
     */
    public static double skillHealingAmount(
            double healingReference,
            double healCoefficient,
            double healingDoneBonus,
            double healingReceivedBonus
    ) {
        if (!Double.isFinite(healingReference) || healingReference < 0.0) {
            throw new IllegalArgumentException(
                    "healingReference must be finite and non-negative."
            );
        }
        if (!Double.isFinite(healCoefficient) || healCoefficient <= 0.0) {
            throw new IllegalArgumentException(
                    "healCoefficient must be finite and positive."
            );
        }
        if (!Double.isFinite(healingDoneBonus)
                || healingDoneBonus < 0.0
                || healingDoneBonus
                        > PlayerEquipmentLoadoutState.HEALING_DONE_GEAR_CAP) {
            throw new IllegalArgumentException(
                    "healingDoneBonus must stay inside canonical [0, 0.40]."
            );
        }
        double outgoing = healingReference
                * healCoefficient
                * (1.0 + healingDoneBonus);
        return receivedHealingAmount(outgoing, healingReceivedBonus);
    }

    /**
     * Applies Healing Received only after the healing source has authored its base amount.
     *
     * <p>For example, the R01 Healing Potion first resolves 35% MaxHP, then this multiplier is
     * applied, and Minecraft/project HP clamping happens at the final application boundary.</p>
     */
    public static double receivedHealingAmount(
            double authoredBaseHealing,
            double healingReceivedBonus
    ) {
        if (!Double.isFinite(authoredBaseHealing) || authoredBaseHealing < 0.0) {
            throw new IllegalArgumentException(
                    "authoredBaseHealing must be finite and non-negative."
            );
        }
        if (!Double.isFinite(healingReceivedBonus)
                || healingReceivedBonus < 0.0
                || healingReceivedBonus
                        > PlayerEquipmentLoadoutState.HEALING_RECEIVED_GEAR_CAP) {
            throw new IllegalArgumentException(
                    "healingReceivedBonus must stay inside canonical [0, 0.40]."
            );
        }
        return authoredBaseHealing * (1.0 + healingReceivedBonus);
    }
}
