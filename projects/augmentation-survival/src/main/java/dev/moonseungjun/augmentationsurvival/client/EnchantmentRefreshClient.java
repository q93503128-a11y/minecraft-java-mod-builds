package dev.moonseungjun.augmentationsurvival.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;

/** Compact in-context refresh control for Enchant Refresh owners. */
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
            int x = left + IMAGE_WIDTH + 5;
            int y = top + 5;
            if (x + 20 > scaledWidth - 2) {
                x = left + IMAGE_WIDTH - 20;
                y = Math.max(2, top - 22);
            }

            Button button = Button.builder(Component.literal("↻"), ignored -> {
                        if (client.gameMode == null) return;
                        client.gameMode.handleInventoryButtonClick(
                                enchantmentScreen.getMenu().containerId,
                                REFRESH_BUTTON_ID
                        );
                    })
                    .bounds(x, y, 20, 20)
                    .build();
            button.setTooltip(Tooltip.create(
                    Component.literal("후보 새로고침 · XP 5 · 청금석 1")
            ));
            Screens.getWidgets(screen).add(button);
        });
    }
}
