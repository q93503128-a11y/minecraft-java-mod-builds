package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/**
 * Combat-source affix identities currently consumed by the M0 build publisher.
 *
 * <p>Defense/resource/utility affixes remain valid equipment data but are not added here until
 * their corresponding project runtime owns those effects.</p>
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
    WEAPON_FAMILY_POWER,
    POISE_OUTPUT;

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
