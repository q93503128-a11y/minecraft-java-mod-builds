package dev.moonseungjun.openworldrpg;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildPublisher;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatSessionAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerVitalsRuntime;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyAttachments;
import dev.moonseungjun.openworldrpg.death.PlayerDeathPenaltyAttachments;
import dev.moonseungjun.openworldrpg.death.PlayerDeathPenaltyService;
import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentAffixCatalogRegistry;
import dev.moonseungjun.openworldrpg.fishing.R01FishingAttachments;
import dev.moonseungjun.openworldrpg.fishing.R01FishingService;
import dev.moonseungjun.openworldrpg.integration.bootstrap.IntegrationBootstrap;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineProjectSkillAccess;
import dev.moonseungjun.openworldrpg.integration.verify.M0PlayerVerificationBootstrap;
import dev.moonseungjun.openworldrpg.integration.verify.R01PlayerVerificationBootstrap;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringAttachments;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringService;
import dev.moonseungjun.openworldrpg.housing.R01HousingAttachments;
import dev.moonseungjun.openworldrpg.housing.R01HousingService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryAttachments;
import dev.moonseungjun.openworldrpg.market.R01NessaMarketAttachments;
import dev.moonseungjun.openworldrpg.network.ProjectCombatNetworking;
import dev.moonseungjun.openworldrpg.progression.PlayerClassAdvancementAttachments;
import dev.moonseungjun.openworldrpg.progression.PlayerClassAdvancementService;
import dev.moonseungjun.openworldrpg.progression.PlayerClassMilestoneAttachments;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressAttachments;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import dev.moonseungjun.openworldrpg.progression.PlayerClassSwitchAttachments;
import dev.moonseungjun.openworldrpg.progression.PlayerClassSwitchService;
import dev.moonseungjun.openworldrpg.progression.r01.R01ClassStarterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongBossLootPlanAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongBossLootPlanService;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongFirstClearRewardService;
import dev.moonseungjun.openworldrpg.progression.r01.R01MainQuestService;
import dev.moonseungjun.openworldrpg.progression.r01.R01OpeningBootstrapService;
import dev.moonseungjun.openworldrpg.progression.r01.R01PostQuarryService;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateService;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuestAttributionAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRunAttributionAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterController;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRunAttributionService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RoadsideTroubleController;
import dev.moonseungjun.openworldrpg.progression.r01.R01RepeatRewardAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01SharedWorldAttachments;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionAttachments;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import dev.moonseungjun.openworldrpg.recovery.RecoveryBeltAttachments;
import dev.moonseungjun.openworldrpg.recovery.RecoveryEffectRuntime;
import dev.moonseungjun.openworldrpg.recovery.RecoveryUseRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ClericRootPassiveRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ConsecratedGroundRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianProvokedRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianResolveRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianSkillRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterFanOfArrowsRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterPinningShotRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterPowerShotRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterSkyfallRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuickstepVolleyRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterSkillRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.MageArcaneWeaveRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.MageAstralConvergenceRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.MageFlameBurstRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.MageFrostRingRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.MagePhaseStepRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectBasicAttackCadenceRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerActionRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectUltimateChargeRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDodgeRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectBurningRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectGuardRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectHostileStatusRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.R01EarthloongMythicRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.SanctuaryRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorSkillRuntime;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeAttachments;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import dev.moonseungjun.openworldrpg.world.spatial.R01QuarrySpatialBindingRegistry;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingRegistry;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenworldRpgMod implements ModInitializer {
    public static final String MOD_ID = "openworld_rpg";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        RuntimeProfile profile = RuntimeProfile.current();
        PlayerProgressionAttachments.initialize();
        PlayerCombatSessionAttachments.initialize();
        PlayerEquipmentAttachments.initialize();
        R01PlayerStateAttachments.initialize();
        R01SharedWorldAttachments.initialize();
        R01EarthloongEncounterAttachments.initialize();
        R01EarthloongBossLootPlanAttachments.initialize();
        R01RepeatRewardAttachments.initialize();
        R01QuestAttributionAttachments.initialize();
        R01QuarryRunAttributionAttachments.initialize();
        R01QuarryRoomEncounterAttachments.initialize();
        PlayerCurrencyAttachments.initialize();
        PlayerDeathPenaltyAttachments.initialize();
        PlayerClassSwitchAttachments.initialize();
        PlayerClassAdvancementAttachments.initialize();
        PlayerClassMilestoneAttachments.initialize();
        PlayerPassiveProgressAttachments.initialize();
        ProjectCombatNetworking.initialize();
        OrdinaryEquipmentAffixCatalogRegistry.initialize(LOGGER);
        PlayerRewardTransactionAttachments.initialize();
        PlayerInventoryAttachments.initialize();
        R01NessaMarketAttachments.initialize();
        R01GatheringAttachments.initialize();
        R01FishingAttachments.initialize();
        R01HousingAttachments.initialize();
        RecoveryBeltAttachments.initialize();
        PlayerActiveWorldTimeAttachments.initialize();
        PlayerVitalsRuntime.initialize();
        R01SpatialBindingRegistry.initialize(LOGGER);
        R01QuarrySpatialBindingRegistry.initialize(LOGGER);
        R01AlderfordRuntimeBindingRegistry.initialize(LOGGER);
        R01PlayerVerificationBootstrap.verifyStaticContracts(LOGGER);
        IntegrationBootstrap.bootstrap(profile, LOGGER);
        M0PlayerVerificationBootstrap.registerCommands();
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            ProjectPlayerActionRuntime.tick(server);
            ProjectGuardRuntime.tick(server);
            ProjectDodgeRuntime.tick(server);
            HunterQuickstepVolleyRuntime.tick(server);
            HunterPinningShotRuntime.tick(server);
            HunterFanOfArrowsRuntime.tick(server);
            HunterPowerShotRuntime.tick(server);
            HunterSkyfallRuntime.tick(server);
            MagePhaseStepRuntime.tick(server);
            MageFrostRingRuntime.tick(server);
            MageFlameBurstRuntime.tick(server);
            MageAstralConvergenceRuntime.tick(server);
            MageArcaneWeaveRuntime.tick(server);
            GuardianSkillRuntime.tick(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerActiveWorldTimeService.tickLoadedPlayers(server);
            RecoveryEffectRuntime.tick(server);
            RecoveryUseRuntime.tick(server);
            ConsecratedGroundRuntime.tick(server);
            SanctuaryRuntime.tick(server);
            WarriorSkillRuntime.tick(server);
            ProjectHostileStatusRuntime.tick(server);
            GuardianProvokedRuntime.tick(server);
            ProjectBurningRuntime.tick(server);
            R01RoadsideTroubleController.tickActiveWorld(server);
            ProjectCombatNetworking.flushDodgeAccepted(server);
            if (Math.floorMod(server.getTickCount(), 20) == 0) {
                for (var player : server.getPlayerList().getPlayers()) {
                    CombatStateServices.persistRuntime(player);
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!M0PlayerVerificationBootstrap.enabled()) {
                R01OpeningBootstrapService.ensureOpeningLoadout(handler.getPlayer());
                R01ClassStarterService.reconcileInterruptedGrant(handler.getPlayer());
                PlayerDeathPenaltyService.reconcilePending(handler.getPlayer());
            }
            PlayerRewardTransactionService.resumePending(handler.getPlayer());
            if (!M0PlayerVerificationBootstrap.enabled()) {
                R01PlayerStateService.reconcileActiveTimeEpochs(handler.getPlayer());
                R01GatheringService.reconcilePending(handler.getPlayer());
                R01FishingService.reconcileInterruptedHooks(handler.getPlayer());
                R01HousingService.reconcilePending(handler.getPlayer());
                R01EarthloongEncounterService.reconcilePendingFinalization(handler.getPlayer());
                R01EarthloongBossLootPlanService.ensureFirstClearPlan(handler.getPlayer());
                R01QuarryRoomEncounterController.reconcilePendingAttributions(
                        handler.getPlayer()
                );
                R01QuarryRunAttributionService.reconcileFirstClearCompletion(
                        handler.getPlayer()
                );
                R01EarthloongFirstClearRewardService.reconcilePending(handler.getPlayer());
                R01PostQuarryService.reconcileImmediateAftermath(
                        handler.getPlayer()
                );
                R01RoadsideTroubleController.reconcilePendingFinalization(
                        handler.getPlayer()
                );
                R01MainQuestService.reconcileCommittedRewards(handler.getPlayer());
            }
            PlayerCombatBuildPublisher.refresh(handler.getPlayer());
            CombatStateServices.restoreRuntime(handler.getPlayer());
            if (!M0PlayerVerificationBootstrap.enabled()) {
                PlayerClassSwitchService.reconcilePending(handler.getPlayer());
                PlayerClassAdvancementService.reconcilePendingBranchSwitch(
                        handler.getPlayer()
                );
                PlayerPassiveProgressService.reconcilePending(
                        handler.getPlayer()
                );
            }
            R01PlayerVerificationBootstrap.prepare(handler.getPlayer(), LOGGER);
            M0PlayerVerificationBootstrap.prepare(handler.getPlayer(), LOGGER);
            SpellEngineProjectSkillAccess.refreshPublishedSkills(
                    handler.getPlayer()
            );
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && !M0PlayerVerificationBootstrap.enabled()) {
                PlayerDeathPenaltyService.applyAfterDeathRespawn(newPlayer);
            }
            WarriorSkillRuntime.reset(newPlayer.getUUID());
            ClericRootPassiveRuntime.reset(newPlayer.getUUID());
            HunterQuickstepVolleyRuntime.reset(newPlayer.getUUID());
            HunterPinningShotRuntime.reset(newPlayer.getUUID());
            HunterFanOfArrowsRuntime.reset(newPlayer.getUUID());
            HunterPowerShotRuntime.reset(newPlayer.getUUID());
            HunterSkyfallRuntime.reset(newPlayer.getUUID());
            HunterSkillRuntime.reset(newPlayer.getUUID());
            MageArcaneWeaveRuntime.reset(newPlayer.getUUID());
            MagePhaseStepRuntime.reset(newPlayer.getUUID());
            MageFrostRingRuntime.reset(newPlayer.getUUID());
            MageFlameBurstRuntime.reset(newPlayer.getUUID());
            MageAstralConvergenceRuntime.reset(newPlayer.getUUID());
            ProjectUltimateChargeRuntime.resetMageTransient(
                    newPlayer.getUUID()
            );
            ProjectUltimateChargeRuntime.resetGuardianTransient(
                    newPlayer.getUUID()
            );
            ProjectBurningRuntime.removeSource(newPlayer.getUUID());
            GuardianSkillRuntime.reset(newPlayer.getUUID());
            GuardianResolveRuntime.reset(newPlayer.getUUID());
            ProjectPlayerActionRuntime.reset(newPlayer);
            ProjectDodgeRuntime.reset(newPlayer);
            PlayerCombatBuildPublisher.refresh(newPlayer);
            SpellEngineProjectSkillAccess.refreshPublishedSkills(newPlayer);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CombatStateServices.persistRuntime(handler.getPlayer());
            var playerId = handler.getPlayer().getUUID();
            RecoveryUseRuntime.disconnect(playerId);
            RecoveryEffectRuntime.disconnect(playerId);
            R01EarthloongMythicRuntime.disconnect(playerId);
            ConsecratedGroundRuntime.disconnect(playerId);
            SanctuaryRuntime.disconnect(playerId);
            WarriorSkillRuntime.disconnect(playerId);
            ClericRootPassiveRuntime.disconnect(playerId);
            HunterQuickstepVolleyRuntime.disconnect(playerId);
            HunterPinningShotRuntime.disconnect(playerId);
            HunterFanOfArrowsRuntime.disconnect(playerId);
            HunterPowerShotRuntime.disconnect(playerId);
            HunterSkyfallRuntime.disconnect(playerId);
            HunterSkillRuntime.disconnect(playerId);
            MageArcaneWeaveRuntime.disconnect(playerId);
            MagePhaseStepRuntime.disconnect(playerId);
            MageFrostRingRuntime.disconnect(playerId);
            MageFlameBurstRuntime.disconnect(playerId);
            MageAstralConvergenceRuntime.disconnect(playerId);
            ProjectUltimateChargeRuntime.resetMageTransient(playerId);
            ProjectUltimateChargeRuntime.resetGuardianTransient(playerId);
            ProjectBurningRuntime.removeSource(playerId);
            GuardianSkillRuntime.disconnect(playerId);
            GuardianResolveRuntime.disconnect(playerId);
            ProjectPlayerActionRuntime.disconnect(playerId);
            ProjectDodgeRuntime.disconnect(playerId);
            ProjectBasicAttackCadenceRuntime.disconnect(playerId);
            CombatStateServices.disconnect(playerId);
        });

        LOGGER.info("Openworld RPG M0 integration bootstrap loaded with profile {}.", profile.id());
    }
}
