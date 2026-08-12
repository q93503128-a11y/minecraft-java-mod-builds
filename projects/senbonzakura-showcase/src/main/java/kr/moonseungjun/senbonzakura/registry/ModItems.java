package kr.moonseungjun.senbonzakura.registry;

import kr.moonseungjun.senbonzakura.SenbonzakuraShowcase;
import kr.moonseungjun.senbonzakura.item.ZanpakutoItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SenbonzakuraShowcase.MOD_ID);

    public static final DeferredItem<ZanpakutoItem> SENBONZAKURA = ITEMS.registerItem(
            "zanpakuto_senbonzakura",
            properties -> new ZanpakutoItem(properties
                    .sword(ToolMaterial.NETHERITE, 4.0F, -2.2F)
                    .rarity(Rarity.EPIC)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) event.accept(SENBONZAKURA.get());
    }
}
