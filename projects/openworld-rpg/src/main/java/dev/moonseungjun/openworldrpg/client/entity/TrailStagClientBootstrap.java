package dev.moonseungjun.openworldrpg.client.entity;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.entity.OpenworldRpgEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client registration for the project-owned Trail Stag presentation. */
public final class TrailStagClientBootstrap implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                OpenworldRpgEntityTypes.TRAIL_STAG,
                TrailStagRenderer::new
        );
        OpenworldRpgMod.LOGGER.info("Openworld RPG Trail Stag skinned GLB renderer registered.");
    }
}
