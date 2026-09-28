package dev.moonseungjun.openworldrpg.equipment;

import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffix;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import java.util.Objects;

/** Maps only actually implemented ordinary-affix payloads into combat authority. */
public final class OrdinaryEquipmentRuntimeAffixAdapter {
    private OrdinaryEquipmentRuntimeAffixAdapter() {
    }

    public static EquipmentCombatAffix adapt(
            OrdinaryEquipmentAffixRoller.GeneratedAffix affix
    ) {
        Objects.requireNonNull(affix, "affix");
        String payload = affix.runtimePayload();
        return switch (payload) {
            case "openworld_rpg:runtime_affix/vit" ->
                    EquipmentCombatAffix.flat(
                            EquipmentCombatAffixKind.VIT,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/end" ->
                    EquipmentCombatAffix.flat(
                            EquipmentCombatAffixKind.END,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/str" ->
                    EquipmentCombatAffix.flat(
                            EquipmentCombatAffixKind.STR,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/dex" ->
                    EquipmentCombatAffix.flat(
                            EquipmentCombatAffixKind.DEX,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/int" ->
                    EquipmentCombatAffix.flat(
                            EquipmentCombatAffixKind.INT,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/wil" ->
                    EquipmentCombatAffix.flat(
                            EquipmentCombatAffixKind.WIL,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/physical_power" ->
                    percentage(
                            EquipmentCombatAffixKind.PHYSICAL_POWER,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/magic_power" ->
                    percentage(
                            EquipmentCombatAffixKind.MAGIC_POWER,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/critical_chance" ->
                    percentage(
                            EquipmentCombatAffixKind.CRITICAL_CHANCE,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/critical_damage" ->
                    percentage(
                            EquipmentCombatAffixKind.CRITICAL_DAMAGE,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/defense" ->
                    percentage(
                            EquipmentCombatAffixKind.DEFENSE,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/magic_resistance" ->
                    percentage(
                            EquipmentCombatAffixKind.MAGIC_RESISTANCE,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/max_hp" ->
                    percentage(
                            EquipmentCombatAffixKind.MAX_HP,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/max_mana" ->
                    percentage(
                            EquipmentCombatAffixKind.MAX_MANA,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/max_stamina" ->
                    percentage(
                            EquipmentCombatAffixKind.MAX_STAMINA,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/mana_recovery" ->
                    percentage(
                            EquipmentCombatAffixKind.MANA_RECOVERY,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/stamina_recovery" ->
                    percentage(
                            EquipmentCombatAffixKind.STAMINA_RECOVERY,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/skill_mana_cost_reduction" ->
                    percentage(
                            EquipmentCombatAffixKind.MANA_COST_REDUCTION,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/guard_strength" ->
                    percentage(
                            EquipmentCombatAffixKind.GUARD_STRENGTH,
                            affix.value()
                    );
            case "openworld_rpg:runtime_affix/poise_stagger_resistance" ->
                    percentage(
                            EquipmentCombatAffixKind.POISE_STAGGER_RESISTANCE,
                            affix.value()
                    );
            default -> throw new IllegalStateException(
                    "Ordinary affix has no live runtime adapter: " + payload
            );
        };
    }

    private static EquipmentCombatAffix percentage(
            EquipmentCombatAffixKind kind,
            double percentagePoints
    ) {
        return EquipmentCombatAffix.flat(
                kind,
                percentagePoints / 100.0
        );
    }
}
