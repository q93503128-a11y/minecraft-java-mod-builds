package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01MeadowViperPoisonRuntime;
import dev.moonseungjun.openworldrpg.mount.TrailStagBufferedAttackRuntime;
import dev.moonseungjun.openworldrpg.mount.TrailStagLiveBinding;
import dev.moonseungjun.openworldrpg.mount.TrailStagMountAttachments;
import dev.moonseungjun.openworldrpg.mount.TrailStagMountIntentRuntime;
import dev.moonseungjun.openworldrpg.network.TrailStagMountNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Registers project-owned entities and their entity-adjacent persistent authority. */
public final class OpenworldRpgEntityBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        OpenworldRpgEntityTypes.initialize();
        TrailStagMountAttachments.initialize();
        TrailStagMountNetworking.initialize();
        TrailStagLiveBinding.initialize();
        ServerTickEvents.END_SERVER_TICK.register(R01MeadowViperPoisonRuntime::tick);
        ServerTickEvents.END_SERVER_TICK.register(TrailStagMountIntentRuntime::tick);
        ServerTickEvents.END_SERVER_TICK.register(TrailStagBufferedAttackRuntime::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            R01MeadowViperPoisonRuntime.disconnect(handler.getPlayer().getUUID());
            TrailStagMountIntentRuntime.disconnect(handler.getPlayer().getUUID());
            TrailStagBufferedAttackRuntime.disconnect(handler.getPlayer().getUUID());
        });
        OpenworldRpgMod.LOGGER.info(
                "Openworld RPG project-owned entity bootstrap registered with Trail Stag live traversal authority."
        );
    }
}
