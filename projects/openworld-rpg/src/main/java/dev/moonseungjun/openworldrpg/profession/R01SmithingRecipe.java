package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import dev.moonseungjun.openworldrpg.market.R01NessaMarketRules;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Canon-locked ordinary R01 Daren Holt forge recipes. */
public enum R01SmithingRecipe {
    HEARTLAND_SWORD_REFINED(
            "heartland_sword_refined",
            R01NessaMarketRules.HEARTLAND_ARMING_SWORD,
            ProjectItemGrade.REFINED,
            4,
            Map.of(
                    R01GatheringRules.IRON_ORE, 6,
                    R01GatheringRules.HARDWOOD, 2
            ),
            60L,
            "openworld_rpg:affix/str"
    ),
    QUARRY_MAUL_REFINED(
            "quarry_maul_refined",
            R01NessaMarketRules.QUARRY_MAUL,
            ProjectItemGrade.REFINED,
            5,
            Map.of(
                    R01GatheringRules.IRON_ORE, 8,
                    R01GatheringRules.HARDWOOD, 2
            ),
            70L,
            "openworld_rpg:affix/physical_power"
    ),
    RIVER_PIKE_REFINED(
            "river_pike_refined",
            R01NessaMarketRules.RIVER_PIKE,
            ProjectItemGrade.REFINED,
            4,
            Map.of(
                    R01GatheringRules.IRON_ORE, 5,
                    R01GatheringRules.HARDWOOD, 3
            ),
            60L,
            "openworld_rpg:affix/end"
    ),
    RIVERWOOD_BOW_REFINED(
            "riverwood_bow_refined",
            R01NessaMarketRules.RIVERWOOD_BOW,
            ProjectItemGrade.REFINED,
            4,
            Map.of(
                    R01GatheringRules.HARDWOOD, 4,
                    TOUGH_HIDE, 2
            ),
            50L,
            "openworld_rpg:affix/dex"
    ),
    INITIATE_STAFF_REFINED(
            "initiate_staff_refined",
            R01NessaMarketRules.INITIATE_STAFF,
            ProjectItemGrade.REFINED,
            4,
            Map.of(
                    R01GatheringRules.HARDWOOD, 4,
                    R01CraftingRecipe.LOUXIA_GLOW, 2
            ),
            60L,
            "openworld_rpg:affix/int"
    ),
    WATCH_BUCKLER_REFINED(
            "watch_buckler_refined",
            R01NessaMarketRules.WATCH_BUCKLER,
            ProjectItemGrade.REFINED,
            4,
            Map.of(
                    R01GatheringRules.HARDWOOD, 4,
                    R01GatheringRules.IRON_ORE, 3
            ),
            50L,
            "openworld_rpg:affix/guard_strength"
    ),

    HEARTLAND_SWORD_SUPERIOR(
            "heartland_sword_superior",
            R01NessaMarketRules.HEARTLAND_ARMING_SWORD,
            ProjectItemGrade.SUPERIOR,
            6,
            superiorOf(HEARTLAND_SWORD_REFINED),
            150L,
            "openworld_rpg:affix/str"
    ),
    QUARRY_MAUL_SUPERIOR(
            "quarry_maul_superior",
            R01NessaMarketRules.QUARRY_MAUL,
            ProjectItemGrade.SUPERIOR,
            6,
            superiorOf(QUARRY_MAUL_REFINED),
            180L,
            "openworld_rpg:affix/physical_power"
    ),
    RIVER_PIKE_SUPERIOR(
            "river_pike_superior",
            R01NessaMarketRules.RIVER_PIKE,
            ProjectItemGrade.SUPERIOR,
            6,
            superiorOf(RIVER_PIKE_REFINED),
            150L,
            "openworld_rpg:affix/end"
    ),
    RIVERWOOD_BOW_SUPERIOR(
            "riverwood_bow_superior",
            R01NessaMarketRules.RIVERWOOD_BOW,
            ProjectItemGrade.SUPERIOR,
            6,
            superiorOf(RIVERWOOD_BOW_REFINED),
            130L,
            "openworld_rpg:affix/dex"
    ),
    INITIATE_STAFF_SUPERIOR(
            "initiate_staff_superior",
            R01NessaMarketRules.INITIATE_STAFF,
            ProjectItemGrade.SUPERIOR,
            6,
            superiorOf(INITIATE_STAFF_REFINED),
            150L,
            "openworld_rpg:affix/int"
    ),
    WATCH_BUCKLER_SUPERIOR(
            "watch_buckler_superior",
            R01NessaMarketRules.WATCH_BUCKLER,
            ProjectItemGrade.SUPERIOR,
            6,
            superiorOf(WATCH_BUCKLER_REFINED),
            130L,
            "openworld_rpg:affix/guard_strength"
    );

