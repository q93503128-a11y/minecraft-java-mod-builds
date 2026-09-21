package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum RootClass {
    WARRIOR,
    HUNTER,
    CLERIC,
    MAGE,
    GUARDIAN;

    public static final Codec<RootClass> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(RootClass.valueOf(value.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Openworld RPG root class: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
