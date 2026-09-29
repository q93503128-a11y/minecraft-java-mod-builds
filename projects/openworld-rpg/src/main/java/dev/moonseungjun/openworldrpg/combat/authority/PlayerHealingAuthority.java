package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentLoadoutState;

/** Server-owned numeric order for incoming project healing. */
public final class PlayerHealingAuthority {
    private PlayerHealingAuthority() {
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
