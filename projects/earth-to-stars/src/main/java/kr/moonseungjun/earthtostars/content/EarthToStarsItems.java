package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipSystemsManager;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

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
            props -> new ShipSupplyItem(props, ShipSystemsManager.SupplyType.PROPELLANT),
            props -> props.stacksTo(16)
    );
    public static final DeferredItem<ShipSupplyItem> OXYGEN_CARTRIDGE = ITEMS.registerItem(
            "oxygen_cartridge",
            props -> new ShipSupplyItem(props, ShipSystemsManager.SupplyType.OXYGEN),
            props -> props.stacksTo(16)
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
    public static final DeferredItem<RecoveredSensorCoreItem> RECOVERED_SENSOR_CORE = ITEMS.registerItem(
            "recovered_sensor_core",
            RecoveredSensorCoreItem::new,
            props -> props.stacksTo(1)
    );

    // Render-only tokens. They are intentionally never added to a creative tab or recipe.
    // ItemDisplay uses their baked OBJ models while gameplay remains on authoritative ship/mission state.
    public static final DeferredItem<Item> STARTER_CRAFT_VISUAL = ITEMS.registerSimpleItem(
            "starter_craft_visual",
            props -> props.stacksTo(1)
    );
    public static final DeferredItem<Item> ORBITAL_SALVAGE_VISUAL = ITEMS.registerSimpleItem(
            "orbital_salvage_visual",
            props -> props.stacksTo(1)
    );
    public static final DeferredItem<Item> ORBITAL_INTERCEPTOR_VISUAL = ITEMS.registerSimpleItem(
            "orbital_interceptor_visual",
            props -> props.stacksTo(1)
    );

    private static final List<DeferredItem<? extends Item>> CREATIVE_ITEMS = List.of(
            REINFORCED_FRAME,
            AVIONICS_UNIT,
            PROPELLANT_CELL,
            OXYGEN_CARTRIDGE,
            LIFE_SUPPORT_UNIT,
            LAUNCH_CRAFT_KIT,
            RECOVERED_SENSOR_CORE
    );

    private EarthToStarsItems() {
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return CREATIVE_ITEMS;
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
