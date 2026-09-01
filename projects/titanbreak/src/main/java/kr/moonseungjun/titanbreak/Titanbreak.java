package kr.moonseungjun.titanbreak;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.titanbreak.augmentation.AugmentationEffectService;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.ArmorAugmentationService;
import kr.moonseungjun.titanbreak.combat.AugmentAbilityService;
import kr.moonseungjun.titanbreak.combat.AugmentedMobilityService;
import kr.moonseungjun.titanbreak.combat.BastionRewardService;
import kr.moonseungjun.titanbreak.combat.CirculatoryAugmentationService;
import kr.moonseungjun.titanbreak.combat.CombatAutopilotService;
import kr.moonseungjun.titanbreak.combat.DamageChannelService;
import kr.moonseungjun.titanbreak.combat.EnemyAttackEffectService;
import kr.moonseungjun.titanbreak.combat.GravemarchRewardService;
import kr.moonseungjun.titanbreak.combat.HuntRewardService;
import kr.moonseungjun.titanbreak.combat.LegAugmentationService;
import kr.moonseungjun.titanbreak.combat.MotorSyncService;
import kr.moonseungjun.titanbreak.combat.NeuralCombatAssistService;
import kr.moonseungjun.titanbreak.combat.NeuralResponseService;
import kr.moonseungjun.titanbreak.combat.OcularAugmentationService;
import kr.moonseungjun.titanbreak.combat.OpticalCamoService;
import kr.moonseungjun.titanbreak.combat.OverdriveCirculationService;
import kr.moonseungjun.titanbreak.combat.PrecisionToolArmService;
import kr.moonseungjun.titanbreak.combat.ReflexDriveService;
import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import kr.moonseungjun.titanbreak.combat.SpineAugmentationService;
import kr.moonseungjun.titanbreak.combat.ThreatDetectionService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.player.VanillaArmorLockout;
import kr.moonseungjun.titanbreak.registry.ModBlocks;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import kr.moonseungjun.titanbreak.registry.ModItems;
import kr.moonseungjun.titanbreak.station.StationService;
import kr.moonseungjun.titanbreak.world.BastionEncounterService;
import kr.moonseungjun.titanbreak.world.BastionTraversalService;
import kr.moonseungjun.titanbreak.world.EncounterDirector;
import kr.moonseungjun.titanbreak.world.GravemarchEncounterService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Titanbreak.MOD_ID)
public final class Titanbreak {
    public static final String MOD_ID = "titanbreak";
    public static final String VERSION = "0.1.0-alpha.46";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final double OVERHEAT_LOCK = 95.0D;
    private static final double OVERHEAT_RESTART = 45.0D;
    private static final double POWER_RESTART_FRACTION = 0.10D;

