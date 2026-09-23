package dev.moonseungjun.openworldrpg;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildPublisher;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionAttachments;
import dev.moonseungjun.openworldrpg.combat.state.PlayerVitalsRuntime;
import dev.moonseungjun.openworldrpg.integration.bootstrap.IntegrationBootstrap;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.verify.M0PlayerVerificationBootstrap;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateAttachments;
import net.fabricmc.api.ModInitializer;
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
        PlayerVitalsRuntime.initialize();
        IntegrationBootstrap.bootstrap(profile, LOGGER);
        M0PlayerVerificationBootstrap.registerCommands();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerCombatBuildPublisher.refresh(handler.getPlayer());
            M0PlayerVerificationBootstrap.prepare(handler.getPlayer(), LOGGER);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CombatStateServices.disconnect(handler.getPlayer().getUUID())
        );

        LOGGER.info("Openworld RPG M0 integration bootstrap loaded with profile {}.", profile.id());
    }
}
