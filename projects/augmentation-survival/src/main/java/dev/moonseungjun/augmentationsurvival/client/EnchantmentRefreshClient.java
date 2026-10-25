package dev.moonseungjun.augmentationsurvival.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;

/** Small in-context refresh control for Enchant Refresh owners. */
public final class EnchantmentRefreshClient {
    private static final int REFRESH_BUTTON_ID = 3;
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    private EnchantmentRefreshClient() {
    }

    public static void initialize() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof EnchantmentScreen enchantmentScreen)
                    || !ClientPerceptionState.enchantRefreshOwned()) {
                return;
            }
            int left = (scaledWidth - IMAGE_WIDTH) / 2;
            int top = (scaledHeight - IMAGE_HEIGHT) / 2;
            int x = left + IMAGE_WIDTH - 54;
            int y = top + 5;
            Button button = Button.builder(Component.literal("새로고침"), ignored -> {
                        if (client.gameMode != null) {
                            client.gameMode.handleInventoryButtonClick(
                                    enchantmentScreen.getMenu().containerId,
                                    REFRESH_BUTTON_ID
                            );
                        }
                    })
                    .bounds(x, y, 48, 18)
                    .build();
            Screens.getWidgets(screen).add(button);
        });
    }
}