    public static final String FORGE_SERVICE_ID =
            "openworld_rpg:service/alderford/holt_forge";
    public static final String TOUGH_HIDE = "openworld_rpg:tough_hide";

    private final String id;
    private final String baseId;
    private final ProjectItemGrade grade;
    private final int itemLevel;
    private final Map<String, Integer> materialCosts;
    private final long goldCost;
    private final String fixedAffixId;

    R01SmithingRecipe(
            String path,
            String baseId,
            ProjectItemGrade grade,
            int itemLevel,
            Map<String, Integer> materialCosts,
            long goldCost,
            String fixedAffixId
    ) {
        this.id = "openworld_rpg:recipe/smithing/" + path;
        this.baseId = Objects.requireNonNull(baseId, "baseId");
        this.grade = Objects.requireNonNull(grade, "grade");
        this.itemLevel = itemLevel;
        this.materialCosts = Map.copyOf(
                Objects.requireNonNull(materialCosts, "materialCosts")
        );
        this.goldCost = goldCost;
        this.fixedAffixId = Objects.requireNonNull(
                fixedAffixId,
                "fixedAffixId"
        );
        if (grade != ProjectItemGrade.REFINED
                && grade != ProjectItemGrade.SUPERIOR) {
            throw new IllegalArgumentException(
                    "R01 ordinary forge recipes are Refined/Superior only."
            );
        }
        if (itemLevel < 1 || goldCost < 0L) {
            throw new IllegalArgumentException(
                    "Invalid R01 smithing recipe level/cost."
            );
        }
    }

    private static Map<String, Integer> superiorOf(
            R01SmithingRecipe refined
    ) {
        Map<String, Integer> next = new HashMap<>(
                refined.materialCosts
        );
        next.put(R01GatheringRules.VERDANT_CRYSTAL, 2);
        return Map.copyOf(next);
    }

    public String id() {
        return id;
    }

    public String baseId() {
        return baseId;
    }

    public ProjectItemGrade grade() {
        return grade;
    }

    public int itemLevel() {
        return itemLevel;
    }

    public Map<String, Integer> materialCosts() {
        return materialCosts;
    }

    public long goldCost() {
        return goldCost;
    }

    public String fixedAffixId() {
        return fixedAffixId;
    }

    public boolean requiresVerdantDiscovery() {
        return grade == ProjectItemGrade.SUPERIOR;
    }

    public ProjectEquipmentSlot resolvedSlot() {
        return baseId.equals(R01NessaMarketRules.WATCH_BUCKLER)
                ? ProjectEquipmentSlot.OFF_HAND
                : ProjectEquipmentSlot.MAIN_WEAPON;
    }

    public long unitSellValue() {
        R01NessaMarketRules.MarketCategory category =
                baseId.equals(R01NessaMarketRules.WATCH_BUCKLER)
                        ? R01NessaMarketRules.MarketCategory.OFF_HAND
                        : R01NessaMarketRules.MarketCategory.WEAPON;
        return R01NessaMarketRules.sellBackGold(
                R01NessaMarketRules.priceGold(category, grade)
        );
    }

    public static Optional<R01SmithingRecipe> byId(String id) {
        Objects.requireNonNull(id, "id");
        return Arrays.stream(values())
                .filter(value -> value.id.equals(id))
                .findFirst();
    }
}
