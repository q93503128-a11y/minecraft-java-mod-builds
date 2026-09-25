package dev.moonseungjun.openworldrpg;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildPublisher;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerVitalsRuntime;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyAttachments;
import dev.moonseungjun.openworldrpg.integration.bootstrap.IntegrationBootstrap;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.verify.M0PlayerVerificationBootstrap;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01ClassStarterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01MainQuestService;
import dev.moonseungjun.openworldrpg.progression.r01.R01OpeningBootstrapService;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateAttachments;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RoadsideTroubleController;
import dev.moonseungjun.openworldrpg.progression.r01.R01SharedWorldAttachments;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionAttachments;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionService;
import dev.moonseungjun.openworldrpg.recovery.RecoveryBeltAttachments;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeAttachments;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import net.fabricmc.api.ModInitializer;
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
        PlayerEquipmentAttachments.initialize();
        R01PlayerStateAttachments.initialize();
        R01SharedWorldAttachments.initialize();
        PlayerCurrencyAttachments.initialize();
        PlayerRewardTransactionAttachments.initialize();
        PlayerInventoryAttachments.initialize();
        RecoveryBeltAttachments.initialize();
        PlayerActiveWorldTimeAttachments.initialize();
        PlayerVitalsRuntime.initialize();
        IntegrationBootstrap.bootstrap(profile, LOGGER);
        M0PlayerVerificationBootstrap.registerCommands();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerActiveWorldTimeService.tickLoadedPlayers(server);
            R01RoadsideTroubleController.tickActiveWorld(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!M0PlayerVerificationBootstrap.enabled()) {
                R01OpeningBootstrapService.ensureOpeningLoadout(handler.getPlayer());
                R01ClassStarterService.reconcileInterruptedGrant(handler.getPlayer());
            }
            PlayerRewardTransactionService.resumePending(handler.getPlayer());
            if (!M0PlayerVerificationBootstrap.enabled()) {
                R01PlayerStateService.reconcileActiveTimeEpochs(handler.getPlayer());
                R01RoadsideTroubleController.reconcilePendingFinalization(
                        handler.getPlayer()
                );
                R01MainQuestService.reconcileCommittedRewards(handler.getPlayer());
            }
            PlayerCombatBuildPublisher.refresh(handler.getPlayer());
            M0PlayerVerificationBootstrap.prepare(handler.getPlayer(), LOGGER);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CombatStateServices.disconnect(handler.getPlayer().getUUID())
        );

        LOGGER.info("Openworld RPG M0 integration bootstrap loaded with profile {}.", profile.id());
    }
}
