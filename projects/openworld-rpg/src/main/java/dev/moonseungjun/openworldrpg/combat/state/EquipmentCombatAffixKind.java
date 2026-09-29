package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/**
 * Combat-source affix identities currently consumed by project-owned runtime publishers.
 */
public enum EquipmentCombatAffixKind {
    VIT,
    END,
    STR,
    DEX,
    INT,
    WIL,
    PHYSICAL_POWER,
    MAGIC_POWER,
    CRITICAL_CHANCE,
    CRITICAL_DAMAGE,
    ATTACK_SPEED,
    MOVEMENT_SPEED,
    MAX_HP,
    MAX_MANA,
    MAX_STAMINA,
    MANA_RECOVERY,
    STAMINA_RECOVERY,
    MANA_COST_REDUCTION,
    DODGE_SPRINT_STAMINA_COST_REDUCTION,
    WEAPON_FAMILY_POWER,
    POISE_OUTPUT,
    DEFENSE,
    MAGIC_RESISTANCE,
    GUARD_STRENGTH,
    POISE_STAGGER_RESISTANCE;

    public static final Codec<EquipmentCombatAffixKind> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            EquipmentCombatAffixKind.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Openworld RPG combat affix: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
