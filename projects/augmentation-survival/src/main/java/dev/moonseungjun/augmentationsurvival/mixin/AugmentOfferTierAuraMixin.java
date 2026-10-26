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
        double pulse = (Math.sin(now / (tier == AugmentTier.MYTHIC ? 210.0 : 310.0)) + 1.0) * 0.5;

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
        int pale = 0xFFFFE9A2;
        int warm = 0xFFFFC957;
        int outer = 0x0D + (int) Math.round(pulse * 0x09);
        int mid = 0x18 + (int) Math.round(pulse * 0x0C);

        augmentationSurvival$ring(g, x, y, w, h, 20, gold, outer, 2);
        augmentationSurvival$ring(g, x, y, w, h, 14, warm, mid, 2);
        augmentationSurvival$ring(g, x, y, w, h, 8, pale, 0x28, 2);

        int cx = x + w / 2;
        int cy = y + h / 2;
        int rayAlpha = 0x1C + (int) Math.round(pulse * 0x10);

        // Large readable halo rays behind the card, on top of the normal subtle glow.
        g.fill(cx - 1, y - 27, cx + 2, y - 6, augmentationSurvival$alpha(pale, rayAlpha + 0x12));
        g.fill(cx - 3, y - 20, cx + 4, y - 8, augmentationSurvival$alpha(warm, rayAlpha));
        g.fill(cx - 1, y + h + 6, cx + 2, y + h + 24, augmentationSurvival$alpha(pale, rayAlpha));

        g.fill(x - 22, cy - 1, x - 6, cy + 2, augmentationSurvival$alpha(warm, rayAlpha));
        g.fill(x + w + 6, cy - 1, x + w + 22, cy + 2, augmentationSurvival$alpha(warm, rayAlpha));

        int offset = slot == 1 ? 3 : 0;
        g.fill(x + 16 + offset, y - 17, x + 18 + offset, y - 7,
                augmentationSurvival$alpha(pale, 0x20));
        g.fill(x + w - 19 - offset, y - 15, x + w - 17 - offset, y - 7,
                augmentationSurvival$alpha(pale, 0x1D));
        g.fill(x + 12, y + h + 7, x + 14, y + h + 15,
                augmentationSurvival$alpha(gold, 0x18));
        g.fill(x + w - 15, y + h + 7, x + w - 13, y + h + 17,
                augmentationSurvival$alpha(gold, 0x18));
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
        int bloodDark = 0xFF52000D;
        int blood = 0xFF8F0018;
        int bloodHot = 0xFFC1122D;
        int bloodCore = 0xFFE12A3D;

        int outer = 0x14 + (int) Math.round(pulse * 0x0D);
        int mid = 0x20 + (int) Math.round(pulse * 0x10);
        augmentationSurvival$ring(g, x, y, w, h, 24, bloodDark, outer, 3);
        augmentationSurvival$ring(g, x, y, w, h, 17, blood, mid, 3);
        augmentationSurvival$ring(g, x, y, w, h, 10, bloodHot, 0x32, 2);

        // Broad dark-red wash that reads as an aura rather than another frame.
        g.fill(x - 10, y - 13, x + w + 10, y - 7,
                augmentationSurvival$alpha(bloodDark, 0x22 + (int) Math.round(pulse * 0x10)));
        g.fill(x - 12, y + h + 7, x + w + 12, y + h + 14,
                augmentationSurvival$alpha(bloodDark, 0x20 + (int) Math.round(pulse * 0x0C)));

        // Uneven blood-flame plumes. Different phase per card keeps three mythic cards from
        // becoming one rectangular red slab.
        double phase = slot * 1.37;
        int[] points = {12, 29, 47, 68, 84};
        for (int i = 0; i < points.length; i++) {
            double wave = (Math.sin(now / 145.0 + phase + i * 0.91) + 1.0) * 0.5;
            int px = x + Math.max(6, (w * points[i]) / 100);
            int plumeH = 9 + (int) Math.round(wave * 17);
            int plumeW = i % 2 == 0 ? 3 : 2;
            int plumeColor = i % 3 == 0 ? bloodCore : i % 3 == 1 ? bloodHot : blood;
            int alpha = 0x28 + (int) Math.round(wave * 0x20);
            g.fill(px - plumeW, y - 7 - plumeH, px + plumeW, y - 6,
                    augmentationSurvival$alpha(plumeColor, alpha));
            if (plumeH >= 18) {
                g.fill(px - 1, y - 12 - plumeH, px + 1, y - 8 - plumeH,
                        augmentationSurvival$alpha(bloodCore, 0x26));
            }
        }

        int sideWave = 7 + (int) Math.round(pulse * 8);
        g.fill(x - 13 - sideWave, y + h / 4, x - 7, y + h / 4 + 3,
                augmentationSurvival$alpha(bloodHot, 0x32));
        g.fill(x + w + 7, y + (h * 2) / 3, x + w + 13 + sideWave, y + (h * 2) / 3 + 3,
                augmentationSurvival$alpha(bloodHot, 0x30));

        // Low embers / blood sparks around the silhouette.
        for (int i = 0; i < 4; i++) {
            double spark = (Math.sin(now / 120.0 + slot * 0.7 + i * 1.5) + 1.0) * 0.5;
            int sx = x + 8 + (i * Math.max(1, w - 16)) / 4;
            int sy = y + h + 8 + (int) Math.round(spark * 10);
            g.fill(sx, sy, sx + 2, sy + 2,
                    augmentationSurvival$alpha(i % 2 == 0 ? bloodCore : bloodHot, 0x55));
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
