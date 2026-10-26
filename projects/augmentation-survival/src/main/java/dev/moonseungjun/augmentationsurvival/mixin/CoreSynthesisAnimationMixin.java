package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a short material-convergence -> flash -> result reveal to successful core synthesis. */
@Mixin(value = AugmentDeviceScreen.class, priority = 1350)
public abstract class CoreSynthesisAnimationMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704;
    private static final int H = 360;

    @Shadow private int synthesisTier;

    @Unique private long augmentationSurvival$synthesisStarted = Long.MIN_VALUE;
    @Unique private int augmentationSurvival$animatedTier;
    @Unique private int augmentationSurvival$animatedOutputs = 1;

    protected CoreSynthesisAnimationMixin(
            AugmentDeviceMenu menu,
            Inventory inventory,
            Component title,
            int imageWidth,
            int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    /**
     * A synthesis click is already gated by the screen's core-count check before send() is called.
     * Restarting here means every valid click restarts the feedback immediately, even when the
     * server status remains STATUS_UPGRADED across consecutive crafts.
     */
    @Inject(method = "send", at = @At("HEAD"))
    private void augmentationSurvival$restartOnSynthesisClick(
            int buttonId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        int oneStart = AugmentDeviceMenu.upgradeOneButtonId(AugmentTier.COMMON);
        int oneEnd = AugmentDeviceMenu.upgradeOneButtonId(AugmentTier.LEGENDARY);
        int allStart = AugmentDeviceMenu.upgradeAllButtonId(AugmentTier.COMMON);
        int allEnd = AugmentDeviceMenu.upgradeAllButtonId(AugmentTier.LEGENDARY);

        int tier = -1;
        if (buttonId >= oneStart && buttonId <= oneEnd) {
            tier = buttonId - oneStart;
        } else if (buttonId >= allStart && buttonId <= allEnd) {
            tier = buttonId - allStart;
        }
        if (tier < 0) return;

        augmentationSurvival$synthesisStarted = System.currentTimeMillis();
        augmentationSurvival$animatedTier = Math.max(0,
                Math.min(tier, AugmentTier.values().length - 2));
        augmentationSurvival$animatedOutputs = 1;
    }

    @Inject(method = "drawSynthesis", at = @At("TAIL"))
    private void augmentationSurvival$animateSynthesis(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        if (augmentationSurvival$synthesisStarted == Long.MIN_VALUE) return;

        // Once the server sync for this craft arrives, use its real output count. The animation
        // itself has already restarted on the click, so repeated STATUS_UPGRADED values are fine.
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED
                && menu.tierOrdinal() == augmentationSurvival$animatedTier) {
            augmentationSurvival$animatedOutputs = Math.max(1, menu.lastOutputCount());
        }

        long age = System.currentTimeMillis() - augmentationSurvival$synthesisStarted;
        if (age < 0 || age > 960L) {
            augmentationSurvival$synthesisStarted = Long.MIN_VALUE;
            return;
        }

        AugmentTier from = AugmentTier.values()[augmentationSurvival$animatedTier];
        AugmentTier to = from.next().orElse(AugmentTier.MYTHIC);
        int fromColor = augmentationSurvival$tierColor(from);
        int toColor = augmentationSurvival$tierColor(to);
        FlowGeometry flow = augmentationSurvival$flowGeometry();

        if (age <= 390L) {
            double p = augmentationSurvival$easeOut(Math.min(1.0, age / 390.0));
            int[] offsets = {-40, 0, 40};
            int[] yOffsets = {4, -7, 4};
            for (int i = 0; i < 3; i++) {
                int sx = flow.fromCenterX() + offsets[i];
                int sy = flow.centerY() + yOffsets[i];
                int x = augmentationSurvival$lerp(sx, flow.centerX(), p) - 8;
                int y = augmentationSurvival$lerp(sy, flow.centerY(), p) - 8;
                int aura = (int) Math.round(0x12 + p * 0x22);
                g.fill(x - 5, y - 5, x + 21, y + 21, augmentationSurvival$withAlpha(fromColor, aura));
                g.item(new ItemStack(AugmentCoreItems.item(from)), x, y);
            }
            int beamAlpha = (int) Math.round(0x18 + p * 0x58);
            g.fill(flow.fromCenterX(), flow.centerY() - 1,
                    flow.centerX(), flow.centerY() + 1,
                    augmentationSurvival$withAlpha(fromColor, beamAlpha));
            return;
        }

        if (age <= 560L) {
            double p = (age - 390L) / 170.0;
            int radius = 7 + (int) Math.round(p * 22.0);
            int outerAlpha = Math.max(0, (int) Math.round(0x88 * (1.0 - p)));
            int innerAlpha = Math.max(0, (int) Math.round(0xE8 * (1.0 - p * 0.65)));
            g.fill(flow.centerX() - radius, flow.centerY() - radius,
                    flow.centerX() + radius, flow.centerY() + radius,
                    augmentationSurvival$withAlpha(toColor, outerAlpha));
            g.fill(flow.centerX() - 5, flow.centerY() - 5,
                    flow.centerX() + 5, flow.centerY() + 5,
                    augmentationSurvival$withAlpha(0xFFFFFFFF, innerAlpha));
            return;
        }

        double p = augmentationSurvival$easeOut(Math.min(1.0, (age - 560L) / 400.0));
        int x = augmentationSurvival$lerp(flow.centerX(), flow.toCenterX(), p) - 8;
        int y = flow.centerY() - 8;
        double pulse = (Math.sin(age / 52.0) + 1.0) * 0.5;
        int halo = 0x1E + (int) Math.round(pulse * 0x18);
        int spread = 8 + (int) Math.round((1.0 - p) * 8.0);
        g.fill(x - spread, y - spread, x + 16 + spread, y + 16 + spread,
                augmentationSurvival$withAlpha(toColor, halo));
        g.fill(x - 4, y - 4, x + 20, y + 20,
                augmentationSurvival$withAlpha(toColor, 0x34));
        g.item(new ItemStack(AugmentCoreItems.item(to)), x, y);

        if (augmentationSurvival$animatedOutputs > 1 && p > 0.55) {
            String count = "×" + augmentationSurvival$animatedOutputs;
            g.text(font, count, x + 19, y + 5, toColor, false);
        }
    }

    @Unique
    private FlowGeometry augmentationSurvival$flowGeometry() {
        Bounds b = augmentationSurvival$bounds();
        int left = b.left() + 10;
        int right = b.right() - 10;
        int width = Math.max(40, right - left);
        int center = (left + right) / 2;
        int height = b.height();

        int headerY = b.top() + 30;
        boolean showGuide = height >= 260;
        boolean compactFlow = height < 210;
        int gridTop = headerY + (showGuide ? 31 : 17);
        int columns = width >= 520 ? 5 : compactFlow && width >= 108 ? 3 : width >= 270 ? 3 : 2;
        int rows = (5 + columns - 1) / columns;
        int gapY = height >= 260 ? 5 : 3;
        int cellH;
        int flowY;

        if (compactFlow) {
            int buttonH = 18;
            int buttonY = b.bottom() - 8 - buttonH;
            int countY = buttonY - 12;
            flowY = countY - 25;
            int gridBottom = flowY - 5;
            int availableGrid = Math.max(rows * 8, gridBottom - gridTop - gapY * Math.max(0, rows - 1));
            cellH = Math.max(8, Math.min(18, availableGrid / rows));
        } else {
            cellH = height >= 260 ? 28 : 20;
            int gridEnd = gridTop + rows * cellH + Math.max(0, rows - 1) * gapY;
            flowY = gridEnd + (height >= 260 ? 20 : 10);
            int flowLabelY = flowY + 48;
            int countY = flowLabelY + 15;
            int buttonH = 24;
            int buttonY = countY + 18;
            int allButtonY = buttonY;
            int maxBottom = b.bottom() - 8;
            if (allButtonY + buttonH > maxBottom) {
                int shift = allButtonY + buttonH - maxBottom;
                flowY -= shift;
            }
        }

        int size = compactFlow ? 26 : 42;
        int fromX = center - (compactFlow ? 56 : 78);
        int toX = center + (compactFlow ? 40 : 62);
        int centerY = flowY + size / 2;
        return new FlowGeometry(fromX + size / 2, center, toX + size / 2, centerY);
    }

    @Unique
    private Bounds augmentationSurvival$bounds() {
        int scaledW = minecraft == null ? W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? H : minecraft.getWindow().getGuiScaledHeight();
        int left = Math.max(8, -leftPos + 8);
        int right = Math.min(W - 8, scaledW - leftPos - 8);
        int top = Math.max(8, -topPos + 8);
        int bottom = Math.min(H - 8, scaledH - topPos - 8);
        if (right <= left) { left = 8; right = W - 8; }
        if (bottom <= top) { top = 8; bottom = H - 8; }
        return new Bounds(left, right, top, bottom);
    }

    @Unique private static double augmentationSurvival$easeOut(double p) {
        double inv = 1.0 - p;
        return 1.0 - inv * inv * inv;
    }
    @Unique private static int augmentationSurvival$lerp(int a, int b, double p) {
        return (int) Math.round(a + (b - a) * p);
    }
    @Unique private static int augmentationSurvival$withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
    @Unique private static int augmentationSurvival$tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFB8BEC5;
            case UNCOMMON -> 0xFF4FC77F;
            case RARE -> 0xFF4394E4;
            case EPIC -> 0xFFAA58D4;
            case LEGENDARY -> 0xFFE4A934;
            case MYTHIC -> 0xFFE44B4B;
        };
    }

    @Unique private record Bounds(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    @Unique private record FlowGeometry(int fromCenterX, int centerX, int toCenterX, int centerY) { }
}
