package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.Objects;

/**
 * Server-authoritative aggregate of combat-relevant equipped-item state.
 *
 * <p>This is not a substitute inventory. The future item/equipment subsystem owns individual slots
 * and affixes, then publishes their validated aggregate here. Keeping the aggregate explicit avoids
 * reading vanilla attack damage or donor spell-power values as project authority.</p>
 */
public record EquipmentCombatState(
        ProjectWeaponFamily weaponFamily,
        int weaponItemLevel,
        EffectiveAttributes flatAttributeBonuses,
        double physicalPowerBonus,
        double magicPowerBonus,
        double weaponFamilyPowerBonus,
        double poiseOutputBonus,
        double supplementalMagicWeaponPower
) {
    public EquipmentCombatState {
        Objects.requireNonNull(weaponFamily, "weaponFamily");
        Objects.requireNonNull(flatAttributeBonuses, "flatAttributeBonuses");
        if (weaponItemLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                || weaponItemLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
            throw new IllegalArgumentException("weaponItemLevel must be inside [1, 80].");
        }
        requireBonus("physicalPowerBonus", physicalPowerBonus);
        requireBonus("magicPowerBonus", magicPowerBonus);
        requireBonus("weaponFamilyPowerBonus", weaponFamilyPowerBonus);
        requireBonus("poiseOutputBonus", poiseOutputBonus);
        if (!Double.isFinite(supplementalMagicWeaponPower) || supplementalMagicWeaponPower < 0.0) {
            throw new IllegalArgumentException(
                    "supplementalMagicWeaponPower must be finite and non-negative."
            );
        }
    }

    public static EquipmentCombatState weaponOnly(
            ProjectWeaponFamily weaponFamily,
            int weaponItemLevel
    ) {
        return new EquipmentCombatState(
                weaponFamily,
                weaponItemLevel,
                new EffectiveAttributes(0, 0, 0, 0, 0, 0),
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    private static void requireBonus(String name, double value) {
        if (!Double.isFinite(value) || 1.0 + value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and keep its multiplier positive.");
        }
    }
}
