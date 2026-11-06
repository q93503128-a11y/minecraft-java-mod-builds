package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the mythic orbit marks after the screen draws its single card body. */
@Mixin(AugmentDeviceScreen.class)
abstract class AugmentDeviceCardPresentationMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    protected AugmentDeviceCardPresentationMixin(
            AugmentDeviceMenu menu, Inventory inventory, Component title, int imageWidth, int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Shadow private double revealProgress(int slot) { throw new AssertionError(); }

    @Inject(method = "drawAugmentCard", at = @At("TAIL"))
    private void augmentationSurvival$mythicOrbit(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, int slot,
            AugmentDefinition definition, int mouseX, int mouseY, boolean compact, CallbackInfo ci
    ) {
        if (definition.tier() != AugmentTier.MYTHIC || height < 150 || width < 75) return;
        y += (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
        int iconSize = compact ? 34 : 42;
        int iconY = y + (compact ? 34 : 38);
        int cx = x + width / 2;
        int cy = iconY + iconSize / 2;
        double phase = System.currentTimeMillis() / 360.0 + slot * 1.9;
        for (int i = 0; i < 8; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 8.0);
            int radius = iconSize / 2 + 5 + (i % 2) * 2;
            int px = cx + (int) Math.round(Math.cos(angle) * radius);
            int py = cy + (int) Math.round(Math.sin(angle) * radius);
            if (px <= x + 4 || px >= x + width - 6 || py <= y + 29 || py >= y + height - 9) continue;
            int size = i % 3 == 0 ? 3 : 2;
            int mote = i % 2 == 0 ? 0xFFFF8B80 : 0xFFFFD1B4;
            graphics.fill(px, py, px + size, py + size, mote);
        }
    }
}
