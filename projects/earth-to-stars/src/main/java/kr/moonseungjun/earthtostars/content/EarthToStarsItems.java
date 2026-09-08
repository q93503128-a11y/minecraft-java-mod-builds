package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipSystemsManager;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EarthToStarsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EarthToStars.MOD_ID);

    public static final DeferredItem<Item> REINFORCED_FRAME = ITEMS.registerSimpleItem(
            "reinforced_frame",
            props -> props.stacksTo(32)
    );
    public static final DeferredItem<Item> AVIONICS_UNIT = ITEMS.registerSimpleItem(
            "avionics_unit",
            props -> props.stacksTo(16)
    );
    public static final DeferredItem<ShipSupplyItem> PROPELLANT_CELL = ITEMS.registerItem(
            "propellant_cell",
            props -> new ShipSupplyItem(props.stacksTo(16), ShipSystemsManager.SupplyType.PROPELLANT)
    );
    public static final DeferredItem<ShipSupplyItem> OXYGEN_CARTRIDGE = ITEMS.registerItem(
            "oxygen_cartridge",
            props -> new ShipSupplyItem(props.stacksTo(16), ShipSystemsManager.SupplyType.OXYGEN)
    );
    public static final DeferredItem<Item> LIFE_SUPPORT_UNIT = ITEMS.registerSimpleItem(
            "life_support_unit",
            props -> props.stacksTo(8)
    );
    public static final DeferredItem<LaunchCraftKitItem> LAUNCH_CRAFT_KIT = ITEMS.registerItem(
            "launch_craft_kit",
            LaunchCraftKitItem::new,
            props -> props.stacksTo(1)
    );

    private EarthToStarsItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
