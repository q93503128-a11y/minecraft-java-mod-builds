package kr.countrysidedays;

import com.mojang.logging.LogUtils;
import kr.countrysidedays.gameplay.KitchenInteractionHandler;
import kr.countrysidedays.gameplay.RanchLifeManager;
import kr.countrysidedays.gameplay.RuralGameplayHandler;
import kr.countrysidedays.gameplay.RuralNpcManager;
import kr.countrysidedays.gametest.ModGameTests;
import kr.countrysidedays.network.EstateHudPayload;
import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.registry.ModCreativeTabs;
import kr.countrysidedays.registry.ModFeatures;
import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysidePropertyManager;
import kr.countrysidedays.worldgen.CountrysideWorldgenAudit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(CountrysideDays.MOD_ID)
public final class CountrysideDays {
    public static final String MOD_ID = "countrysidedays";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CountrysideDays(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModGameTests.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(CountrysidePropertyManager::onUseBlock);
        NeoForge.EVENT_BUS.addListener(CountrysidePropertyManager::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(KitchenInteractionHandler::onUseItemOnBlock);
        NeoForge.EVENT_BUS.addListener(KitchenInteractionHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(RuralGameplayHandler::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(RuralGameplayHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RuralGameplayHandler::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(RuralGameplayHandler::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(RuralGameplayHandler::onItemFished);
        NeoForge.EVENT_BUS.addListener(RuralNpcManager::handleInteraction);
        NeoForge.EVENT_BUS.addListener(RanchLifeManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(RanchLifeManager::onUseBlock);
        NeoForge.EVENT_BUS.addListener(RanchLifeManager::onAnimalInteract);
        NeoForge.EVENT_BUS.addListener(RanchLifeManager::onAnimalDamage);
        NeoForge.EVENT_BUS.addListener(CountrysideWorldgenAudit::onServerStarted);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(EstateHudPayload.TYPE, EstateHudPayload.STREAM_CODEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Countryside Days {} core content registered", "0.1.0-alpha.8");
    }
}
