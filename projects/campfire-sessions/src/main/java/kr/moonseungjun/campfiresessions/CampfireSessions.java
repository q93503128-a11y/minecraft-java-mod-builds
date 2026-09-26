package kr.moonseungjun.campfiresessions;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.campfiresessions.gameplay.ChairSeatManager;
import kr.moonseungjun.campfiresessions.registry.ModBlocks;
import kr.moonseungjun.campfiresessions.registry.ModCreativeTabs;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import kr.moonseungjun.campfiresessions.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CampfireSessions.MOD_ID)
public final class CampfireSessions {
    public static final String MOD_ID = "campfiresessions";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CampfireSessions(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(ChairSeatManager::onUseBlock);
        NeoForge.EVENT_BUS.addListener(ChairSeatManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(ChairSeatManager::onPlayerLoggedOut);
        LOGGER.info("Campfire Sessions {} registered", VERSION);
    }
}
