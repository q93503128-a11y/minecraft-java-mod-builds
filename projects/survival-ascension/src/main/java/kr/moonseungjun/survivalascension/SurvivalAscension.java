package kr.moonseungjun.survivalascension;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.survivalascension.command.AscensionCommands;
import kr.moonseungjun.survivalascension.mining.MiningProgression;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SurvivalAscension.MOD_ID)
public final class SurvivalAscension {
    public static final String MOD_ID = "survivalascension";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SurvivalAscension(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(MiningProgression::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(AscensionCommands::onRegisterCommands);
        LOGGER.info("Survival Ascension {} loaded: mining progression 0-100, area tiers 1/3/5/7", VERSION);
    }
}