    public Titanbreak(IEventBus modEventBus) {
        TitanPlayerData.verifyPersistenceContract();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBossEntities.register(modEventBus);
        modEventBus.addListener(TitanbreakNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(HuntRewardService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(GravemarchRewardService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(BastionRewardService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(SpineAugmentationService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EnemyAttackEffectService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(AugmentAbilityService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LegAugmentationService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SpineAugmentationService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeuralResponseService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ArmorAugmentationService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, CirculatoryAugmentationService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeuralResponseService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(StationService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("TITANBREAK {} loaded", VERSION);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).ensureProfile(player);
        ReflexFieldService.clear(player.getUUID());
        AugmentedMobilityService.clear(player);
        AugmentationEffectService.clear(player);
        AnalysisJammingService.clear(player.getUUID());
        AugmentAbilityService.clear(player.getUUID());
        LegAugmentationService.clear(player.getUUID());
        SpineAugmentationService.clear(player.getUUID());
        ArmorAugmentationService.clear(player.getUUID());
        CirculatoryAugmentationService.clear(player.getUUID());
        DamageChannelService.clear(player.getUUID());
        NeuralCombatAssistService.clear(player.getUUID());
        ThreatDetectionService.clear(player.getUUID());
        NeuralResponseService.clear(player);
        CombatAutopilotService.clear(player.getUUID());
        PrecisionToolArmService.clear(player);
        OverdriveCirculationService.clear(player);
        StationService.clear(player.getUUID());
        EncounterDirector.clear(player.getUUID());
        GravemarchEncounterService.clear(player.getUUID());
        BastionEncounterService.clear(player.getUUID());
        TitanbreakNetwork.sync(player);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReflexDriveService.clear(player.getUUID());
        ReflexFieldService.clear(player.getUUID());
        AugmentedMobilityService.clear(player);
        AugmentationEffectService.clear(player);
        AnalysisJammingService.clear(player.getUUID());
        AugmentAbilityService.clear(player.getUUID());
        LegAugmentationService.clear(player.getUUID());
        SpineAugmentationService.clear(player.getUUID());
        ArmorAugmentationService.clear(player.getUUID());
        CirculatoryAugmentationService.clear(player.getUUID());
        DamageChannelService.clear(player.getUUID());
        NeuralCombatAssistService.clear(player.getUUID());
        ThreatDetectionService.clear(player.getUUID());
        NeuralResponseService.clear(player);
        CombatAutopilotService.clear(player.getUUID());
        PrecisionToolArmService.clear(player);
        OverdriveCirculationService.clear(player);
        StationService.clear(player.getUUID());
        EncounterDirector.clear(player.getUUID());
        GravemarchEncounterService.clear(player.getUUID());
        BastionEncounterService.clear(player.getUUID());
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReflexDriveService.clear(player.getUUID());
        ReflexFieldService.clear(player.getUUID());
        AugmentedMobilityService.clear(player);
        AugmentationEffectService.clear(player);
        AnalysisJammingService.clear(player.getUUID());
        AugmentAbilityService.clear(player.getUUID());
        LegAugmentationService.clear(player.getUUID());
        SpineAugmentationService.clear(player.getUUID());
        ArmorAugmentationService.clear(player.getUUID());
        CirculatoryAugmentationService.clear(player.getUUID());
        DamageChannelService.clear(player.getUUID());
        NeuralCombatAssistService.clear(player.getUUID());
        ThreatDetectionService.clear(player.getUUID());
        NeuralResponseService.clear(player);
        CombatAutopilotService.clear(player.getUUID());
        PrecisionToolArmService.clear(player);
        OverdriveCirculationService.clear(player);
        StationService.clear(player.getUUID());
        EncounterDirector.clear(player.getUUID());
        GravemarchEncounterService.clear(player.getUUID());
        BastionEncounterService.clear(player.getUUID());
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).ensureProfile(player);
        TitanbreakNetwork.sync(player);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        VanillaArmorLockout.tick(player);
        EnemyAttackEffectService.tick(player);
        AugmentationResourceService.tick(player, state);
        AugmentationEffectService.tick(player, state);
        OcularAugmentationService.tick(player, state);
        NeuralResponseService.tick(player, state);
        CirculatoryAugmentationService.tick(player, state);
        OverdriveCirculationService.tick(player, state);
        OpticalCamoService.tick(player, state);
        NeuralCombatAssistService.tick(player, state);
        ThreatDetectionService.tick(player, state);
        LegAugmentationService.tick(player, state);
        CombatAutopilotService.tick(player, state);
        MotorSyncService.tick(player, state);
        SpineAugmentationService.tick(player, state);
        PrecisionToolArmService.tick(player, state);
        AugmentAbilityService.tick(player);
        StationService.tick(player);
        EncounterDirector.tick(player, state);
        GravemarchEncounterService.tick(player, state);
        BastionEncounterService.tick(player, state);
        BastionTraversalService.tick(player, state);

        TitanPlayerData.AugmentInstance drive = state.firstInstalledInstance("reflex_drive_i");
        boolean installed = drive != null;
        int driveMk = drive == null ? 1 : drive.mk();
        int rating = ReflexDriveService.ratingForMk(driveMk);
        double radius = ReflexDriveService.radiusForMk(driveMk);
        if (!installed) ReflexDriveService.setRequested(player, false, rating);
        else ReflexDriveService.updateRating(player, rating);

        AugmentationResourceService.Snapshot resources = AugmentationResourceService.snapshot(state);
        boolean requested = installed && ReflexDriveService.requested(player.getUUID());
        boolean wasActive = ReflexDriveService.active(player.getUUID());
        boolean heatReady = wasActive ? state.heat() < OVERHEAT_LOCK : state.heat() < OVERHEAT_RESTART;
        double continuousPowerCost = installed
                ? AugmentationResourceService.continuousPowerCostPerTick(state, "reflex_drive_i") : 0.0D;
        double currentPower = AugmentationResourceService.currentPower(player, state);
        double requiredPower = wasActive
                ? continuousPowerCost
                : Math.max(continuousPowerCost, resources.powerCapacity() * POWER_RESTART_FRACTION);
        boolean resourceReady = !resources.neuralOverloaded() && currentPower + 1.0E-6D >= requiredPower;
        boolean active = requested && heatReady && resourceReady;

        if (active && !AugmentationResourceService.trySpendContinuousPower(player, state, "reflex_drive_i")) active = false;

        ReflexDriveService.setActive(player, active);
        ReflexFieldService.update(player, active, rating, radius);

        if (active) {
            double enhancementEfficiency = 1.0D - Math.min(0.15D, drive.enhancement() * 0.015D);
            double masteryEfficiency = state.heatLoadMultiplier("reflex_drive_i");
            double rawHeatPerTick = ReflexDriveService.heatPerTickForMk(driveMk) * enhancementEfficiency * masteryEfficiency;
            double normalizedHeat = AugmentationResourceService.normalizedHeatGain(state, rawHeatPerTick);
            data.setHeat(player, state.heat() + normalizedHeat);
            double sanityDrain = (state.masteryLevel("reflex_drive_i") >= 5 ? 0.0015D : 0.002D)
                    * NeuralResponseService.sanityDrainMultiplier(state);
            data.setSanity(player, state.sanity() - sanityDrain);
            if (player.tickCount % 20 == 0) data.addMasteryXp(player, "reflex_drive_i", 2);
        } else if (!OverdriveCirculationService.active(player.getUUID())) {
            data.setHeat(player, state.heat() - resources.coolingPerTick());
        }

        AugmentedMobilityService.clear(player);
        if (player.tickCount % 5 == 0) TitanbreakNetwork.sync(player);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        EncounterDirector.clearAll();
        GravemarchEncounterService.clearAll();
        BastionEncounterService.clearAll();
        EnemyAttackEffectService.clearAll();
        AnalysisJammingService.clearAll();
        AugmentAbilityService.clearAll();
        LegAugmentationService.clearAll();
        SpineAugmentationService.clearAll();
        ArmorAugmentationService.clearAll();
        CirculatoryAugmentationService.clearAll();
        DamageChannelService.clearAll();
        NeuralCombatAssistService.clearAll();
        ThreatDetectionService.clearAll();
        NeuralResponseService.clearAll();
        CombatAutopilotService.clearAll();
        OverdriveCirculationService.clearAll();
        ReflexFieldService.clearAll();
        AugmentationResourceService.clearAll();
        ReflexDriveService.restore(event.getServer());
    }
}
