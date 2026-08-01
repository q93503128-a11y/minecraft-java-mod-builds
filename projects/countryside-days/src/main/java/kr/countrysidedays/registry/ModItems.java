package kr.countrysidedays.registry;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.item.LifeGuideItem;
import kr.countrysidedays.item.RecipeNotebookItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CountrysideDays.MOD_ID);

    public static final DeferredItem<BlockItem> COUNTRY_KITCHEN_COUNTER = ITEMS.registerSimpleBlockItem(
            "country_kitchen_counter",
            ModBlocks.COUNTRY_KITCHEN_COUNTER
    );

    public static final DeferredItem<Item> WILD_HERB = ITEMS.registerSimpleItem("wild_herb");

    public static final DeferredItem<Item> RIVER_FISH = ITEMS.registerSimpleItem(
            "river_fish",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2F)
                    .build())
    );

    public static final DeferredItem<Item> COUNTRY_STEW = ITEMS.registerSimpleItem(
            "country_stew",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(0.8F)
                    .build())
    );

    public static final DeferredItem<Item> HERB_TEA = ITEMS.registerSimpleItem(
            "herb_tea",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.5F)
                    .alwaysEdible()
                    .build())
    );

    public static final DeferredItem<Item> FARM_BREAKFAST = ITEMS.registerSimpleItem(
            "farm_breakfast",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(9)
                    .saturationModifier(0.9F)
                    .build())
    );

    public static final DeferredItem<Item> GRILLED_RIVER_FISH = ITEMS.registerSimpleItem(
            "grilled_river_fish",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.7F)
                    .build())
    );

    public static final DeferredItem<Item> POTATO_PANCAKE = ITEMS.registerSimpleItem(
            "potato_pancake",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(7)
                    .saturationModifier(0.8F)
                    .build())
    );

    public static final DeferredItem<Item> HONEY_CARROT_SALAD = ITEMS.registerSimpleItem(
            "honey_carrot_salad",
            properties -> properties.food(new FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.6F)
                    .build())
    );

    public static final DeferredItem<RecipeNotebookItem> RECIPE_NOTEBOOK = ITEMS.registerItem(
            "recipe_notebook",
            RecipeNotebookItem::new,
            properties -> properties.stacksTo(1)
    );

    public static final DeferredItem<LifeGuideItem> LIFE_GUIDE = ITEMS.registerItem(
            "life_guide",
            LifeGuideItem::new,
            properties -> properties.stacksTo(1)
    );

    public static final DeferredItem<Item> VILLAGE_COIN = ITEMS.registerSimpleItem(
            "village_coin",
            properties -> properties.stacksTo(64)
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
