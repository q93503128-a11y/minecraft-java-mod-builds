package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum ProjectEquipmentSlot {
    MAIN_WEAPON,
    OFF_HAND,
    HEAD,
    CHEST,
    LEGS,
    GLOVES,
    BOOTS,
    NECKLACE,
    RING_1,
    RING_2,
    CHARM,
    RELIC;

    public static final Codec<ProjectEquipmentSlot> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            ProjectEquipmentSlot.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Openworld RPG equipment slot: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
