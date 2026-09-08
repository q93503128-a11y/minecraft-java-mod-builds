package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.EarthToStars;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EarthToStarsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EarthToStars.MOD_ID);

    public static final DeferredItem<Item> REINFORCED_FRAME = ITEMS.registerSimpleItem(
            "reinforced_frame",
            new Item.Properties().stacksTo(32)
    );
    public static final DeferredItem<Item> AVIONICS_UNIT = ITEMS.registerSimpleItem(
            "avionics_unit",
            new Item.Properties().stacksTo(16)
    );
    public static final DeferredItem<Item> PROPELLANT_CELL = ITEMS.registerSimpleItem(
            "propellant_cell",
            new Item.Properties().stacksTo(16)
    );
    public static final DeferredItem<Item> OXYGEN_CARTRIDGE = ITEMS.registerSimpleItem(
            "oxygen_cartridge",
            new Item.Properties().stacksTo(16)
    );
    public static final DeferredItem<Item> LIFE_SUPPORT_UNIT = ITEMS.registerSimpleItem(
            "life_support_unit",
            new Item.Properties().stacksTo(8)
    );
    public static final DeferredItem<LaunchCraftKitItem> LAUNCH_CRAFT_KIT = ITEMS.registerItem(
            "launch_craft_kit",
            LaunchCraftKitItem::new,
            new Item.Properties().stacksTo(1)
    );

    private EarthToStarsItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
