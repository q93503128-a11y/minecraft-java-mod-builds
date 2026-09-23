package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Internal-only canonical R01 main-stage order from R01_VERTICAL_SLICE.md. */
public enum R01MainStage {
    ARRIVAL_ROAD,
    ALDERFORD_REACHED,
    FIRST_CLASS_SELECTED,
    QUARRY_ROAD_ACTIVE,
    QUARRY_ROAD_COMPLETE,
    QUARRY_ENTRANCE_DISCOVERED,
    QUARRY_DUNGEON_ACTIVE,
    EARTHLOONG_CLEARED,
    POST_QUARRY_BRIEFING_PENDING,
    ACT1_LEADS_OPEN;

    public static final Codec<R01MainStage> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            R01MainStage.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown R01 main stage: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public boolean isAtLeast(R01MainStage other) {
        return ordinal() >= other.ordinal();
    }

    public static R01MainStage furthest(R01MainStage left, R01MainStage right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
