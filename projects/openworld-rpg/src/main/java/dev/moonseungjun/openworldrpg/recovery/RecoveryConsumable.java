package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Canonical R01 quick-recovery consumable identities. */
public enum RecoveryConsumable {
    HEALING_POTION,
    FOCUS_DRAUGHT,
    CLEANSING_TONIC;

    public static final Codec<RecoveryConsumable> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            RecoveryConsumable.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown recovery consumable: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
