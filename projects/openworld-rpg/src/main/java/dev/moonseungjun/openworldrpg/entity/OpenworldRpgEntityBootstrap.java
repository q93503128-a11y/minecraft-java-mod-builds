package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01MeadowViperPoisonRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Registers project-owned entities before the main integration bootstrap consumes their IDs. */
public final class OpenworldRpgEntityBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        OpenworldRpgEntityTypes.initialize();
        ServerTickEvents.END_SERVER_TICK.register(R01MeadowViperPoisonRuntime::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                R01MeadowViperPoisonRuntime.disconnect(handler.getPlayer().getUUID())
        );
        OpenworldRpgMod.LOGGER.info("Openworld RPG project-owned Meadow Viper entity bootstrap registered.");
    }
}
