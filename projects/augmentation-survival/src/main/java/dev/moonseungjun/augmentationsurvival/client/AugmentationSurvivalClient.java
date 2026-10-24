package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/** Client registration for augmentation workstation screens and movement input. */
public final class AugmentationSurvivalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(AugmentDeviceMenus.BASIC, AugmentDeviceScreen::new);
        MenuScreens.register(AugmentDeviceMenus.ADVANCED, AugmentDeviceScreen::new);
        MovementInputClient.initialize();
    }
}
