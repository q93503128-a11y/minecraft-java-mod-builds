package dev.moonseungjun.openworldrpg.client.entity;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.entity.OpenworldRpgEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client registration for the project-owned Meadow Viper presentation. */
public final class MeadowViperClientBootstrap implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                OpenworldRpgEntityTypes.MEADOW_VIPER,
                MeadowViperRenderer::new
        );
        OpenworldRpgMod.LOGGER.info("Openworld RPG Meadow Viper skinned GLB renderer registered.");
    }
}
