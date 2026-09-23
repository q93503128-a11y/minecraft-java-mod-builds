package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;
import java.util.Optional;

/** One persistent Recovery Belt slot. */
public enum RecoveryBeltSlot {
    EMPTY,
    HEALING_POTION,
    FOCUS_DRAUGHT,
    CLEANSING_TONIC;

    public static final Codec<RecoveryBeltSlot> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            RecoveryBeltSlot.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown recovery-belt slot value: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public static RecoveryBeltSlot loaded(RecoveryConsumable consumable) {
        return switch (consumable) {
            case HEALING_POTION -> HEALING_POTION;
            case FOCUS_DRAUGHT -> FOCUS_DRAUGHT;
            case CLEANSING_TONIC -> CLEANSING_TONIC;
        };
    }

    public Optional<RecoveryConsumable> consumable() {
        return switch (this) {
            case EMPTY -> Optional.empty();
            case HEALING_POTION -> Optional.of(RecoveryConsumable.HEALING_POTION);
            case FOCUS_DRAUGHT -> Optional.of(RecoveryConsumable.FOCUS_DRAUGHT);
            case CLEANSING_TONIC -> Optional.of(RecoveryConsumable.CLEANSING_TONIC);
        };
    }
}
