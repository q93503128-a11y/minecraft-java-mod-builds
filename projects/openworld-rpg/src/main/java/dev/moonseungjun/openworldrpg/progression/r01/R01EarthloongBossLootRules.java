package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Canon-locked Earthloong first-clear boss-loot pool identities. */
public final class R01EarthloongBossLootRules {
    public static final int SIGNATURE_DROP_PERCENT = 15;

    public static final List<NormalBase> NORMAL_BASE_POOL = List.of(
            NormalBase.QUARRY_MAUL,
            NormalBase.WATCH_BUCKLER,
            NormalBase.IRONBOUND_GUARD,
            NormalBase.INITIATE_STAFF,
            NormalBase.APPRENTICE_FOCUS,
            NormalBase.QUARRY_SEAL
    );

    public static final List<MythicBase> MYTHIC_POOL = List.of(
            MythicBase.ROOTQUAKE_MAUL,
            MythicBase.EARTHSCALE_WARD
    );

    private R01EarthloongBossLootRules() {
    }

    public static R01EarthloongBossLootPlanState.FirstClearPlan createPlan(
            int normalPoolIndex,
            int signatureRollPercent,
            int mythicPoolIndex
    ) {
        if (normalPoolIndex < 0 || normalPoolIndex >= NORMAL_BASE_POOL.size()) {
            throw new IllegalArgumentException("normalPoolIndex outside Earthloong pool.");
        }
        if (signatureRollPercent < 0 || signatureRollPercent >= 100) {
            throw new IllegalArgumentException("signatureRollPercent must be inside 0..99.");
        }
        if (mythicPoolIndex < 0 || mythicPoolIndex >= MYTHIC_POOL.size()) {
            throw new IllegalArgumentException("mythicPoolIndex outside Earthloong Mythic pool.");
        }

        MythicBase mythic = signatureRollPercent < SIGNATURE_DROP_PERCENT
                ? MYTHIC_POOL.get(mythicPoolIndex)
                : null;
        return new R01EarthloongBossLootPlanState.FirstClearPlan(
                NORMAL_BASE_POOL.get(normalPoolIndex),
                ProjectItemGrade.SUPERIOR,
                signatureRollPercent,
                java.util.Optional.ofNullable(mythic)
        );
    }

    public enum NormalBase {
        QUARRY_MAUL("openworld_rpg:quarry_maul"),
        WATCH_BUCKLER("openworld_rpg:watch_buckler"),
        IRONBOUND_GUARD("openworld_rpg:ironbound_guard"),
        INITIATE_STAFF("openworld_rpg:initiate_staff"),
        APPRENTICE_FOCUS("openworld_rpg:apprentice_focus"),
        QUARRY_SEAL("openworld_rpg:quarry_seal");

        public static final Codec<NormalBase> CODEC = enumCodec(
                NormalBase.class,
                "Earthloong normal base"
        );

        private final String baseId;

        NormalBase(String baseId) {
            this.baseId = baseId;
        }

        public String baseId() {
            return baseId;
        }
    }

    public enum MythicBase {
        ROOTQUAKE_MAUL(
                "openworld_rpg:rootquake_maul",
                "openworld_rpg:mythic/rootquake"
        ),
        EARTHSCALE_WARD(
                "openworld_rpg:earthscale_ward",
                "openworld_rpg:mythic/earthen_reprieve"
        );

        public static final Codec<MythicBase> CODEC = enumCodec(
                MythicBase.class,
                "Earthloong Mythic base"
        );

        private final String itemId;
        private final String uniquePowerId;

        MythicBase(String itemId, String uniquePowerId) {
            this.itemId = itemId;
            this.uniquePowerId = uniquePowerId;
        }

        public String itemId() {
            return itemId;
        }

        public String uniquePowerId() {
            return uniquePowerId;
        }
    }

    private static <E extends Enum<E>> Codec<E> enumCodec(
            Class<E> type,
            String label
    ) {
        Objects.requireNonNull(type, "type");
        return Codec.STRING.comapFlatMap(
                value -> {
                    try {
                        return DataResult.success(
                                Enum.valueOf(
                                        type,
                                        value.trim().toUpperCase(Locale.ROOT)
                                )
                        );
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(
                                () -> "Unknown " + label + ": " + value
                        );
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );
    }
}
