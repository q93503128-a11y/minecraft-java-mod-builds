package dev.moonseungjun.augmentationsurvival.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Focused augment selection/collection/synthesis UI. Gameplay state remains server-authoritative. */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704, H = 360;
    private static final int BG = 0xF20A0E12, SURFACE = 0xFF11171C, SURFACE_HI = 0xFF172027;
    private static final int LINE = 0xFF37434B, LINE_HI = 0xFF8797A1;
    private static final int TEXT = 0xFFF3F6F8, MUTED = 0xFF909CA3, FAINT = 0xFF616B71;
    private static final int GOOD = 0xFF79D496, BAD = 0xFFE67F86;
    private static final int OFFER_MAX_CARD_W = 196, OFFER_MAX_CARD_H = 238;
    private static final int OWNED_PER_PAGE = 18;

    private View view = View.SELECT;
    private int ownedPage;
    private int synthesisTier;
    private long revealStarted = Long.MIN_VALUE;
    private int observedCandidateKey = Integer.MIN_VALUE;
    private String hoveredOwnedId = "";

    public AugmentDeviceScreen(AugmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int scaledW = minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft.getWindow().getGuiScaledHeight();
        g.fill(0, 0, scaledW, scaledH, 0x8B000000);

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            OfferLayout layout = offerLayout();
            int l = leftPos + layout.visibleLeft() - 5;
            int t = topPos + layout.visibleTop() - 5;
            int r = leftPos + layout.visibleRight() + 5;
            int b = topPos + layout.visibleBottom() + 5;
            g.fill(l, t, r, b, BG);
            g.outline(l, t, r - l, b - t, 0xFF4A5760);
            g.fill(l + 1, t + 1, r - 1, t + 3, 0xFF83939D);
            return;
        }

        int l = leftPos, t = topPos;
        g.fill(l, t, l + W, t + H, BG);
        g.outline(l, t, W, H, 0xFF4A5760);
        g.fill(l + 1, t + 1, l + W - 1, t + 31, 0xFF151D23);
        g.fill(l + 2, t + 2, l + W - 2, t + 3, 0xFF83939D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        syncReveal();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        boolean choosing = menu.status() == AugmentDeviceMenu.STATUS_READY;
        if (choosing) {
            drawOffer(g, mx, my);
            return;
        }
        g.text(font, title.getString(), 14, 11, TEXT, false);
        drawTabs(g, mx, my);
        switch (view) {
            case SELECT -> drawTierSelect(g, mx, my);
            case OWNED -> drawOwned(g, mx, my);
            case SYNTHESIS -> drawSynthesis(g, mx, my);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        int x = 250;
        drawTab(g, x, 9, 72, "동조", view == View.SELECT, mx, my);
        drawTab(g, x + 76, 9, 88, "보유 증강", view == View.OWNED, mx, my);
        if (menu.advanced()) drawTab(g, x + 168, 9, 88, "코어 합성", view == View.SYNTHESIS, mx, my);
    }

    private void drawTierSelect(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "동조할 코어 등급을 선택하세요", W / 2, 53, TEXT);
        centered(g, "코어 하나가 세 후보를 고정합니다 · 화면을 닫아도 후보는 바뀌지 않습니다", W / 2, 70, MUTED);
        int startX = 25, y = 103, cellW = 101, gap = 10;
        for (AugmentTier tier : AugmentTier.values()) {
            int x = startX + tier.ordinal() * (cellW + gap);
            drawTierChoice(g, x, y, cellW, 116, tier, mx, my);
        }
        centered(g, "획득한 증강과 현재 레벨은 ‘보유 증강’에서 확인할 수 있습니다.", W / 2, 246, FAINT);
        String state = switch (menu.status()) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 증강을 모두 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "동조 완료 · 획득 정보가 보유 증강에 반영되었습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "진행 중인 동조를 먼저 완료하세요.";
            default -> "";
        };
        if (!state.isBlank()) centered(g, state, W / 2, 274,
                menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED ? GOOD : BAD);
    }

    private void drawTierChoice(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(tier);
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + w, y + 2, color);
        g.outline(x, y, w, h, hover ? brighten(color, 26) : LINE);
        g.item(new ItemStack(AugmentCoreItems.item(tier)), x + w / 2 - 8, y + 21);
        centered(g, tierLabel(tier), x + w / 2, y + 49, hover ? TEXT : 0xFFD3DADF);
        int count = coreCount(tier);
        centered(g, "보유 " + count, x + w / 2, y + 69, count > 0 ? MUTED : BAD);
        centered(g, count > 0 ? "동조" : "코어 필요", x + w / 2, y + 94, count > 0 ? color : FAINT);
    }

    private void drawOffer(GuiGraphicsExtractor g, int mx, int my) {
        OfferLayout layout = offerLayout();
        AugmentTier selectedTier = menu.tierOrdinal() >= 0
                ? AugmentTier.values()[menu.tierOrdinal()] : AugmentTier.COMMON;
        int color = tierColor(selectedTier);
        int selectedCount = Integer.bitCount(menu.selectedMask());

        centered(g, "증강 선택", layout.centerX(), layout.headerTop(), TEXT);
        String rule = tierLabel(selectedTier) + " 코어 · 3개 중 " + menu.selectionCount() + "개 선택";
        centered(g, rule, layout.centerX(), layout.headerTop() + 14, color);
        if (!layout.compact()) {
            String progress = menu.selectionCount() == 2
                    ? "선택 " + selectedCount + " / 2 · 서로 다른 증강을 고르세요"
                    : "원하는 증강 하나를 고르세요";
            centered(g, progress, layout.centerX(), layout.headerTop() + 27, MUTED);
        }

        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            int x = layout.cardLeft() + slot * (layout.cardWidth() + layout.gap());
            drawAugmentCard(g, x, layout.cardTop(), layout.cardWidth(), layout.cardHeight(), slot, d, mx, my, layout.compact());
        }

        String footer = menu.selectionCount() == 2
                ? "선택 " + selectedCount + " / 2 · 카드에 마우스를 올리고 Shift: 정확한 수치"
                : "카드에 마우스를 올리고 Shift: 정확한 수치";
        centered(g, clip(footer, layout.visibleRight() - layout.visibleLeft() - 12),
                layout.centerX(), layout.footerY(), MUTED);
    }

    private void drawAugmentCard(GuiGraphicsExtractor g, int x, int y, int w, int h, int slot,
                                 AugmentDefinition d, int mx, int my, boolean compact) {
        double reveal = revealProgress(slot);
        y += (int) Math.round((1.0 - reveal) * 10.0);
        boolean hover = inside(mx, my, x, y, w, h);
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean detail = hover && shiftDown();
        int color = tierColor(d.tier());
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);

        drawCardFrame(g, x, y, w, h, d.tier(), color, hover, selected);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF080C10);
        g.fill(x + 6, y + 6, x + w - 6, y + 27, tint(0xFF10161B, color, 7));

        String kind = p.kind().label();
        g.text(font, clip(kind, Math.max(20, w - 18)), x + 9, y + 12,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : 0xFFE8D17B, false);
        if (selected) {
            String picked = "선택됨";
            g.text(font, picked, x + w - 9 - font.width(picked), y + 12, brighten(color, 42), false);
        }

        int iconBox = compact ? 32 : 42;
        int iconY = y + (compact ? 34 : 37);
        int iconX = x + (w - iconBox) / 2;
        g.fill(iconX, iconY, iconX + iconBox, iconY + iconBox, 0xFF0C1216);
        g.outline(iconX, iconY, iconBox, iconBox, hover ? shade(color, 0.78) : 0xFF283239);
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + w / 2 - 8, iconY + (iconBox - 16) / 2);

        int nameY = iconY + iconBox + (compact ? 6 : 8);
        centered(g, clip(p.displayName(), Math.max(24, w - 18)), x + w / 2, nameY, TEXT);
        int now = menu.currentLevel(slot), next = Math.min(now + 1, d.maxLevel());
        String level = now == 0
                ? "신규 · Lv." + next + "/" + d.maxLevel()
                : "Lv." + now + " → " + next + "/" + d.maxLevel();
        centered(g, clip(level, Math.max(24, w - 18)), x + w / 2, nameY + 13, now == 0 ? color : MUTED);

        int dividerY = nameY + 29;
        g.fill(x + 10, dividerY, x + w - 10, dividerY + 1, 0xFF2A3338);
        int descriptionTop = dividerY + 8;
        int bottomTextY = y + h - 18;
        int availableLines = Math.max(1, (bottomTextY - descriptionTop - 4) / 10);
        int lineLimit = Math.min(compact ? 4 : (detail ? 7 : 6), availableLines);
        String description = detail ? p.detail() : p.summary();
        drawWrapped(g, description, x + 10, descriptionTop, Math.max(24, w - 20), lineLimit,
                detail ? 0xFFDCE3E6 : 0xFFC7D0D5);

        if (h >= 132) {
            String shift = detail ? "상세 수치" : "Shift 상세";
            centered(g, shift, x + w / 2, bottomTextY, detail ? color : FAINT);
        }
    }

    private void drawCardFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier,
                               int color, boolean hover, boolean selected) {
        if (tier.ordinal() >= AugmentTier.EPIC.ordinal() && w >= 120 && h >= 150) {
            int halo = tier == AugmentTier.MYTHIC ? brighten(color, 28) : shade(color, 0.68);
            g.outline(x - 2, y - 2, w + 4, h + 4, halo);
        }
        if (tier.ordinal() >= AugmentTier.LEGENDARY.ordinal() && w >= 120 && h >= 150) {
            g.outline(x - 1, y - 1, w + 2, h + 2, shade(color, 0.9));
        }
        g.fill(x, y, x + w, y + h, 0xFF0B1014);
        g.outline(x, y, w, h, selected ? brighten(color, 55) : hover ? color : shade(color, 0.52));
        g.outline(x + 3, y + 3, w - 6, h - 6, selected ? color : 0xFF2B343A);
        if (selected) {
            g.fill(x + 6, y + h - 7, x + w - 6, y + h - 5, color);
        }
    }

    private OfferLayout offerLayout() {
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
        int cardW = Math.min(OFFER_MAX_CARD_W, Math.max(82, (usableW - gap * 2) / 3));
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = visibleLeft + Math.max(0, (usableW - groupW) / 2);
        boolean compact = cardW < 145 || usableH < 270;
        int headerTop = visibleTop + 5;
        int cardTop = headerTop + (compact ? 33 : 43);
        int footerY = visibleBottom - 11;
        int cardH = Math.min(OFFER_MAX_CARD_H, Math.max(104, footerY - cardTop - 9));
        if (cardTop + cardH > footerY - 5) cardH = Math.max(92, footerY - cardTop - 5);
        return new OfferLayout(visibleLeft, visibleRight, visibleTop, visibleBottom,
                cardLeft, cardTop, cardW, cardH, gap, headerTop, footerY, compact);
    }

    private void drawOwned(GuiGraphicsExtractor g, int mx, int my) {
        List<AugmentDefinition> owned = ownedDefinitions();
        int pages = Math.max(1, (owned.size() + OWNED_PER_PAGE - 1) / OWNED_PER_PAGE);
        ownedPage = Math.max(0, Math.min(ownedPage, pages - 1));
        g.text(font, "보유 증강 " + owned.size() + " / 146", 18, 48, TEXT, false);
        String help = "마우스: 설명 · Shift: 정확한 수치";
        g.text(font, help, W - 18 - font.width(help), 48, MUTED, false);
        hoveredOwnedId = "";

        int start = ownedPage * OWNED_PER_PAGE;
        int end = Math.min(owned.size(), start + OWNED_PER_PAGE);
        int startX = 18, startY = 68, cellW = 216, cellH = 31, gapX = 8, gapY = 5;
        for (int i = start; i < end; i++) {
            int local = i - start, col = local % 3, row = local / 3;
            AugmentDefinition d = owned.get(i);
            drawOwnedCell(g, startX + col * (cellW + gapX), startY + row * (cellH + gapY), cellW, cellH, d, mx, my);
        }
        if (owned.isEmpty()) centered(g, "아직 획득한 증강이 없습니다.", W / 2, 150, MUTED);
        if (pages > 1) drawPager(g, W / 2, 304, ownedPage, pages, mx, my);
        if (!hoveredOwnedId.isBlank()) {
            AugmentDefinition d = AugmentCatalog.require(hoveredOwnedId);
            int level = ClientAugmentProgressState.level(d.id());
            if (shiftDown()) drawDetailedStrip(g, d, 18, H - 45, W - 36, level);
            else drawSummaryStrip(g, d, 18, H - 45, W - 36, level);
        }
    }

    private void drawOwnedCell(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentDefinition d, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(d.tier());
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + 2, y + h, color);
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + 7, y + 8);
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        g.text(font, clip(p.displayName(), w - 72), x + 29, y + 6, hover ? TEXT : 0xFFD3DADE, false);
        int level = ClientAugmentProgressState.level(d.id());
        g.text(font, "Lv." + level + "/" + d.maxLevel(), x + 29, y + 18, MUTED, false);
        String kind = p.kind().label();
        g.text(font, kind, x + w - 8 - font.width(kind), y + 11,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? FAINT : 0xFFE8CF78, false);
        if (hover) hoveredOwnedId = d.id();
    }

    private void drawPager(GuiGraphicsExtractor g, int centerX, int y, int page, int pages, int mx, int my) {
        boolean left = page > 0 && inside(mx, my, centerX - 58, y - 5, 28, 20);
        boolean right = page + 1 < pages && inside(mx, my, centerX + 30, y - 5, 28, 20);
        g.text(font, "‹", centerX - 50, y, left ? TEXT : FAINT, false);
        String state = (page + 1) + " / " + pages;
        centered(g, state, centerX, y, MUTED);
        g.text(font, "›", centerX + 42, y, right ? TEXT : FAINT, false);
    }

    private void drawSynthesis(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "같은 등급 코어 3개를 다음 등급 코어 1개로 정제합니다.", W / 2, 54, MUTED);
        int y = 77;
        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier tier = AugmentTier.values()[i];
            int x = 131 + i * 89;
            boolean selected = synthesisTier == i, hover = inside(mx, my, x, y, 78, 28);
            g.fill(x, y, x + 78, y + 28, selected || hover ? SURFACE_HI : SURFACE);
            g.fill(x, y + 26, x + 78, y + 28, selected ? tierColor(tier) : 0xFF343D43);
            centered(g, tierLabel(tier), x + 39, y + 9, selected ? TEXT : MUTED);
        }
        AugmentTier from = AugmentTier.values()[synthesisTier];
        AugmentTier to = from.next().orElse(AugmentTier.MYTHIC);
        int cy = 172;
        for (int i = 0; i < 3; i++) g.item(new ItemStack(AugmentCoreItems.item(from)), 196 + i * 23, cy);
        g.text(font, "×3", 263, cy + 5, MUTED, false);
        centered(g, "→", W / 2, cy + 5, tierColor(to));
        g.item(new ItemStack(AugmentCoreItems.item(to)), 433, cy);
        g.text(font, "×1", 455, cy + 5, MUTED, false);
        centered(g, tierLabel(from) + "  →  " + tierLabel(to), W / 2, 205, TEXT);
        centered(g, "보유 " + coreCount(from), W / 2, 223, coreCount(from) >= 3 ? MUTED : BAD);
        drawButton(g, 259, 251, 82, 26, "1회 정제", mx, my);
        drawButton(g, 363, 251, 82, 26, "모두 정제", mx, my);
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED) centered(g, "정제 완료 ×" + Math.max(1, menu.lastOutputCount()), W / 2, 298, GOOD);
        else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) centered(g, "코어가 부족합니다.", W / 2, 298, BAD);
    }

    private void drawSummaryStrip(GuiGraphicsExtractor g, AugmentDefinition d, int x, int y, int w, int level) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        g.fill(x, y, x + w, y + 34, 0xE30D1216);
        g.fill(x, y, x + 3, y + 34, tierColor(d.tier()));
        g.text(font, p.displayName() + "  Lv." + level, x + 11, y + 6, TEXT, false);
        g.text(font, clip(p.summary(), w - 22), x + 11, y + 19, 0xFFC7D0D5, false);
    }

    private void drawDetailedStrip(GuiGraphicsExtractor g, AugmentDefinition d, int x, int y, int w, int level) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        g.fill(x, y - 16, x + w, y + 36, 0xF20B1014);
        g.fill(x, y - 16, x + 3, y + 36, tierColor(d.tier()));
        g.text(font, p.displayName() + "  Lv." + level + "/" + d.maxLevel(), x + 11, y - 10, TEXT, false);
        List<String> lines = wrap(p.detail(), w - 22);
        for (int i = 0; i < Math.min(3, lines.size()); i++) g.text(font, lines.get(i), x + 11, y + 3 + i * 10, 0xFFD2D9DD, false);
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 20);
        int color = active ? 0xFFC7D3D9 : hover ? LINE_HI : MUTED;
        g.text(font, text, x + (w - font.width(text)) / 2, y + 5, color, false);
        if (active) g.fill(x + 10, y + 18, x + w - 10, y + 20, 0xFF8FA3AE);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.outline(x, y, w, h, hover ? LINE_HI : LINE);
        centered(g, text, x + w / 2, y + 9, hover ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double x = event.x() - leftPos, y = event.y() - topPos;
        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            OfferLayout layout = offerLayout();
            for (int slot = 0; slot < 3; slot++) {
                int cx = layout.cardLeft() + slot * (layout.cardWidth() + layout.gap());
                int cy = layout.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
                if (menu.candidate(slot) != null && inside(x, y, cx, cy, layout.cardWidth(), layout.cardHeight())) {
                    playClick(1.05F + slot * 0.04F);
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
                }
            }
            return true;
        }
        if (inside(x, y, 250, 9, 72, 20)) { view = View.SELECT; playClick(1.0F); return true; }
        if (inside(x, y, 326, 9, 88, 20)) { view = View.OWNED; playClick(1.04F); return true; }
        if (menu.advanced() && inside(x, y, 418, 9, 88, 20)) { view = View.SYNTHESIS; playClick(1.08F); return true; }

        if (view == View.SELECT) {
            int startX = 25, yy = 103, cellW = 101, gap = 10;
            for (AugmentTier tier : AugmentTier.values()) {
                int xx = startX + tier.ordinal() * (cellW + gap);
                if (inside(x, y, xx, yy, cellW, 116) && coreCount(tier) > 0) {
                    playClick(0.92F + tier.ordinal() * 0.04F);
                    return send(AugmentDeviceMenu.beginButtonId(tier));
                }
            }
        } else if (view == View.OWNED) {
            List<AugmentDefinition> owned = ownedDefinitions();
            int pages = Math.max(1, (owned.size() + OWNED_PER_PAGE - 1) / OWNED_PER_PAGE);
            if (inside(x, y, W / 2 - 58, 299, 28, 20) && ownedPage > 0) {
                ownedPage--;
                playClick(0.96F);
                return true;
            }
            if (inside(x, y, W / 2 + 30, 299, 28, 20) && ownedPage + 1 < pages) {
                ownedPage++;
                playClick(1.04F);
                return true;
            }
        } else if (view == View.SYNTHESIS && menu.advanced()) {
            for (int i = 0; i < AugmentTier.values().length - 1; i++) {
                int xx = 131 + i * 89;
                if (inside(x, y, xx, 77, 78, 28)) { synthesisTier = i; playClick(0.95F + i * 0.05F); return true; }
            }
            AugmentTier tier = AugmentTier.values()[synthesisTier];
            if (inside(x, y, 259, 251, 82, 26)) return send(AugmentDeviceMenu.upgradeOneButtonId(tier));
            if (inside(x, y, 363, 251, 82, 26)) return send(AugmentDeviceMenu.upgradeAllButtonId(tier));
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean send(int id) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, id)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private void syncReveal() {
        int key = 17;
        for (int i = 0; i < 3; i++) {
            AugmentDefinition d = menu.candidate(i);
            key = 31 * key + (d == null ? 0 : d.id().hashCode());
        }
        if (key != observedCandidateKey && menu.status() == AugmentDeviceMenu.STATUS_READY) revealStarted = System.currentTimeMillis();
        observedCandidateKey = key;
    }

    private double revealProgress(int slot) {
        if (revealStarted == Long.MIN_VALUE) return 1.0;
        long age = System.currentTimeMillis() - revealStarted - slot * 90L;
        if (age <= 0) return 0.0;
        double p = Math.max(0.0, Math.min(1.0, age / 260.0));
        double inv = 1.0 - p;
        return 1.0 - inv * inv * inv;
    }

    private List<AugmentDefinition> ownedDefinitions() {
        List<AugmentDefinition> owned = new ArrayList<>();
        for (String id : ClientAugmentProgressState.levels().keySet()) {
            try { owned.add(AugmentCatalog.require(id)); } catch (RuntimeException ignored) { }
        }
        owned.sort(Comparator.comparing((AugmentDefinition d) -> d.tier().ordinal())
                .thenComparing(d -> AugmentPresentationCatalog.get(d).displayName()));
        return owned;
    }

    private int coreCount(AugmentTier tier) {
        if (minecraft == null || minecraft.player == null) return 0;
        int count = 0;
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(AugmentCoreItems.item(tier))) count += stack.getCount();
        }
        return count;
    }

    private boolean shiftDown() {
        return minecraft != null && (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }

    private void playClick(float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch));
    }

    private void drawWrapped(GuiGraphicsExtractor g, String value, int x, int y, int maxWidth, int maxLines, int color) {
        List<String> lines = wrap(value, maxWidth);
        int visible = Math.min(maxLines, lines.size());
        for (int i = 0; i < visible; i++) {
            String line = lines.get(i);
            if (i == visible - 1 && lines.size() > visible) line = ellipsize(line, maxWidth);
            g.text(font, line, x, y + i * 10, color, false);
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
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private String clip(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        return ellipsize(value, maxWidth);
    }

    private String ellipsize(String value, int maxWidth) {
        String result = value;
        while (result.length() > 1 && font.width(result + "…") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x - font.width(value) / 2, y, color, false);
    }

    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount), gg = Math.min(255, ((color >> 8) & 255) + amount), b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor), gg = (int) (((color >> 8) & 255) * factor), b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int tint(int base, int color, int percent) {
        int inv = 100 - percent;
        int r = (((base >> 16) & 255) * inv + ((color >> 16) & 255) * percent) / 100;
        int gg = (((base >> 8) & 255) * inv + ((color >> 8) & 255) * percent) / 100;
        int b = ((base & 255) * inv + (color & 255) * percent) / 100;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFB8BEC5;
            case UNCOMMON -> 0xFF4FC77F;
            case RARE -> 0xFF4394E4;
            case EPIC -> 0xFFAA58D4;
            case LEGENDARY -> 0xFFE4A934;
            case MYTHIC -> 0xFFE54C76;
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

    private record OfferLayout(
            int visibleLeft,
            int visibleRight,
            int visibleTop,
            int visibleBottom,
            int cardLeft,
            int cardTop,
            int cardWidth,
            int cardHeight,
            int gap,
            int headerTop,
            int footerY,
            boolean compact
    ) {
        int centerX() {
            return (visibleLeft + visibleRight) / 2;
        }
    }

    private enum View { SELECT, OWNED, SYNTHESIS }
}
