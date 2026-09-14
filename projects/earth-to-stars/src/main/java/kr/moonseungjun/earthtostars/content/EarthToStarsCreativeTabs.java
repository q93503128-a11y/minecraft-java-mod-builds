package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.EarthToStars;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class EarthToStarsCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            EarthToStars.MOD_ID
    );

    public static final Supplier<CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.earth_to_stars.main"))
                    .icon(() -> new ItemStack(EarthToStarsItems.LAUNCH_CRAFT_KIT.get()))
                    .displayItems((parameters, output) -> EarthToStarsItems.creativeItems()
                            .forEach(item -> output.accept(item.get())))
                    .build()
    );

    private EarthToStarsCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
