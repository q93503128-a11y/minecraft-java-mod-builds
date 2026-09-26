package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Bounded canonical participation units for one R01 Quarry dungeon run. */
public enum R01QuarryRunContribution {
    UPPER_GALLERY_COMBAT,
    COLLAPSED_HOIST_COMBAT,
    LIFT_SHORTCUT,
    ROOT_BREACHED_COMBAT,
    RELAY_EVIDENCE,
    EARTHLOONG_COMBAT;

    public static final Codec<R01QuarryRunContribution> CODEC =
            Codec.STRING.comapFlatMap(
                    value -> {
                        try {
                            return DataResult.success(
                                    R01QuarryRunContribution.valueOf(
                                            value.trim().toUpperCase(Locale.ROOT)
                                    )
                            );
                        } catch (IllegalArgumentException exception) {
                            return DataResult.error(
                                    () -> "Unknown R01 Quarry contribution: " + value
                            );
                        }
                    },
                    value -> value.name().toLowerCase(Locale.ROOT)
            );
}
