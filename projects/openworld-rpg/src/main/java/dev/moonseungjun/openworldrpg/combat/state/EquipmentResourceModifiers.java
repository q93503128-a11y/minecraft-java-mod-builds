package dev.moonseungjun.openworldrpg.combat.state;

/** Canonical equipped-gear modifiers consumed by project HP/Mana/Stamina authority. */
public record EquipmentResourceModifiers(
        double maxHealthBonus,
        double maxManaBonus,
        double maxStaminaBonus,
        double manaRecoveryBonus,
        double staminaRecoveryBonus,
        double manaCostReduction
) {
    public static final double MAX_MANA_COST_REDUCTION = 0.20;

    public EquipmentResourceModifiers {
        requireNonNegative("maxHealthBonus", maxHealthBonus);
        requireNonNegative("maxManaBonus", maxManaBonus);
        requireNonNegative("maxStaminaBonus", maxStaminaBonus);
        requireNonNegative("manaRecoveryBonus", manaRecoveryBonus);
        requireNonNegative("staminaRecoveryBonus", staminaRecoveryBonus);
        requireNonNegative("manaCostReduction", manaCostReduction);
        if (manaCostReduction > MAX_MANA_COST_REDUCTION) {
            throw new IllegalArgumentException(
                    "Mana-cost reduction exceeds canonical +20% gear cap."
            );
        }
    }

    public static EquipmentResourceModifiers none() {
        return new EquipmentResourceModifiers(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    private static void requireNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative."
            );
        }
    }
}
