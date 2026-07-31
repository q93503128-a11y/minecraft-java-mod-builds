package kr.countrysidedays;

import com.mojang.logging.LogUtils;
import kr.countrysidedays.gameplay.KitchenInteractionHandler;
import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.registry.ModCreativeTabs;
import kr.countrysidedays.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CountrysideDays.MOD_ID)
public final class CountrysideDays {
    public static final String MOD_ID = "countrysidedays";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CountrysideDays(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(KitchenInteractionHandler::onUseItemOnBlock);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Countryside Days {} core content registered", "0.1.0-alpha.2");
    }
}
