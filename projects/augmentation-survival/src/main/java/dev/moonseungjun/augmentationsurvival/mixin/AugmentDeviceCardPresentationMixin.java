package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Card presentation rescue driven by the actual playtest screenshot: preserve the working
 * server/menu flow, but replace the flat card skin and never ellipsize effect text.
 */
@Mixin(AugmentDeviceScreen.class)
public abstract class AugmentDeviceCardPresentationMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704;
    private static final int H = 360;
    private static final int SURFACE = 0xFF111619;
    private static final int SURFACE_HI = 0xFF192126;
    private static final int LINE_SOFT = 0xFF273137;
    private static final int TEXT = 0xFFF2F3F1;
    private static final int MUTED = 0xFFA2AAAD;
    private static final int FAINT = 0xFF667075;
    private static final int GOLD = 0xFFE2C679;
    private static final int OFFER_MAX_CARD_W = 196;
    private static final int OFFER_MAX_CARD_H = 238;

    protected AugmentDeviceCardPresentationMixin(
            AugmentDeviceMenu menu,
            Inventory inventory,
            Component title,
            int imageWidth,
            int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Shadow
    private double revealProgress(int slot) {
        throw new AssertionError();
    }

    @Shadow
    private boolean shiftDown() {
        throw new AssertionError();
    }

    @Inject(method = "drawAugmentCard", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$polishedCard(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            int slot,
            AugmentDefinition definition,
            int mouseX,
            int mouseY,
            boolean compact,
            CallbackInfo ci
    ) {
        ci.cancel();

        double reveal = revealProgress(slot);
        y += (int) Math.round((1.0 - reveal) * 10.0);
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean tiny = h < 132;
        int color = tierColor(definition.tier());
        AugmentPresentationCatalog.Entry presentation = AugmentPresentationCatalog.get(definition);

        drawFrame(g, x, y, w, h, definition.tier(), color, hover, selected);
        drawMaterial(g, x + 4, y + 4, w - 8, h - 8, definition.tier(), color, hover);

        g.text(font, presentation.kind().label(), x + 10, y + 11,
                presentation.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : GOLD, false);

        boolean rerollOwned = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
        if (!rerollOwned && selected && w >= 108) {
            String picked = "선택됨";
            g.text(font, picked, x + w - 10 - font.width(picked), y + 11, brighten(color, 42), false);
        }

        String control = controlHint(definition, presentation);
        int descriptionTop;
        if (compact) {
            int iconSize = tiny ? 22 : 28;
            int iconX = x + 10;
            int iconY = y + (tiny ? 31 : 34);
            drawSocket(g, iconX, iconY, iconSize, color, definition.tier(), hover);
            g.item(AugmentIconClient.stackFor(definition.id(), definition.tier()),
                    iconX + iconSize / 2 - 8, iconY + iconSize / 2 - 8);

            int textX = iconX + iconSize + 8;
            int textW = Math.max(24, x + w - 10 - textX);
            int nameY = iconY + (tiny ? 0 : 1);
            g.text(font, fit(presentation.displayName(), textW), textX, nameY, TEXT, false);

            int now = menu.currentLevel(slot);
            int next = Math.min(now + 1, definition.maxLevel());
            String level = now == 0
                    ? "신규 · Lv." + next + "/" + definition.maxLevel()
                    : "Lv." + now + " → " + next + "/" + definition.maxLevel();
            g.text(font, fit(level, textW), textX, nameY + 12, now == 0 ? color : MUTED, false);

            int dividerY;
            if (!control.isBlank() && !tiny) {
                g.text(font, fit(control, textW), textX, nameY + 24, shade(brighten(color, 18), 0.92), false);
                dividerY = iconY + iconSize + 8;
            } else {
                dividerY = iconY + iconSize + 7;
            }
            g.fill(x + 10, dividerY, x + w - 10, dividerY + 1, withAlpha(brighten(color, 12), 0x55));
            descriptionTop = dividerY + 6;
        } else {
            int iconSize = 42;
            int iconY = y + 37;
            int iconX = x + (w - iconSize) / 2;
            drawSocket(g, iconX, iconY, iconSize, color, definition.tier(), hover);
            g.item(AugmentIconClient.stackFor(definition.id(), definition.tier()),
                    iconX + iconSize / 2 - 8, iconY + iconSize / 2 - 8);

            int nameY = iconY + iconSize + 8;
            centered(g, fit(presentation.displayName(), Math.max(24, w - 20)), x + w / 2, nameY, TEXT);
            int now = menu.currentLevel(slot);
            int next = Math.min(now + 1, definition.maxLevel());
            String level = now == 0
                    ? "신규 · Lv." + next + "/" + definition.maxLevel()
                    : "Lv." + now + " → " + next + "/" + definition.maxLevel();
            centered(g, fit(level, Math.max(24, w - 20)), x + w / 2, nameY + 12, now == 0 ? color : MUTED);

            int dividerY = nameY + 28;
            if (!control.isBlank()) {
                centered(g, fit(control, Math.max(24, w - 20)), x + w / 2, nameY + 23,
                        shade(brighten(color, 18), 0.92));
                dividerY = nameY + 36;
            }
            g.fill(x + 11, dividerY, x + w - 11, dividerY + 1, withAlpha(brighten(color, 12), 0x55));
            descriptionTop = dividerY + 7;
        }

        int actionY = y + h - 22;
        int descriptionBottom = rerollOwned ? actionY - 5 : y + h - 8;
        drawAllLines(g, presentation.summary(), x + 10, descriptionTop, Math.max(24, w - 20),
                Math.max(8, descriptionBottom - descriptionTop), 0xFFD6DCDD);

        if (rerollOwned) {
            boolean buttonHover = inside(mouseX, mouseY, x + 8, actionY, w - 16, 15);
            boolean canReroll = menu.rerollsRemaining() > 0 && !selected;
            int buttonLine = canReroll ? (buttonHover ? brighten(color, 24) : shade(color, 0.72)) : LINE_SOFT;
            int buttonFill = canReroll && buttonHover ? tint(SURFACE_HI, color, 18) : 0xE810161B;
            g.fill(x + 8, actionY, x + w - 8, actionY + 15, buttonFill);
            g.outline(x + 8, actionY, w - 16, 15, buttonLine);
            String label = selected ? "선택 해제" : menu.rerollsRemaining() > 0 ? "↻ 재추첨" : "재추첨 소진";
            centered(g, fit(label, w - 24), x + w / 2, actionY + 3,
                    canReroll ? (buttonHover ? TEXT : color) : FAINT);
        }
    }

    @Inject(method = "drawOffer", at = @At("TAIL"))
    private void augmentationSurvival$fullShiftDetail(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        if (!shiftDown()) return;
        OfferGeometry geometry = offerGeometry();
        AugmentDefinition hovered = null;
        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition candidate = menu.candidate(slot);
            if (candidate == null) continue;
            int x = geometry.cardLeft() + slot * (geometry.cardWidth() + geometry.gap());
            int y = geometry.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
            if (inside(mouseX, mouseY, x, y, geometry.cardWidth(), geometry.cardHeight())) {
                hovered = candidate;
                break;
            }
        }
        if (hovered == null) return;

        AugmentPresentationCatalog.Entry presentation = AugmentPresentationCatalog.get(hovered);
        int color = tierColor(hovered.tier());
        int panelW = Math.min(560, Math.max(150, geometry.visibleRight() - geometry.visibleLeft() - 24));
        int panelX = geometry.centerX() - panelW / 2;
        List<String> lines = wrap(presentation.detail(), Math.max(40, panelW - 24));
        int lineHeight = 10;
        int panelH = 34 + lines.size() * lineHeight;
        int maxH = Math.max(70, geometry.visibleBottom() - geometry.visibleTop() - 54);
        if (panelH > maxH) {
            lineHeight = 9;
            panelH = 34 + lines.size() * lineHeight;
        }
        int panelY = Math.max(geometry.visibleTop() + 34, geometry.visibleBottom() - panelH - 16);

        g.fill(panelX - 4, panelY - 4, panelX + panelW + 4, panelY + panelH + 4, 0xC8000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF50B1013);
        g.outline(panelX, panelY, panelW, panelH, shade(color, 0.88));
        g.fill(panelX, panelY, panelX + 4, panelY + panelH, color);
        g.fill(panelX + 4, panelY, panelX + panelW, panelY + 2, withAlpha(brighten(color, 30), 0xCC));

        g.text(font, presentation.displayName(), panelX + 12, panelY + 8, TEXT, false);
        String meta = tierLabel(hovered.tier()) + " · 정확한 수치";
        g.text(font, meta, panelX + panelW - 12 - font.width(meta), panelY + 8, color, false);
        g.fill(panelX + 12, panelY + 23, panelX + panelW - 12, panelY + 24, LINE_SOFT);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, lines.get(i), panelX + 12, panelY + 29 + i * lineHeight, 0xFFE3E7E7, false);
        }
    }

    private OfferGeometry offerGeometry() {
        int scaledW = minecraft == null ? W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? H : minecraft.getWindow().getGuiScaledHeight();
        int visibleLeft = Math.max(8, -leftPos + 8);
        int visibleRight = Math.min(W - 8, scaledW - leftPos - 8);
        int visibleTop = Math.max(8, -topPos + 8);
        int visibleBottom = Math.min(H - 8, scaledH - topPos - 8);
        if (visibleRight <= visibleLeft) {
            visibleLeft = 8;
            visibleRight = W - 8;
        }
        if (visibleBottom <= visibleTop) {
            visibleTop = 8;
            visibleBottom = H - 8;
        }

        int usableW = visibleRight - visibleLeft;
        int usableH = visibleBottom - visibleTop;
        int gap = usableW >= 620 ? 14 : usableW >= 480 ? 10 : 6;
        int rawCardW = Math.max(1, (usableW - gap * 2) / 3);
        int cardW = Math.min(OFFER_MAX_CARD_W, rawCardW);
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = visibleLeft + Math.max(0, (usableW - groupW) / 2);
        boolean compact = cardW < 145 || usableH < 270;
        int headerTop = visibleTop + 5;
        int cardTop = headerTop + (compact ? 33 : 43);
        int footerY = visibleBottom - 11;
        int cardH = Math.min(OFFER_MAX_CARD_H, Math.max(88, footerY - cardTop - 3));
        return new OfferGeometry(visibleLeft, visibleRight, visibleTop, visibleBottom,
                cardLeft, cardTop, cardW, cardH, gap);
    }

    private String controlHint(AugmentDefinition definition, AugmentPresentationCatalog.Entry presentation) {
        String id = definition.id();
        if (id.endsWith(":pockets") || id.endsWith(":remote_storage")) return "B · 보관함";
        if (id.endsWith(":spatial_link")) return "G · 공간 연결";
        if (id.endsWith(":authority_theft")) return "J · 권능 해방";
        if (presentation.kind() != AugmentPresentationCatalog.Kind.PASSIVE) return "K 편성 · Alt 모드 · 1~0";
        return "";
    }

    private void drawFrame(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            AugmentTier tier,
            int color,
            boolean hover,
            boolean selected
    ) {
        boolean roomy = w >= 120 && h >= 132;
        g.fill(x - 3, y - 3, x + w + 3, y + h + 4, 0x48000000);

        if (tier == AugmentTier.EPIC && roomy) {
            g.fill(x - 3, y - 3, x + w + 3, y + h + 3, withAlpha(color, 0x12));
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(color, 0x78));
        } else if (tier == AugmentTier.LEGENDARY && roomy) {
            g.fill(x - 6, y - 6, x + w + 6, y + h + 6, withAlpha(color, 0x20));
            g.outline(x - 5, y - 5, w + 10, h + 10, withAlpha(color, 0x42));
            g.outline(x - 3, y - 3, w + 6, h + 6, withAlpha(brighten(color, 28), 0xA8));
            g.fill(x + 12, y - 3, x + w - 12, y - 1, withAlpha(brighten(color, 50), 0xC0));
        } else if (tier == AugmentTier.MYTHIC && roomy) {
            double wave = (Math.sin(System.currentTimeMillis() / 210.0) + 1.0) * 0.5;
            int outerAlpha = 0x32 + (int) Math.round(wave * 0x20);
            int midAlpha = 0x68 + (int) Math.round(wave * 0x24);
            g.fill(x - 7, y - 7, x + w + 7, y + h + 7, withAlpha(color, 0x26));
            g.outline(x - 6, y - 6, w + 12, h + 12, withAlpha(color, outerAlpha));
            g.outline(x - 4, y - 4, w + 8, h + 8, withAlpha(brighten(color, 14), midAlpha));
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(brighten(color, 38), 0xD0));
            g.fill(x + 10, y - 4, x + w - 10, y - 1, withAlpha(brighten(color, 55), 0xD0));
            g.fill(x + 10, y + h + 1, x + w - 10, y + h + 4, withAlpha(color, 0x90));
        } else if (tier.ordinal() >= AugmentTier.EPIC.ordinal()) {
            g.outline(x - 1, y - 1, w + 2, h + 2, withAlpha(color, 0x90));
        }

        g.fill(x, y, x + w, y + h, 0xFF0A0F12);
        int outer = selected
                ? brighten(color, 58)
                : hover ? brighten(color, 20)
                : tier.ordinal() >= AugmentTier.EPIC.ordinal() ? shade(color, 0.74) : 0xFF596268;
        g.outline(x, y, w, h, outer);
        g.outline(x + 2, y + 2, w - 4, h - 4,
                tier.ordinal() >= AugmentTier.EPIC.ordinal() ? withAlpha(color, 0x98) : LINE_SOFT);
        if (selected) g.fill(x + 7, y + h - 8, x + w - 7, y + h - 5, brighten(color, 28));
    }

    private void drawMaterial(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            AugmentTier tier,
            int color,
            boolean hover
    ) {
        g.fill(x, y, x + w, y + h, 0xFF080C0F);
        int tintAmount = switch (tier) {
            case COMMON -> 3;
            case UNCOMMON -> 5;
            case RARE -> 7;
            case EPIC -> 10;
            case LEGENDARY -> 13;
            case MYTHIC -> 15;
        };
        int upper = y + Math.max(24, h * 43 / 100);
        g.fill(x + 1, y + 1, x + w - 1, upper, tint(0xFF0B1014, color, tintAmount));
        g.fill(x + 1, upper, x + w - 1, y + h - 1, 0xFF080C0F);
        g.fill(x + 3, y + 3, x + w - 3, y + 4,
                withAlpha(brighten(color, 44), hover ? 0x28 : 0x18));
        g.fill(x + 3, y + 5, x + w - 3, y + 6, withAlpha(0xFFFFFFFF, hover ? 0x16 : 0x0D));

        int facet = withAlpha(brighten(color, 58), hover ? 0x1C : 0x10);
        int facet2 = withAlpha(0xFFFFFFFF, hover ? 0x12 : 0x08);
        for (int i = 0; i < 6; i++) {
            int yy = y + 18 + i * 4;
            if (yy >= y + h - 24) break;
            int xx = x + 8 + i * 5;
            int end = Math.min(x + w - 8, xx + Math.max(16, w / 4));
            g.fill(xx, yy, end, yy + 1, (i & 1) == 0 ? facet : facet2);
        }

        int lowerBand = y + h - Math.max(18, h / 7);
        g.fill(x + 1, lowerBand, x + w - 1, y + h - 1, withAlpha(color, 0x0D));
        g.fill(x + 8, lowerBand, x + w - 8, lowerBand + 1, withAlpha(color, 0x30));
        g.outline(x + 2, y + 2, w - 4, h - 4,
                tier.ordinal() >= AugmentTier.EPIC.ordinal()
                        ? withAlpha(brighten(color, 12), 0x78)
                        : 0xFF30383D);

        int corner = withAlpha(brighten(color, 35), hover ? 0xB0 : 0x78);
        g.fill(x + 5, y + 5, x + 16, y + 6, corner);
        g.fill(x + 5, y + 5, x + 6, y + 13, corner);
        g.fill(x + w - 16, y + 5, x + w - 5, y + 6, corner);
        g.fill(x + w - 6, y + 5, x + w - 5, y + 13, corner);
    }

    private void drawSocket(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int size,
            int color,
            AugmentTier tier,
            boolean hover
    ) {
        if (tier == AugmentTier.LEGENDARY) {
            g.fill(x - 4, y - 4, x + size + 4, y + size + 4, withAlpha(color, 0x20));
            g.outline(x - 3, y - 3, size + 6, size + 6, withAlpha(brighten(color, 24), 0x84));
        } else if (tier == AugmentTier.MYTHIC) {
            double wave = (Math.sin(System.currentTimeMillis() / 220.0) + 1.0) * 0.5;
            int alpha = 0x24 + (int) Math.round(wave * 0x18);
            g.fill(x - 5, y - 5, x + size + 5, y + size + 5, withAlpha(color, alpha));
            g.outline(x - 4, y - 4, size + 8, size + 8, withAlpha(brighten(color, 28), 0xA0));
        } else if (tier == AugmentTier.EPIC) {
            g.outline(x - 2, y - 2, size + 4, size + 4, withAlpha(color, 0x80));
        }

        g.fill(x, y, x + size, y + size,
                tint(0xFF090D10, color, tier.ordinal() >= AugmentTier.EPIC.ordinal() ? 12 : 5));
        g.outline(x, y, size, size, hover ? brighten(color, 28) : shade(color, 0.82));
        g.outline(x + 3, y + 3, size - 6, size - 6, withAlpha(brighten(color, 20), 0x88));
        g.fill(x + 4, y + 4, x + size - 4, y + 5, withAlpha(0xFFFFFFFF, 0x24));
        g.fill(x + 5, y + size - 6, x + size - 5, y + size - 5, withAlpha(color, 0x42));
    }

    private void drawAllLines(
            GuiGraphicsExtractor g,
            String value,
            int x,
            int y,
            int maxWidth,
            int availableHeight,
            int color
    ) {
        List<String> lines = wrap(value, maxWidth);
        if (lines.isEmpty()) return;
        int lineHeight = Math.min(10, Math.max(8, availableHeight / Math.max(1, lines.size())));
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, lines.get(i), x, y + i * lineHeight, color, false);
        }
    }

    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (font.width(word) > maxWidth) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                StringBuilder part = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int codePoint = word.codePointAt(offset);
                    String next = part.toString() + new String(Character.toChars(codePoint));
                    if (part.length() > 0 && font.width(next) > maxWidth) {
                        lines.add(part.toString());
                        part.setLength(0);
                    }
                    part.appendCodePoint(codePoint);
                    offset += Character.charCount(codePoint);
                }
                line.append(part);
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private String fit(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            int end = result.offsetByCodePoints(result.length(), -1);
            result = result.substring(0, end);
        }
        return result.isEmpty() ? "…" : result + "…";
    }

    private void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x - font.width(value) / 2, y, color, false);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static int tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFB8BEC5;
            case UNCOMMON -> 0xFF4FC77F;
            case RARE -> 0xFF4394E4;
            case EPIC -> 0xFFAA58D4;
            case LEGENDARY -> 0xFFE4A934;
            case MYTHIC -> 0xFFE44B4B;
        };
    }

    private static String tierLabel(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> "일반";
            case UNCOMMON -> "고급";
            case RARE -> "희귀";
            case EPIC -> "영웅";
            case LEGENDARY -> "전설";
            case MYTHIC -> "신화";
        };
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount);
        int g = Math.min(255, ((color >> 8) & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor);
        int g = (int) (((color >> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int tint(int base, int color, int percent) {
        int inv = 100 - percent;
        int r = (((base >> 16) & 255) * inv + ((color >> 16) & 255) * percent) / 100;
        int g = (((base >> 8) & 255) * inv + ((color >> 8) & 255) * percent) / 100;
        int b = ((base & 255) * inv + (color & 255) * percent) / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private record OfferGeometry(
            int visibleLeft,
            int visibleRight,
            int visibleTop,
            int visibleBottom,
            int cardLeft,
            int cardTop,
            int cardWidth,
            int cardHeight,
            int gap
    ) {
        int centerX() {
            return (visibleLeft + visibleRight) / 2;
        }
    }
}
