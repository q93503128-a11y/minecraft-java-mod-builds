package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Canonical R01 quick-recovery consumable identities. */
public enum RecoveryConsumable {
    HEALING_POTION("openworld_rpg:healing_potion"),
    FOCUS_DRAUGHT("openworld_rpg:focus_draught"),
    CLEANSING_TONIC("openworld_rpg:cleansing_tonic");

    private final String itemId;

    RecoveryConsumable(String itemId) {
        this.itemId = itemId;
    }

    public String itemId() {
        return itemId;
    }

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
