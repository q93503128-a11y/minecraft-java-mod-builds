package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Titanbreak.MOD_ID);

    public static final DeferredItem<Item> REFLEX_DRIVE_I = ITEMS.registerItem(
            "reflex_drive_i",
            properties -> new Item(properties.stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SERVO_BUNDLE = material("servo_bundle");
    public static final DeferredItem<Item> SYNTHETIC_TENDON = material("synthetic_tendon");
    public static final DeferredItem<Item> HIGH_DENSITY_MUSCLE_FIBER = material("high_density_muscle_fiber");
    public static final DeferredItem<Item> HIGH_DENSITY_NEURAL_FIBER = material("high_density_neural_fiber");

    private ModItems() {}

    private static DeferredItem<Item> material(String id) {
        return ITEMS.registerItem(id, properties -> new Item(properties));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(REFLEX_DRIVE_I.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(SERVO_BUNDLE.get());
            event.accept(SYNTHETIC_TENDON.get());
            event.accept(HIGH_DENSITY_MUSCLE_FIBER.get());
            event.accept(HIGH_DENSITY_NEURAL_FIBER.get());
        }
    }
}
