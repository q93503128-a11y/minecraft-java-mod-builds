package dev.moonseungjun.augmentationsurvival.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;

/** Small in-context refresh control for Enchant Refresh owners. */
public final class EnchantmentRefreshClient {
    private static final int REFRESH_BUTTON_ID = 3;

    private EnchantmentRefreshClient() {
    }

    public static void initialize() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof EnchantmentScreen enchantmentScreen)
                    || !ClientPerceptionState.enchantRefreshOwned()) {
                return;
            }
            int x = enchantmentScreen.getLeftPos() + enchantmentScreen.getImageWidth() - 54;
            int y = enchantmentScreen.getTopPos() + 5;
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
