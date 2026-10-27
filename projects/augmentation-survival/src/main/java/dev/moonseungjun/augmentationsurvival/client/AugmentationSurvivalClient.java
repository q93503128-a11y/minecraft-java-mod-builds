package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.companion.CloneEntityTypes;
import dev.moonseungjun.augmentationsurvival.client.render.CombatCloneRenderer;
import dev.moonseungjun.augmentationsurvival.client.render.InvulnerabilityShieldLayer;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentStorageScreen;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenus;
import dev.moonseungjun.augmentationsurvival.menu.AugmentStorageMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

/** Client registration for augmentation screens, HUD and shared input runtimes. */
public final class AugmentationSurvivalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(AugmentDeviceMenus.BASIC, AugmentDeviceScreen::new);
        MenuScreens.register(AugmentDeviceMenus.ADVANCED, AugmentDeviceScreen::new);
        MenuScreens.register(AugmentStorageMenus.STORAGE, AugmentStorageScreen::new);
        GuidebookClient.initialize();
        EntityRenderers.register(CloneEntityTypes.COMBAT_CLONE, CombatCloneRenderer::new);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
                helper.register(new InvulnerabilityShieldLayer(avatarRenderer));
            }
        });
        ClientInvulnerabilityShieldState.initialize();
        ActiveLoadoutClientNetworking.initialize();
        CombatHudClient.initialize();
        ConsumptionClientNetworking.initialize();
        TemporalClientNetworking.initialize();
        PhaseClientNetworking.initialize();
        PerceptionClientNetworking.initialize();
        EnchantmentRefreshClient.initialize();
        ClientMiningPredictionState.initialize();
        MovementInputClient.initialize();
        ActiveAbilityInputClient.initialize();
        AuthorityTheftInputClient.initialize();
        StorageInputClient.initialize();
        SpatialLinkClientNetworking.initialize();
        SpatialLinkInputClient.initialize();
    }
}
