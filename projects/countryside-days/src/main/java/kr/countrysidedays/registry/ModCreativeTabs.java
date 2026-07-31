package kr.countrysidedays.registry;

import kr.countrysidedays.CountrysideDays;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            CountrysideDays.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.countrysidedays.main"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.COUNTRY_STEW.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COUNTRY_KITCHEN_COUNTER.get());
                        output.accept(ModItems.WILD_HERB.get());
                        output.accept(ModItems.RIVER_FISH.get());
                        output.accept(ModItems.COUNTRY_STEW.get());
                        output.accept(ModItems.RECIPE_NOTEBOOK.get());
                        output.accept(ModItems.VILLAGE_COIN.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
