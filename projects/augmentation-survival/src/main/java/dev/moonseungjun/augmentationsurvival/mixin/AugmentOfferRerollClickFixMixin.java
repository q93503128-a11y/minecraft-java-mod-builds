package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Canonical owner of offer-screen left-click input.
 *
 * <p>The offer screen has several presentation mixins. They may still draw the cards, but this
 * early injector owns the actual READY-state click so one physical click can never fall through to
 * both the reroll strip and a card-selection handler.</p>
 */
@Mixin(value = AugmentDeviceScreen.class, priority = 10000)
public abstract class AugmentOfferRerollClickFixMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    @Unique private static final int AUGMENTATION_SURVIVAL$W = 704;
    @Unique private static final int AUGMENTATION_SURVIVAL$H = 360;
    @Unique private static final double AUGMENTATION_SURVIVAL$CARD_W_TO_H = 0.615D;

    @Shadow private double revealProgress(int slot) { throw new AssertionError(); }

    protected AugmentOfferRerollClickFixMixin(
            AugmentDeviceMenu menu, Inventory inventory, Component title, int imageWidth, int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, order = 0)
    private void augmentationSurvival$ownOfferClick(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (menu.status() != AugmentDeviceMenu.STATUS_READY || event.button() != 0) return;

        double mx = event.x() - leftPos;
        double my = event.y() - topPos;
        Geometry geo = augmentationSurvival$geometry();
        boolean rerollVisible = menu.rerollsRemaining() > 0
                || ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;

        for (int slot = 0; slot < 3; slot++) {
            if (menu.candidate(slot) == null) continue;
            int x = geo.cardLeft + slot * (geo.cardW + geo.gap);
            int y = geo.cardTop + (int) Math.round((1.0D - revealProgress(slot)) * 9.0D);
            int buttonY = y + geo.cardH - 18;

            // Own the complete visual reroll strip, including a small border allowance. The strip
            // is checked before the card body and is never allowed to fall through to selection.
            if (rerollVisible
                    && augmentationSurvival$inside(mx, my, x + 6, buttonY - 3, geo.cardW - 12, 19)) {
                boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
                if (!selected && menu.rerollsRemaining() > 0) {
                    augmentationSurvival$send(AugmentDeviceMenu.rerollButtonId(slot));
                }
                cir.setReturnValue(true);
                return;
            }

            if (augmentationSurvival$inside(mx, my, x, y, geo.cardW, geo.cardH)) {
                augmentationSurvival$send(AugmentDeviceMenu.candidateButtonId(slot));
                cir.setReturnValue(true);
                return;
            }
        }

        // READY-state clicks outside the three canonical card regions are intentionally consumed.
        // This prevents lower-priority legacy presentation handlers from interpreting a different
        // geometry for the same physical click.
        cir.setReturnValue(true);
    }

    @Unique
    private Geometry augmentationSurvival$geometry() {
        Bounds b = augmentationSurvival$bounds();
        int usableW = b.right - b.left;
        int usableH = b.bottom - b.top;
        int tierOrdinal = Math.max(0, menu.tierOrdinal());
        int auraPad = tierOrdinal >= AugmentTier.MYTHIC.ordinal() ? 14
                : tierOrdinal >= AugmentTier.LEGENDARY.ordinal() ? 12
                : tierOrdinal >= AugmentTier.EPIC.ordinal() ? 9 : 6;
        int outerPad = auraPad + 4;
        int gap = usableW >= 390 ? (tierOrdinal >= AugmentTier.LEGENDARY.ordinal() ? 24 : 20)
                : usableW >= 300 ? 14 : 9;
        int headerBand = usableH >= 250 ? 44 : usableH >= 210 ? 40 : 34;
        int footerBand = usableH >= 250 ? 24 : usableH >= 210 ? 20 : 15;
        int availableH = Math.max(100, usableH - headerBand - footerBand);
        int idealH = Math.min(222, availableH);
        int idealW = (int) Math.round(idealH * AUGMENTATION_SURVIVAL$CARD_W_TO_H);
        int widthBudget = Math.max(42, usableW - outerPad * 2 - gap * 2);
        int cardW = Math.min(idealW, Math.max(42, widthBudget / 3));
        int cardH = Math.min(
                availableH,
                Math.max(100, (int) Math.round(cardW / AUGMENTATION_SURVIVAL$CARD_W_TO_H))
        );
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = b.left + Math.max(0, (usableW - groupW) / 2);
        int contentTop = b.top + headerBand;
        int contentBottom = b.bottom - footerBand;
        int cardTop = contentTop + Math.max(0, (contentBottom - contentTop - cardH) / 2);
        return new Geometry(cardLeft, cardTop, cardW, cardH, gap);
    }

    @Unique
    private Bounds augmentationSurvival$bounds() {
        int scaledW = minecraft == null ? AUGMENTATION_SURVIVAL$W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? AUGMENTATION_SURVIVAL$H : minecraft.getWindow().getGuiScaledHeight();
        int left = Math.max(8, -leftPos + 8);
        int right = Math.min(AUGMENTATION_SURVIVAL$W - 8, scaledW - leftPos - 8);
        int top = Math.max(8, -topPos + 8);
        int bottom = Math.min(AUGMENTATION_SURVIVAL$H - 8, scaledH - topPos - 8);
        if (right <= left) { left = 8; right = AUGMENTATION_SURVIVAL$W - 8; }
        if (bottom <= top) { top = 8; bottom = AUGMENTATION_SURVIVAL$H - 8; }
        return new Bounds(left, right, top, bottom);
    }

    @Unique
    private boolean augmentationSurvival$send(int button) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, button)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        return true;
    }

    @Unique
    private static boolean augmentationSurvival$inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Unique private record Bounds(int left, int right, int top, int bottom) { }
    @Unique private record Geometry(int cardLeft, int cardTop, int cardW, int cardH, int gap) { }
}
