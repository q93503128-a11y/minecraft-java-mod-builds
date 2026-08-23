package kr.moonseungjun.survivalascension;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.survivalascension.combat.CombatProgression;
import kr.moonseungjun.survivalascension.command.AscensionCommands;
import kr.moonseungjun.survivalascension.construction.ConstructionProgression;
import kr.moonseungjun.survivalascension.elite.EliteMobSystem;
import kr.moonseungjun.survivalascension.elite.EndgameMutationSystem;
import kr.moonseungjun.survivalascension.elite.WarbandDirector;
import kr.moonseungjun.survivalascension.endgame.AscensionTrialSystem;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.harvesting.HarvestingProgression;
import kr.moonseungjun.survivalascension.harvesting.IrrigationReplantService;
import kr.moonseungjun.survivalascension.mining.BoreMiningService;
import kr.moonseungjun.survivalascension.mining.MiningProgression;
import kr.moonseungjun.survivalascension.mobility.MobilityProgression;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.woodcutting.WoodcuttingProgression;
import kr.moonseungjun.survivalascension.world.WorldAscensionProgression;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SurvivalAscension.MOD_ID)
public final class SurvivalAscension {
    public static final String MOD_ID = "survivalascension";
    public static final String VERSION = "0.21.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SurvivalAscension(IEventBus modEventBus) {
        modEventBus.addListener(SkillNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(MiningProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(BoreMiningService::onServerTick);
        NeoForge.EVENT_BUS.addListener(WoodcuttingProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(WoodcuttingProgression::onServerTick);
        NeoForge.EVENT_BUS.addListener(HarvestingProgression::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(HarvestingProgression::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(IrrigationReplantService::onServerTick);
        NeoForge.EVENT_BUS.addListener(CombatProgression::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatProgression::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(ConstructionProgression::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(ConstructionProgression::onServerTick);
        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onDamagePre);
        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onDamagePost);
        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EndgameMutationSystem::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(EndgameMutationSystem::onDamagePost);
        NeoForge.EVENT_BUS.addListener(EndgameMutationSystem::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(WarbandDirector::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(WarbandDirector::onServerTick);
        NeoForge.EVENT_BUS.addListener(WarbandDirector::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(AscensionTrialSystem::onServerTick);
        NeoForge.EVENT_BUS.addListener(AscensionTrialSystem::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(WorldAscensionProgression::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(AscensionAffixes::onEliteDeath);
        NeoForge.EVENT_BUS.addListener(AscensionCommands::onRegisterCommands);
        LOGGER.info("Survival Ascension {} loaded: six skills + mastery VI + tactical warbands + world ascension + endgame mutations + ascension trials", VERSION);
    }
}
