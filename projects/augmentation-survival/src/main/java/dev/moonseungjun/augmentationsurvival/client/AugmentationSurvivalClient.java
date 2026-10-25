package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentStorageScreen;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenus;
import dev.moonseungjun.augmentationsurvival.menu.AugmentStorageMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client registration for augmentation screens and shared input runtimes. */
public final class AugmentationSurvivalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(AugmentDeviceMenus.BASIC, AugmentDeviceScreen::new);
        MenuScreens.register(AugmentDeviceMenus.ADVANCED, AugmentDeviceScreen::new);
        MenuScreens.register(AugmentStorageMenus.STORAGE, AugmentStorageScreen::new);
        ConsumptionClientNetworking.initialize();
        TemporalClientNetworking.initialize();
        PhaseClientNetworking.initialize();
        PerceptionClientNetworking.initialize();
        EnchantmentRefreshClient.initialize();
        MovementInputClient.initialize();
        ActiveAbilityInputClient.initialize();
        StorageInputClient.initialize();
    }
}
