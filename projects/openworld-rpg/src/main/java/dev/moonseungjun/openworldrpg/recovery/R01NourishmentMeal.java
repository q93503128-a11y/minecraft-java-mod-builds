package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.Locale;

public enum R01NourishmentMeal {
    HERBED_LOUXIA_ROAST(
            "openworld_rpg:herbed_louxia_roast",
            Effect.MAX_HP,
            0.06
    ),
    TRAIL_SKEWERS(
            "openworld_rpg:trail_skewers",
            Effect.STAMINA_RECOVERY,
            0.10
    ),
    GLOW_BROTH(
            "openworld_rpg:glow_broth",
            Effect.MANA_RECOVERY,
            0.10
    );

    public static final Codec<R01NourishmentMeal> CODEC =
            Codec.STRING.comapFlatMap(
                    value -> Arrays.stream(values())
                            .filter(meal -> meal.name().equalsIgnoreCase(value))
                            .findFirst()
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(
                                    () -> "Unknown nourishment meal: " + value
                            )),
                    value -> value.name().toLowerCase(Locale.ROOT)
            );

    private final String itemId;
    private final Effect effect;
    private final double baseMagnitude;

    R01NourishmentMeal(
            String itemId,
            Effect effect,
            double baseMagnitude
    ) {
        this.itemId = itemId;
        this.effect = effect;
        this.baseMagnitude = baseMagnitude;
    }

    public String itemId() {
        return itemId;
    }

    public Effect effect() {
        return effect;
    }

    public double baseMagnitude() {
        return baseMagnitude;
    }

    public enum Effect {
        MAX_HP,
        STAMINA_RECOVERY,
        MANA_RECOVERY
    }
}
