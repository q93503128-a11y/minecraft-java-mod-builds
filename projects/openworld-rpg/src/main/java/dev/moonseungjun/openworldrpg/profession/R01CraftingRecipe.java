package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.market.R01FixedMerchantService;
import dev.moonseungjun.openworldrpg.recovery.R01NourishmentMeal;
import dev.moonseungjun.openworldrpg.recovery.RecoveryConsumable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Canon-locked R01 alchemy/cooking recipes. */
public enum R01CraftingRecipe {
    HEALING_POTION(
            "openworld_rpg:recipe/alchemy/healing_potion",
            R01FixedMerchantService.LYSA_SERVICE_ID,
            ProfessionMasteryState.Profession.ALCHEMY,
            Map.of(R01GatheringRules.HEALING_HERB, 2),
            5L
    ),
    FOCUS_DRAUGHT(
            "openworld_rpg:recipe/alchemy/focus_draught",
            R01FixedMerchantService.LYSA_SERVICE_ID,
            ProfessionMasteryState.Profession.ALCHEMY,
            Map.of(
                    R01GatheringRules.HEALING_HERB, 1,
                    "openworld_rpg:louxia_glow", 1
            ),
            8L
    ),
    CLEANSING_TONIC(
            "openworld_rpg:recipe/alchemy/cleansing_tonic",
            R01FixedMerchantService.LYSA_SERVICE_ID,
            ProfessionMasteryState.Profession.ALCHEMY,
            Map.of(
                    R01GatheringRules.HEALING_HERB, 1,
                    "openworld_rpg:louxia_glow", 1
            ),
            10L
    ),
    HERBED_LOUXIA_ROAST(
            "openworld_rpg:recipe/cooking/herbed_louxia_roast",
            R01FixedMerchantService.BRIN_SERVICE_ID,
            ProfessionMasteryState.Profession.COOKING,
            Map.of(
                    "openworld_rpg:louxia_meat", 2,
                    R01GatheringRules.HEALING_HERB, 1
            ),
            0L
    ),
    TRAIL_SKEWERS(
            "openworld_rpg:recipe/cooking/trail_skewers",
            R01FixedMerchantService.BRIN_SERVICE_ID,
            ProfessionMasteryState.Profession.COOKING,
            Map.of(
                    "openworld_rpg:louxia_meat", 1,
                    R01GatheringRules.HEALING_HERB, 1
            ),
            0L
    ),
    GLOW_BROTH(
            "openworld_rpg:recipe/cooking/glow_broth",
            R01FixedMerchantService.BRIN_SERVICE_ID,
            ProfessionMasteryState.Profession.COOKING,
            Map.of(
                    "openworld_rpg:louxia_meat", 1,
                    "openworld_rpg:louxia_glow", 1,
                    R01GatheringRules.HEALING_HERB, 1
            ),
            0L
    );

    public static final String LOUXIA_MEAT = "openworld_rpg:louxia_meat";
    public static final String LOUXIA_GLOW = "openworld_rpg:louxia_glow";

    private final String id;
    private final String serviceId;
    private final ProfessionMasteryState.Profession profession;
    private final Map<String, Integer> materialCosts;
    private final long unitGoldFee;

    R01CraftingRecipe(
            String id,
            String serviceId,
            ProfessionMasteryState.Profession profession,
            Map<String, Integer> materialCosts,
            long unitGoldFee
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.profession = Objects.requireNonNull(profession, "profession");
        this.materialCosts = Map.copyOf(
                Objects.requireNonNull(materialCosts, "materialCosts")
        );
        this.unitGoldFee = unitGoldFee;
    }

    public String id() {
        return id;
    }

    public String serviceId() {
        return serviceId;
    }

    public ProfessionMasteryState.Profession profession() {
        return profession;
    }

    public Map<String, Integer> unitMaterialCosts() {
        return materialCosts;
    }

    public long unitGoldFee() {
        return unitGoldFee;
    }

    public Map<String, Integer> materialCostsFor(int quantity) {
        requireQuantity(quantity);
        Map<String, Integer> total = new HashMap<>();
        materialCosts.forEach((id, amount) ->
                total.put(id, Math.multiplyExact(amount, quantity))
        );
        return Map.copyOf(total);
    }

    public long goldCostFor(int quantity) {
        requireQuantity(quantity);
        return Math.multiplyExact(unitGoldFee, quantity);
    }

    public ProjectInventoryItem outputChunk(int quantity) {
        requireQuantity(quantity);
        ProjectInventoryItem unit = outputUnit();
        if (quantity > unit.stackCap()) {
            throw new IllegalArgumentException(
                    "Craft output chunk exceeds item stack cap."
            );
        }
        return unit.withQuantity(quantity);
    }

    public ProjectInventoryItem outputUnit() {
        return switch (this) {
            case HEALING_POTION -> ProjectInventoryItem.recovery(
                    "openworld_rpg:healing_potion",
                    RecoveryConsumable.HEALING_POTION,
                    1,
                    0L
            );
            case FOCUS_DRAUGHT -> ProjectInventoryItem.recovery(
                    "openworld_rpg:focus_draught",
                    RecoveryConsumable.FOCUS_DRAUGHT,
                    1,
                    0L
            );
            case CLEANSING_TONIC -> ProjectInventoryItem.recovery(
                    "openworld_rpg:cleansing_tonic",
                    RecoveryConsumable.CLEANSING_TONIC,
                    1,
                    0L
            );
            case HERBED_LOUXIA_ROAST -> mealUnit(
                    R01NourishmentMeal.HERBED_LOUXIA_ROAST
            );
            case TRAIL_SKEWERS -> mealUnit(
                    R01NourishmentMeal.TRAIL_SKEWERS
            );
            case GLOW_BROTH -> mealUnit(
                    R01NourishmentMeal.GLOW_BROTH
            );
        };
    }

    public String insightFlag() {
        return "openworld_rpg:profession_insight/"
                + profession.name().toLowerCase(java.util.Locale.ROOT)
                + "/"
                + id.substring(id.lastIndexOf('/') + 1);
    }

    public static Optional<R01CraftingRecipe> byId(String id) {
        Objects.requireNonNull(id, "id");
        return Arrays.stream(values())
                .filter(value -> value.id.equals(id))
                .findFirst();
    }

    private static ProjectInventoryItem mealUnit(R01NourishmentMeal meal) {
        return ProjectInventoryItem.ordinary(
                meal.itemId(),
                1,
                50,
                0L
        );
    }

    private static void requireQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Craft quantity must be positive.");
        }
    }
}
