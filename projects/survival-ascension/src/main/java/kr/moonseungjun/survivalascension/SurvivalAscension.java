package kr.moonseungjun.survivalascension;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.survivalascension.combat.CombatProgression;
import kr.moonseungjun.survivalascension.command.AscensionCommands;
import kr.moonseungjun.survivalascension.harvesting.HarvestingProgression;
import kr.moonseungjun.survivalascension.mining.MiningProgression;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.woodcutting.WoodcuttingProgression;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SurvivalAscension.MOD_ID)
public final class SurvivalAscension {
    public static final String MOD_ID = "survivalascension";
    public static final String VERSION = "0.5.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SurvivalAscension(IEventBus modEventBus) {
        modEventBus.addListener(SkillNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(WoodcuttingProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(HarvestingProgression::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(HarvestingProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(CombatProgression::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatProgression::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(AscensionCommands::onRegisterCommands);
        LOGGER.info("Survival Ascension {} loaded: mining, woodcutting, harvesting, combat, synced HUD and skills screen", VERSION);
    }
}
