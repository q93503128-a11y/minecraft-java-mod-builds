package dev.moonseungjun.openworldrpg.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Canonical equipment quality identity persisted with project inventory equipment. */
public enum ProjectItemGrade {
    STANDARD,
    REFINED,
    SUPERIOR,
    EXALTED,
    MYTHIC;

    public static final Codec<ProjectItemGrade> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            ProjectItemGrade.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown project item grade: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
