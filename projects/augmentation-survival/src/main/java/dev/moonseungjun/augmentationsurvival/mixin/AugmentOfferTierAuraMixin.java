package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extra tier VFX only. This deliberately renders before the existing offer-card mixin so card
 * geometry, text, icons and click targets stay untouched.
 */
@Mixin(value = AugmentDeviceScreen.class, priority = 2500)
public abstract class AugmentOfferTierAuraMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704;
    private static final int H = 360;
    private static final double CARD_W_TO_H = 0.615;

    protected AugmentOfferTierAuraMixin(
            AugmentDeviceMenu menu,
            Inventory inventory,
            Component title,
            int imageWidth,
            int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Inject(method = "drawOffer", at = @At("HEAD"))
    private void augmentationSurvival$drawTierAura(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        int ordinal = menu.tierOrdinal();
        if (ordinal < AugmentTier.LEGENDARY.ordinal()) return;

        AugmentTier tier = AugmentTier.values()[Math.min(ordinal, AugmentTier.values().length - 1)];
        Geometry geo = augmentationSurvival$geometry(tier);
        long now = System.currentTimeMillis();
        double pulse = (Math.sin(now / (tier == AugmentTier.MYTHIC ? 190.0 : 280.0)) + 1.0) * 0.5;

        for (int slot = 0; slot < 3; slot++) {
            int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
            int y = geo.cardTop();
            if (tier == AugmentTier.LEGENDARY) {
                augmentationSurvival$legendaryHalo(g, x, y, geo.cardW(), geo.cardH(), pulse, slot);
            } else if (tier == AugmentTier.MYTHIC) {
                augmentationSurvival$mythicBloodAura(g, x, y, geo.cardW(), geo.cardH(), pulse, slot, now);
            }
        }
    }

    @Unique
    private void augmentationSurvival$legendaryHalo(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            double pulse,
            int slot
    ) {
        int gold = 0xFFE5AA36;
        int pale = 0xFFFFEDAA;
        int warm = 0xFFFFC84C;
        int outer = 0x32 + (int) Math.round(pulse * 0x16);
        int mid = 0x54 + (int) Math.round(pulse * 0x18);

        // Keep the ordinary neon frame, then add a clearly readable halo outside it.
        augmentationSurvival$ring(g, x, y, w, h, 18, gold, outer, 3);
        augmentationSurvival$ring(g, x, y, w, h, 12, warm, mid, 3);
        augmentationSurvival$ring(g, x, y, w, h, 7, pale, 0x8C, 2);

        int cx = x + w / 2;
        int cy = y + h / 2;
        int ray = 0x72 + (int) Math.round(pulse * 0x20);
        int softRay = 0x46 + (int) Math.round(pulse * 0x18);

        // Long cardinal rays make the legendary tier visibly radiate instead of only changing
        // border colour. The rays stay narrow so neighbouring cards do not merge into one panel.
        g.fill(cx - 2, y - 34, cx + 3, y - 6, augmentationSurvival$alpha(pale, ray));
        g.fill(cx - 5, y - 27, cx + 6, y - 9, augmentationSurvival$alpha(warm, softRay));
        g.fill(cx - 2, y + h + 6, cx + 3, y + h + 28, augmentationSurvival$alpha(pale, ray - 0x10));

        g.fill(x - 27, cy - 2, x - 6, cy + 3, augmentationSurvival$alpha(warm, ray - 0x12));
        g.fill(x + w + 6, cy - 2, x + w + 27, cy + 3, augmentationSurvival$alpha(warm, ray - 0x12));

        // A crown-like secondary halo above and below the card.
        g.fill(x + 10, y - 12, x + w - 10, y - 8,
                augmentationSurvival$alpha(warm, 0x48 + (int) Math.round(pulse * 0x14)));
        g.fill(x + 24, y - 19, x + w - 24, y - 15,
                augmentationSurvival$alpha(pale, 0x3C + (int) Math.round(pulse * 0x10)));
        g.fill(x + 14, y + h + 8, x + w - 14, y + h + 12,
                augmentationSurvival$alpha(gold, 0x3E));

        int offset = slot == 1 ? 4 : 0;
        g.fill(x + 14 + offset, y - 25, x + 18 + offset, y - 12,
                augmentationSurvival$alpha(pale, 0x5C));
        g.fill(x + w - 20 - offset, y - 23, x + w - 16 - offset, y - 11,
                augmentationSurvival$alpha(pale, 0x54));
        g.fill(x + 9, y + h + 11, x + 13, y + h + 22,
                augmentationSurvival$alpha(gold, 0x42));
        g.fill(x + w - 14, y + h + 11, x + w - 10, y + h + 23,
                augmentationSurvival$alpha(gold, 0x42));
    }

    @Unique
    private void augmentationSurvival$mythicBloodAura(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            double pulse,
            int slot,
            long now
    ) {
        int bloodDark = 0xFF3A0008;
        int blood = 0xFF780012;
        int bloodHot = 0xFFB20825;
        int bloodCore = 0xFFF02A43;

        int outer = 0x34 + (int) Math.round(pulse * 0x18);
        int mid = 0x58 + (int) Math.round(pulse * 0x1C);
        augmentationSurvival$ring(g, x, y, w, h, 19, bloodDark, outer, 3);
        augmentationSurvival$ring(g, x, y, w, h, 13, blood, mid, 3);
        augmentationSurvival$ring(g, x, y, w, h, 7, bloodHot, 0x8C, 2);

        // Dense top/bottom bleed, restricted to short exterior bands so it reads as aura rather
        // than a rectangular backing plate.
        g.fill(x + 5, y - 12, x + w - 5, y - 7,
                augmentationSurvival$alpha(blood, 0x58 + (int) Math.round(pulse * 0x18)));
        g.fill(x + 14, y - 18, x + w - 14, y - 13,
                augmentationSurvival$alpha(bloodHot, 0x46 + (int) Math.round(pulse * 0x18)));
        g.fill(x + 8, y + h + 7, x + w - 8, y + h + 13,
                augmentationSurvival$alpha(bloodDark, 0x52 + (int) Math.round(pulse * 0x12)));

        // Large, irregular blood-flame plumes. Each card gets a different phase so three mythic
        // cards still look like separate objects instead of one red rectangle.
        double phase = slot * 1.41;
        int[] points = {8, 22, 37, 53, 68, 82, 93};
        for (int i = 0; i < points.length; i++) {
            double wave = (Math.sin(now / 130.0 + phase + i * 0.83) + 1.0) * 0.5;
            int px = x + Math.max(5, (w * points[i]) / 100);
            int plumeH = 18 + (int) Math.round(wave * 27);
            int outerW = 4 + (i % 3);
            int innerW = Math.max(2, outerW - 2);
            int outerAlpha = 0x56 + (int) Math.round(wave * 0x26);
            int innerAlpha = 0x78 + (int) Math.round(wave * 0x32);

            g.fill(px - outerW, y - 8 - plumeH, px + outerW, y - 5,
                    augmentationSurvival$alpha(i % 2 == 0 ? blood : bloodDark, outerAlpha));
            g.fill(px - innerW, y - 5 - (plumeH * 3) / 4, px + innerW, y - 4,
                    augmentationSurvival$alpha(i % 3 == 0 ? bloodCore : bloodHot, innerAlpha));
            if (plumeH >= 32) {
                g.fill(px - 1, y - 13 - plumeH, px + 2, y - 7 - plumeH,
                        augmentationSurvival$alpha(bloodCore, 0x70));
            }
        }

        // Side wisps and lower flare make the aura visibly extend beyond all four card edges.
        int sideWave = 12 + (int) Math.round(pulse * 10);
        g.fill(x - 11 - sideWave, y + h / 4, x - 6, y + h / 4 + 5,
                augmentationSurvival$alpha(bloodHot, 0x72));
        g.fill(x - 15 - sideWave / 2, y + h / 2, x - 7, y + h / 2 + 4,
                augmentationSurvival$alpha(blood, 0x60));
        g.fill(x + w + 6, y + (h * 2) / 3, x + w + 11 + sideWave, y + (h * 2) / 3 + 5,
                augmentationSurvival$alpha(bloodHot, 0x70));
        g.fill(x + w + 7, y + h / 3, x + w + 15 + sideWave / 2, y + h / 3 + 4,
                augmentationSurvival$alpha(blood, 0x5E));

        int lowerPulse = 0x5A + (int) Math.round(pulse * 0x1E);
        g.fill(x + w / 5, y + h + 14, x + (w * 2) / 5, y + h + 19,
                augmentationSurvival$alpha(bloodHot, lowerPulse));
        g.fill(x + (w * 3) / 5, y + h + 12, x + (w * 4) / 5, y + h + 17,
                augmentationSurvival$alpha(blood, lowerPulse));

        // Bright blood sparks around the silhouette.
        for (int i = 0; i < 7; i++) {
            double spark = (Math.sin(now / 105.0 + slot * 0.73 + i * 1.17) + 1.0) * 0.5;
            int sx = x + 5 + (i * Math.max(1, w - 10)) / 7;
            int sy = y + h + 9 + (int) Math.round(spark * 15);
            int size = i % 3 == 0 ? 3 : 2;
            g.fill(sx, sy, sx + size, sy + size,
                    augmentationSurvival$alpha(i % 2 == 0 ? bloodCore : bloodHot, 0xA0));
        }
    }

    @Unique
    private void augmentationSurvival$ring(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            int spread,
            int color,
            int alpha,
            int thickness
    ) {
        int c = augmentationSurvival$alpha(color, alpha);
        g.fill(x - spread, y - spread, x + w + spread, y - spread + thickness, c);
        g.fill(x - spread, y + h + spread - thickness, x + w + spread, y + h + spread, c);
        g.fill(x - spread, y - spread + thickness, x - spread + thickness, y + h + spread - thickness, c);
        g.fill(x + w + spread - thickness, y - spread + thickness, x + w + spread, y + h + spread - thickness, c);
    }

    @Unique
    private Geometry augmentationSurvival$geometry(AugmentTier tier) {
        Bounds b = augmentationSurvival$bounds();
        int usableW = b.width();
        int usableH = b.height();
        int auraPad = tier == AugmentTier.MYTHIC ? 14 : tier == AugmentTier.LEGENDARY ? 12 : 9;
        int outerPad = auraPad + 4;
        int gap = usableW >= 390 ? (tier.ordinal() >= AugmentTier.LEGENDARY.ordinal() ? 24 : 20)
                : usableW >= 300 ? 14 : 9;
        int headerBand = usableH >= 250 ? 44 : usableH >= 210 ? 40 : 34;
        int footerBand = usableH >= 250 ? 24 : usableH >= 210 ? 20 : 15;
        int availableH = Math.max(100, usableH - headerBand - footerBand);
        int idealH = Math.min(222, availableH);
        int idealW = (int) Math.round(idealH * CARD_W_TO_H);
        int widthBudget = Math.max(42, usableW - outerPad * 2 - gap * 2);
        int maxCardW = Math.max(42, widthBudget / 3);
        int cardW = Math.min(idealW, maxCardW);
        int cardH = Math.min(availableH, Math.max(100, (int) Math.round(cardW / CARD_W_TO_H)));
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = b.left() + Math.max(0, (usableW - groupW) / 2);
        int contentTop = b.top() + headerBand;
        int contentBottom = b.bottom() - footerBand;
        int cardTop = contentTop + Math.max(0, (contentBottom - contentTop - cardH) / 2);
        return new Geometry(cardLeft, cardTop, cardW, cardH, gap);
    }

    @Unique
    private Bounds augmentationSurvival$bounds() {
        int scaledW = minecraft == null ? W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? H : minecraft.getWindow().getGuiScaledHeight();
        int left = Math.max(8, -leftPos + 8);
        int right = Math.min(W - 8, scaledW - leftPos - 8);
        int top = Math.max(8, -topPos + 8);
        int bottom = Math.min(H - 8, scaledH - topPos - 8);
        if (right <= left) {
            left = 8;
            right = W - 8;
        }
        if (bottom <= top) {
            top = 8;
            bottom = H - 8;
        }
        return new Bounds(left, right, top, bottom);
    }

    @Unique
    private static int augmentationSurvival$alpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    @Unique
    private record Bounds(int left, int right, int top, int bottom) {
        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }
    }

    @Unique
    private record Geometry(int cardLeft, int cardTop, int cardW, int cardH, int gap) { }
}
