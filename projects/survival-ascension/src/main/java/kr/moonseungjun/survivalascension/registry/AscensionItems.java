package kr.moonseungjun.survivalascension.registry;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AscensionItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SurvivalAscension.MOD_ID);

    public static final DeferredItem<Item> TEMPERING_SEAL = ITEMS.registerSimpleItem(
            "tempering_seal",
            properties -> properties
                    .stacksTo(16)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    );

    public static final DeferredItem<Item> ENCHANTMENT_STONE = ITEMS.registerSimpleItem(
            "enchantment_stone",
            properties -> properties
                    .stacksTo(16)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    );

    private AscensionItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
