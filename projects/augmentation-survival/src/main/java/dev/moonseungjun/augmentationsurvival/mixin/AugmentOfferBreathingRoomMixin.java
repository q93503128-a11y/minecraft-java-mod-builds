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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Offer-only playtest pass.
 * Keeps the three cards at a stable ARAM-like portrait ratio, reserves air for glow,
 * and makes render/click geometry identical. Server offer state remains untouched.
 */
@Mixin(value = AugmentDeviceScreen.class, priority = 2000)
public abstract class AugmentOfferBreathingRoomMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704;
    private static final int H = 360;
    private static final double CARD_W_TO_H = 0.615;
    private static final int TEXT = 0xFFF2F3F1;
    private static final int MUTED = 0xFFA2AAAD;
    private static final int FAINT = 0xFF667075;
    private static final int LINE_SOFT = 0xFF273137;
    private static final int DEEP = 0xFF080C0F;
    private static final int GOLD = 0xFFE2C679;

    @Shadow private double revealProgress(int slot) { throw new AssertionError(); }
    @Shadow private boolean shiftDown() { throw new AssertionError(); }

    protected AugmentOfferBreathingRoomMixin(
            AugmentDeviceMenu menu,
            Inventory inventory,
            Component title,
            int imageWidth,
            int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Inject(method = "drawOffer", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$drawAiryOffer(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        ci.cancel();
        Geometry geo = geometry();
        AugmentTier tier = menu.tierOrdinal() >= 0
                ? AugmentTier.values()[menu.tierOrdinal()]
                : AugmentTier.COMMON;
        int color = tierColor(tier);

        centered(g, "증강 선택", geo.centerX(), geo.titleY(), TEXT);
        String rule = tierLabel(tier) + " 코어 · 3개 중 " + menu.selectionCount() + "개 선택";
        centered(g, fit(rule, geo.bounds().width() - 32), geo.centerX(), geo.ruleY(), color);

        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
            int y = geo.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 9.0);
            drawCard(g, x, y, geo.cardW(), geo.cardH(), slot, d, mouseX, mouseY);
        }

        String footer = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0
                ? "카드 선택 · Shift: 정확한 수치 · 재추첨 " + menu.rerollsRemaining() + "회"
                : "카드 선택 · Shift: 정확한 수치";
        centered(g, fit(footer, geo.bounds().width() - 36), geo.centerX(), geo.footerY(), MUTED);

        if (shiftDown()) {
            for (int slot = 0; slot < 3; slot++) {
                AugmentDefinition d = menu.candidate(slot);
                if (d == null) continue;
                int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
                int y = geo.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 9.0);
                if (inside(mouseX, mouseY, x, y, geo.cardW(), geo.cardH())) {
                    drawDetail(g, d, mouseX, mouseY, geo.bounds());
                    break;
                }
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$clickAiryOffer(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (menu.status() != AugmentDeviceMenu.STATUS_READY || event.button() != 0) return;
        double mx = event.x() - leftPos;
        double my = event.y() - topPos;
        Geometry geo = geometry();
        boolean reroll = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;

        for (int slot = 0; slot < 3; slot++) {
            if (menu.candidate(slot) == null) continue;
            int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
            int y = geo.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 9.0);
            if (reroll) {
                int buttonY = y + geo.cardH() - 18;
                if (inside(mx, my, x + 8, buttonY, geo.cardW() - 16, 13)) {
                    boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
                    if (selected) send(AugmentDeviceMenu.candidateButtonId(slot));
                    else if (menu.rerollsRemaining() > 0) send(AugmentDeviceMenu.rerollButtonId(slot));
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (inside(mx, my, x, y, geo.cardW(), geo.cardH())) {
                send(AugmentDeviceMenu.candidateButtonId(slot));
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(true);
    }

    @Unique
    private void drawCard(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            int slot,
            AugmentDefinition d,
            int mouseX,
            int mouseY
    ) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int color = tierColor(d.tier());
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;

        drawAura(g, x, y, w, h, d.tier(), color, hover, selected);
        drawMaterial(g, x, y, w, h, d.tier(), color, hover);

        g.text(font, p.kind().label(), x + 9, y + 9,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : GOLD, false);
        String key = controlBadge(d, p);
        if (!key.isBlank() && w >= 84) {
            int bw = font.width(key) + 10;
            int bx = x + w - bw - 8;
            g.fill(bx, y + 6, bx + bw, y + 20, withAlpha(color, 0x16));
            g.outline(bx, y + 6, bw, 14, withAlpha(brighten(color, 18), 0x72));
            centered(g, key, bx + bw / 2, y + 9, brighten(color, 34));
        }

        List<String> bodyLines = wrap(p.summary(), Math.max(36, w - 18));
        boolean dense = bodyLines.size() >= 8 || h < 184;
        int iconSize = dense ? 26 : 32;
        int iconY = y + (dense ? 26 : 29);
        int iconX = x + (w - iconSize) / 2;
        drawSocket(g, iconX, iconY, iconSize, d.tier(), color, hover);
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()),
                iconX + iconSize / 2 - 8, iconY + iconSize / 2 - 8);

        int nameY = iconY + iconSize + (dense ? 4 : 6);
        centered(g, fit(p.displayName(), w - 16), x + w / 2, nameY, TEXT);
        int now = menu.currentLevel(slot);
        int next = Math.min(now + 1, d.maxLevel());
        String level = now == 0
                ? "신규 · Lv." + next + "/" + d.maxLevel()
                : "Lv." + now + " → " + next + "/" + d.maxLevel();
        centered(g, fit(level, w - 16), x + w / 2, nameY + 11, now == 0 ? color : MUTED);

        int dividerY = nameY + (dense ? 23 : 25);
        g.fill(x + 9, dividerY, x + w - 9, dividerY + 1, withAlpha(brighten(color, 20), 0x58));

        boolean reroll = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
        int buttonY = y + h - 18;
        int bodyTop = dividerY + 6;
        int bodyBottom = reroll ? buttonY - 4 : y + h - 8;
        int available = Math.max(8, bodyBottom - bodyTop);
        int lineHeight = bodyLines.isEmpty() ? 9 : Math.max(8, Math.min(10, available / bodyLines.size()));
        for (int i = 0; i < bodyLines.size(); i++) {
            int yy = bodyTop + i * lineHeight;
            if (yy + 7 > bodyBottom) break;
            g.text(font, bodyLines.get(i), x + 9, yy, 0xFFD9DFE0, false);
        }

        if (reroll) {
            boolean can = menu.rerollsRemaining() > 0 && !selected;
            boolean buttonHover = inside(mouseX, mouseY, x + 8, buttonY, w - 16, 13);
            g.fill(x + 8, buttonY, x + w - 8, buttonY + 13,
                    can && buttonHover ? tint(0xFF10161B, color, 18) : 0xF0101519);
            g.outline(x + 8, buttonY, w - 16, 13,
                    can ? (buttonHover ? brighten(color, 30) : shade(color, 0.76)) : LINE_SOFT);
            String label = selected ? "선택 해제" : menu.rerollsRemaining() > 0 ? "↻ 재추첨" : "재추첨 소진";
            centered(g, fit(label, w - 22), x + w / 2, buttonY + 2,
                    can ? (buttonHover ? TEXT : color) : FAINT);
        } else if (selected) {
            g.fill(x + 12, y + h - 6, x + w - 12, y + h - 3, brighten(color, 36));
        }
    }

    @Unique
    private void drawAura(
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
        double pulse = (Math.sin(System.currentTimeMillis() / 260.0) + 1.0) * 0.5;
        switch (tier) {
            case COMMON -> { }
            case UNCOMMON -> glowRing(g, x, y, w, h, 4, color, hover ? 0x24 : 0x14);
            case RARE -> {
                glowRing(g, x, y, w, h, 6, color, hover ? 0x28 : 0x18);
                glowRing(g, x, y, w, h, 3, brighten(color, 24), 0x30);
            }
            case EPIC -> {
                glowRing(g, x, y, w, h, 9, color, 0x16 + (hover ? 0x0C : 0));
                glowRing(g, x, y, w, h, 5, brighten(color, 20), 0x32);
                glowRing(g, x, y, w, h, 2, brighten(color, 45), 0x58);
            }
            case LEGENDARY -> {
                int outer = 0x12 + (int) Math.round(pulse * 0x0E);
                glowRing(g, x, y, w, h, 12, color, outer);
                glowRing(g, x, y, w, h, 7, brighten(color, 20), 0x2C);
                glowRing(g, x, y, w, h, 3, brighten(color, 58), 0x78);
                g.fill(x + 13, y - 4, x + w - 13, y - 2, withAlpha(brighten(color, 75), 0xD0));
            }
            case MYTHIC -> {
                int outer = 0x14 + (int) Math.round(pulse * 0x12);
                int middle = 0x24 + (int) Math.round(pulse * 0x12);
                glowRing(g, x, y, w, h, 14, color, outer);
                glowRing(g, x, y, w, h, 9, brighten(color, 15), middle);
                glowRing(g, x, y, w, h, 5, brighten(color, 38), 0x4A);
                glowRing(g, x, y, w, h, 2, brighten(color, 76), 0x94);
                g.fill(x + 11, y - 5, x + w - 11, y - 2, withAlpha(brighten(color, 92), 0xE4));
                g.fill(x + 17, y + h + 2, x + w - 17, y + h + 4, withAlpha(color, 0xA0));
            }
        }

        g.fill(x - 3, y - 3, x + w + 3, y + h + 4, 0x42000000);
        int edge = selected ? brighten(color, 68)
                : hover ? brighten(color, 30)
                : tier.ordinal() >= AugmentTier.RARE.ordinal() ? shade(color, 0.82) : 0xFF596268;
        g.outline(x, y, w, h, edge);
        g.outline(x + 3, y + 3, w - 6, h - 6,
                tier.ordinal() >= AugmentTier.EPIC.ordinal()
                        ? withAlpha(brighten(color, 22), 0x92)
                        : LINE_SOFT);
    }

    @Unique
    private void glowRing(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            int spread,
            int color,
            int alpha
    ) {
        int c = withAlpha(color, alpha);
        int thickness = spread >= 10 ? 3 : spread >= 5 ? 2 : 1;
        g.fill(x - spread, y - spread, x + w + spread, y - spread + thickness, c);
        g.fill(x - spread, y + h + spread - thickness, x + w + spread, y + h + spread, c);
        g.fill(x - spread, y - spread + thickness, x - spread + thickness, y + h + spread - thickness, c);
        g.fill(x + w + spread - thickness, y - spread + thickness, x + w + spread, y + h + spread - thickness, c);
    }

    @Unique
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
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, DEEP);
        int tintAmount = switch (tier) {
            case COMMON -> 2;
            case UNCOMMON -> 4;
            case RARE -> 6;
            case EPIC -> 8;
            case LEGENDARY -> 10;
            case MYTHIC -> 11;
        };
        int upper = y + Math.max(54, h * 42 / 100);
        g.fill(x + 4, y + 4, x + w - 4, upper, tint(0xFF0A1014, color, tintAmount));
        g.fill(x + 7, y + 7, x + w - 7, y + 8, withAlpha(0xFFFFFFFF, hover ? 0x2A : 0x18));
        g.fill(x + 10, y + 11, x + Math.max(11, w * 3 / 5), y + 12,
                withAlpha(brighten(color, 55), hover ? 0x24 : 0x12));
        int texture = withAlpha(brighten(color, 36), hover ? 0x12 : 0x08);
        for (int i = 0; i < 3; i++) {
            int yy = y + 18 + i * 6;
            int xx = x + 9 + i * 5;
            g.fill(xx, yy, Math.min(x + w - 9, xx + Math.max(16, w / 3)), yy + 1, texture);
        }
        int lower = y + h - Math.max(15, h / 9);
        g.fill(x + 5, lower, x + w - 5, y + h - 5, withAlpha(color, 0x0B));
    }

    @Unique
    private void drawSocket(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int size,
            AugmentTier tier,
            int color,
            boolean hover
    ) {
        if (tier == AugmentTier.EPIC) glowRing(g, x, y, size, size, 3, color, 0x28);
        if (tier == AugmentTier.LEGENDARY) {
            glowRing(g, x, y, size, size, 5, color, 0x24);
            glowRing(g, x, y, size, size, 2, brighten(color, 30), 0x48);
        }
        if (tier == AugmentTier.MYTHIC) {
            double pulse = (Math.sin(System.currentTimeMillis() / 220.0) + 1.0) * 0.5;
            glowRing(g, x, y, size, size, 6, color, 0x20 + (int) Math.round(pulse * 0x10));
            glowRing(g, x, y, size, size, 3, brighten(color, 38), 0x50);
        }
        g.fill(x, y, x + size, y + size,
                tint(DEEP, color, tier.ordinal() >= AugmentTier.EPIC.ordinal() ? 12 : 5));
        g.outline(x, y, size, size, hover ? brighten(color, 34) : shade(color, 0.84));
        g.outline(x + 3, y + 3, size - 6, size - 6, withAlpha(brighten(color, 24), 0x82));
        g.fill(x + 4, y + 4, x + size - 4, y + 5, withAlpha(0xFFFFFFFF, 0x28));
    }

    @Unique
    private void drawDetail(GuiGraphicsExtractor g, AugmentDefinition d, int mx, int my, Bounds b) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int color = tierColor(d.tier());
        int maxW = Math.max(180, b.width() - 20);
        int panelW = Math.min(390, Math.max(230, maxW * 4 / 5));
        panelW = Math.min(panelW, maxW);
        List<String> lines = wrap(p.detail(), Math.max(40, panelW - 24));
        int lineH = 10;
        int panelH = Math.min(b.height() - 18, 74 + lines.size() * lineH);
        int px = mx + 14;
        if (px + panelW > b.right() - 4) px = mx - panelW - 14;
        px = Math.max(b.left() + 4, Math.min(px, b.right() - panelW - 4));
        int py = my + 12;
        if (py + panelH > b.bottom() - 4) py = my - panelH - 12;
        py = Math.max(b.top() + 4, Math.min(py, b.bottom() - panelH - 4));

        g.fill(px - 3, py - 3, px + panelW + 3, py + panelH + 3, 0xC0000000);
        g.fill(px, py, px + panelW, py + panelH, 0xFB090E11);
        g.outline(px, py, panelW, panelH, brighten(color, 12));
        g.fill(px, py, px + panelW, py + 3, color);
        g.fill(px + 10, py + 11, px + 42, py + 43, DEEP);
        g.outline(px + 10, py + 11, 32, 32, shade(color, 0.84));
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), px + 18, py + 19);
        int tx = px + 50;
        g.text(font, fit(p.displayName(), panelW - 62), tx, py + 12, TEXT, false);
        String meta = tierLabel(d.tier()) + " · 정확한 수치";
        g.text(font, fit(meta, panelW - 62), tx, py + 26, color, false);
        g.fill(px + 10, py + 48, px + panelW - 10, py + 49, LINE_SOFT);
        for (int i = 0; i < lines.size(); i++) {
            int yy = py + 54 + i * lineH;
            if (yy + 8 > py + panelH - 15) break;
            g.text(font, lines.get(i), px + 12, yy, 0xFFE4E8E8, false);
        }
        g.text(font, "Shift · 정확한 수치 표시 중", px + 12, py + panelH - 13, color, false);
    }

    @Unique
    private Geometry geometry() {
        Bounds b = bounds();
        int usableW = b.width();
        int usableH = b.height();
        int tierOrdinal = Math.max(0, menu.tierOrdinal());
        int auraPad = tierOrdinal >= AugmentTier.MYTHIC.ordinal() ? 14
                : tierOrdinal >= AugmentTier.LEGENDARY.ordinal() ? 12
                : tierOrdinal >= AugmentTier.EPIC.ordinal() ? 9 : 6;
        int outerPad = auraPad + 4;
        int gap = usableW >= 390
                ? (tierOrdinal >= AugmentTier.LEGENDARY.ordinal() ? 24 : 20)
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
        int titleY = b.top() + 5;
        int ruleY = titleY + 14;
        int footerY = b.bottom() - 12;
        return new Geometry(b, cardLeft, cardTop, cardW, cardH, gap,
                b.left() + usableW / 2, titleY, ruleY, footerY);
    }

    @Unique
    private Bounds bounds() {
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

    @Unique
    private boolean send(int button) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, button)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        return true;
    }

    @Unique
    private String controlBadge(AugmentDefinition d, AugmentPresentationCatalog.Entry p) {
        String id = d.id();
        if (id.endsWith(":pockets") || id.endsWith(":remote_storage")) return "B";
        if (id.endsWith(":spatial_link")) return "G";
        if (id.endsWith(":authority_theft")) return "J";
        if (p.kind() != AugmentPresentationCatalog.Kind.PASSIVE) return "K·Alt";
        return "";
    }

    @Unique
    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (font.width(word) > maxWidth) {
                if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
                StringBuilder part = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int cp = word.codePointAt(offset);
                    String next = part.toString() + new String(Character.toChars(cp));
                    if (part.length() > 0 && font.width(next) > maxWidth) {
                        out.add(part.toString());
                        part.setLength(0);
                    }
                    part.appendCodePoint(cp);
                    offset += Character.charCount(cp);
                }
                line.append(part);
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else {
                if (line.length() > 0) out.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    @Unique
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

    @Unique private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, s, x - font.width(s) / 2, y, color, false);
    }
    @Unique private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
    @Unique private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount);
        int gg = Math.min(255, ((color >> 8) & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor);
        int gg = (int) (((color >> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int tint(int base, int color, int percent) {
        int inv = 100 - percent;
        int r = (((base >> 16) & 255) * inv + ((color >> 16) & 255) * percent) / 100;
        int gg = (((base >> 8) & 255) * inv + ((color >> 8) & 255) * percent) / 100;
        int b = ((base & 255) * inv + (color & 255) * percent) / 100;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
    @Unique private static int tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFB8BEC5;
            case UNCOMMON -> 0xFF4FC77F;
            case RARE -> 0xFF4394E4;
            case EPIC -> 0xFFAA58D4;
            case LEGENDARY -> 0xFFE4A934;
            case MYTHIC -> 0xFFE44B4B;
        };
    }
    @Unique private static String tierLabel(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> "일반";
            case UNCOMMON -> "고급";
            case RARE -> "희귀";
            case EPIC -> "영웅";
            case LEGENDARY -> "전설";
            case MYTHIC -> "신화";
        };
    }

    @Unique private record Bounds(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    @Unique private record Geometry(
            Bounds bounds,
            int cardLeft,
            int cardTop,
            int cardW,
            int cardH,
            int gap,
            int centerX,
            int titleY,
            int ruleY,
            int footerY
    ) { }
}
