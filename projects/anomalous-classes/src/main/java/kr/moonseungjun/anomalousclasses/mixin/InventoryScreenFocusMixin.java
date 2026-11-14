package kr.moonseungjun.anomalousclasses.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the vanilla 18-pixel slot surround behind the existing 16-pixel menu slot. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenFocusMixin {
    @Unique private static final ResourceLocation anomalousclasses$emptyMarker =
            ResourceLocation.fromNamespaceAndPath("anomalousclasses", "textures/gui/sprites/focus_slot/empty.png");

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void anomalousclasses$drawFocusSlotFrame(GuiGraphics graphics, float partialTick,
                                                      int mouseX, int mouseY, CallbackInfo ci) {
        // InventoryMenuFocusMixin uses slot index 46 at (77, 35). Vanilla slot
        // frames are 18x18 around the 16x16 click/item area.
        graphics.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"),
                leftPos + 76, topPos + 34, 18, 18);
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (screen.getMenu().slots.size() > 46) {
            Slot focusSlot = screen.getMenu().slots.get(46);
            if (!focusSlot.hasItem()) {
                graphics.blit(anomalousclasses$emptyMarker, leftPos + 77, topPos + 35,
                        0, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
    }
}
