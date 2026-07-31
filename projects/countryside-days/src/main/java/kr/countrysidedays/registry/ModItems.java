package kr.countrysidedays.registry;

import kr.countrysidedays.CountrysideDays;
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

    public static final DeferredItem<RecipeNotebookItem> RECIPE_NOTEBOOK = ITEMS.register(
            "recipe_notebook",
            () -> new RecipeNotebookItem(new Item.Properties().stacksTo(1))
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
