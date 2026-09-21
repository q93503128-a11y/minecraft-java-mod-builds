package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.Objects;

/**
 * Immutable current player combat build from canonical player level, one root-class allocation,
 * and validated equipped-item aggregate.
 */
public record PlayerCombatBuildState(
        int combatLevel,
        RootClass activeClass,
        AttributeAllocation allocation,
        EquipmentCombatState equipment
) {
    public PlayerCombatBuildState {
        if (combatLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                || combatLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
            throw new IllegalArgumentException("combatLevel must be inside [1, 80].");
        }
        Objects.requireNonNull(activeClass, "activeClass");
        Objects.requireNonNull(allocation, "allocation");
        Objects.requireNonNull(equipment, "equipment");
        if (allocation.spentPoints() > combatLevel - 1) {
            throw new IllegalArgumentException(
                    "Spent attribute points exceed the canonical earned pool for Lv " + combatLevel + "."
            );
        }
    }

    public EffectiveAttributes effectiveAttributes() {
        EffectiveAttributes gear = equipment.flatAttributeBonuses();
        return new EffectiveAttributes(
                allocation.value(CombatAttribute.VIT) + gear.vit(),
                allocation.value(CombatAttribute.END) + gear.end(),
                allocation.value(CombatAttribute.STR) + gear.str(),
                allocation.value(CombatAttribute.DEX) + gear.dex(),
                allocation.value(CombatAttribute.INT) + gear.intel(),
                allocation.value(CombatAttribute.WIL) + gear.wil()
        );
    }

    public ProjectImpactTransaction.DamageSourceSnapshot damageSource(
            ProjectImpactTransaction.DamageSchool school
    ) {
        ProjectWeaponFamily family = equipment.weaponFamily();
        double weaponPower = Math.round(
                22.0
                        * ProjectCombatRules.gearScale(equipment.weaponItemLevel())
                        * family.powerFactor()
        );
        double weightedStat = family.weightedOffensiveStat(effectiveAttributes());
        double schoolPower = switch (school) {
            case PHYSICAL -> equipment.physicalPowerBonus();
            case MAGIC -> equipment.magicPowerBonus();
        };
        double additivePower = schoolPower + equipment.weaponFamilyPowerBonus();
        double poiseOutput = family.poiseMultiplier() * (1.0 + equipment.poiseOutputBonus());

        return new ProjectImpactTransaction.DamageSourceSnapshot(
                combatLevel,
                weaponPower,
                weightedStat,
                additivePower,
                poiseOutput
        );
    }
}
