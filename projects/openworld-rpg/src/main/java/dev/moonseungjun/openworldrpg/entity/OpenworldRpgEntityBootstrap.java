package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01MeadowViperPoisonRuntime;
import dev.moonseungjun.openworldrpg.mount.TrailStagMountAttachments;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Registers project-owned entities and their entity-adjacent persistent authority. */
public final class OpenworldRpgEntityBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        OpenworldRpgEntityTypes.initialize();
        TrailStagMountAttachments.initialize();
        ServerTickEvents.END_SERVER_TICK.register(R01MeadowViperPoisonRuntime::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                R01MeadowViperPoisonRuntime.disconnect(handler.getPlayer().getUUID())
        );
        OpenworldRpgMod.LOGGER.info(
                "Openworld RPG project-owned entity bootstrap registered with persistent Trail Stag mount authority."
        );
    }
}
