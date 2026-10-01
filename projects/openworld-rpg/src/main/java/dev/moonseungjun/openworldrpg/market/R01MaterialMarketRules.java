package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.profession.R01CraftingRecipe;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

/** Canon-locked R01 Material Pouch sell values and confirmation rules. */
public final class R01MaterialMarketRules {
    public static final String TOUGH_HIDE = "openworld_rpg:tough_hide";
    public static final String REGALHART_ANTLER =
            "openworld_rpg:regalhart_antler";
    public static final String EARTHLOONG_SCALE =
            "openworld_rpg:earthloong_scale";

    private static final Map<String, Long> SELL_VALUES = Map.ofEntries(
            Map.entry(R01GatheringRules.IRON_ORE, 5L),
            Map.entry(R01GatheringRules.HARDWOOD, 4L),
            Map.entry(R01GatheringRules.HEALING_HERB, 5L),
            Map.entry(R01GatheringRules.VERDANT_CRYSTAL, 24L),
            Map.entry(R01CraftingRecipe.LOUXIA_MEAT, 3L),
            Map.entry(R01CraftingRecipe.LOUXIA_GLOW, 8L),
            Map.entry(TOUGH_HIDE, 6L),
            Map.entry(REGALHART_ANTLER, 45L),
            Map.entry(EARTHLOONG_SCALE, 45L)
    );

    private static final Set<String> SIGNATURE_MATERIALS = Set.of(
            REGALHART_ANTLER,
            EARTHLOONG_SCALE
    );

    private R01MaterialMarketRules() {
    }

    public static OptionalLong sellValue(String materialId) {
        Objects.requireNonNull(materialId, "materialId");
        Long value = SELL_VALUES.get(materialId);
        return value == null
                ? OptionalLong.empty()
                : OptionalLong.of(value);
    }

    public static Map<String, Long> sellValues() {
        return SELL_VALUES;
    }

    public static boolean requiresSignatureConfirmation(String materialId) {
        Objects.requireNonNull(materialId, "materialId");
        return SIGNATURE_MATERIALS.contains(materialId);
    }

    public static boolean bulkEligible(String materialId) {
        return sellValue(materialId).isPresent()
                && !requiresSignatureConfirmation(materialId);
    }
}
