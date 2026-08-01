package kr.moonseungjun.arcanecircle;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.arcanecircle.gameplay.MagicCircleInteractionHandler;
import kr.moonseungjun.arcanecircle.gameplay.StarterKitHandler;
import kr.moonseungjun.arcanecircle.registry.ModBlocks;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ArcaneCircle.MOD_ID)
public final class ArcaneCircle {
    public static final String MOD_ID = "arcanecircle";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneCircle(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(MagicCircleInteractionHandler::onUseItemOnBlock);
        NeoForge.EVENT_BUS.addListener(MagicCircleInteractionHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(StarterKitHandler::onPlayerLoggedIn);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Arcane Circle Lab {} registered", VERSION);
    }
}
