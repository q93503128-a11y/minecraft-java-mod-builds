package kr.moonseungjun.earthtostars.fabric.content;

import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class EarthToStarsFabricItems {
    public static final ResourceKey<Item> REINFORCED_FRAME_KEY = key("reinforced_frame");
    public static final ResourceKey<Item> AVIONICS_UNIT_KEY = key("avionics_unit");
    public static final ResourceKey<Item> LIFE_SUPPORT_UNIT_KEY = key("life_support_unit");
    public static final ResourceKey<Item> LAUNCH_CRAFT_KIT_KEY = key("launch_craft_kit");

    public static final Item REINFORCED_FRAME = register(
            REINFORCED_FRAME_KEY,
            Item::new,
            new Item.Properties().stacksTo(32)
    );
    public static final Item AVIONICS_UNIT = register(
            AVIONICS_UNIT_KEY,
            Item::new,
            new Item.Properties().stacksTo(16)
    );
    public static final Item LIFE_SUPPORT_UNIT = register(
            LIFE_SUPPORT_UNIT_KEY,
            Item::new,
            new Item.Properties().stacksTo(8)
    );
    public static final Item LAUNCH_CRAFT_KIT = register(
            LAUNCH_CRAFT_KIT_KEY,
            LaunchCraftKitItem::new,
            new Item.Properties().stacksTo(1)
    );

    private EarthToStarsFabricItems() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(REINFORCED_FRAME);
            entries.accept(AVIONICS_UNIT);
            entries.accept(LIFE_SUPPORT_UNIT);
            entries.accept(LAUNCH_CRAFT_KIT);
        });
    }

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, EarthToStarsFabric.id(path));
    }

    private static <T extends Item> T register(
            ResourceKey<Item> key,
            Function<Item.Properties, T> factory,
            Item.Properties properties
    ) {
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
