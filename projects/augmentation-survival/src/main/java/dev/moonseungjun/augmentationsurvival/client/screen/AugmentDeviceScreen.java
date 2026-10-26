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
    private final long[] revealStarted = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
    private final int[] observedCandidateKey = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
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
            drawResponsivePanel(g, layout.visibleLeft(), layout.visibleTop(), layout.visibleRight(), layout.visibleBottom());
            return;
        }
        if (view == View.SELECT) {
            TierLayout layout = tierLayout();
            drawResponsivePanel(g, layout.visibleLeft(), layout.visibleTop(), layout.visibleRight(), layout.visibleBottom());
            return;
        }

        int l = leftPos, t = topPos;
        g.fill(l, t, l + W, t + H, BG);
        g.outline(l, t, W, H, 0xFF4A5760);
        g.fill(l + 1, t + 1, l + W - 1, t + 31, 0xFF151D23);
        g.fill(l + 2, t + 2, l + W - 2, t + 3, 0xFF83939D);
    }

    private void drawResponsivePanel(GuiGraphicsExtractor g, int visibleLeft, int visibleTop, int visibleRight, int visibleBottom) {
        int l = leftPos + visibleLeft - 5;
        int t = topPos + visibleTop - 5;
        int r = leftPos + visibleRight + 5;
        int b = topPos + visibleBottom + 5;
        g.fill(l, t, r, b, BG);
        g.outline(l, t, r - l, b - t, 0xFF4A5760);
        g.fill(l + 1, t + 1, r - 1, t + 3, 0xFF83939D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        syncReveal();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            drawOffer(g, mx, my);
            return;
        }
        VisibleBounds bounds = viewportBounds(8);
        if (view != View.SELECT && bounds.width() >= 520) {
            g.text(font, title.getString(), bounds.left(), bounds.top() + 6, TEXT, false);
        }
        drawTabs(g, mx, my);
        switch (view) {
            case SELECT -> drawTierSelect(g, mx, my);
            case OWNED -> drawOwned(g, mx, my);
            case SYNTHESIS -> drawSynthesis(g, mx, my);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        TabLayout tabs = tabLayout();
        drawTab(g, tabs.selectX(), tabs.y(), tabs.selectW(), "동조", view == View.SELECT, mx, my);
        drawTab(g, tabs.ownedX(), tabs.y(), tabs.ownedW(), "보유 증강", view == View.OWNED, mx, my);
        if (menu.advanced()) {
            drawTab(g, tabs.synthesisX(), tabs.y(), tabs.synthesisW(), "코어 합성", view == View.SYNTHESIS, mx, my);
        }
    }

    private void drawTierSelect(GuiGraphicsExtractor g, int mx, int my) {
        TierLayout layout = tierLayout();
        centered(g, "코어 등급 선택", layout.centerX(), layout.headerTop(), TEXT);
        int usableH = layout.visibleBottom() - layout.visibleTop();
        boolean showGuide = usableH >= 190;
        boolean showBonus = !layout.compact() && usableH >= 300;
        if (showGuide) {
            String guide = layout.compact()
                    ? "코어 1개 → 같은 등급 후보 3개"
                    : "코어 1개를 사용해 같은 등급의 증강 후보 3개를 확인합니다.";
            centered(g, clip(guide, layout.visibleRight() - layout.visibleLeft() - 20),
                    layout.centerX(), layout.headerTop() + 14, MUTED);
        }

        String bonus = selectionBonusLine();
        if (!bonus.isBlank() && showBonus) {
            centered(g, clip(bonus, layout.visibleRight() - layout.visibleLeft() - 20),
                    layout.centerX(), layout.headerTop() + 28, 0xFFB9C5CA);
        }

        for (AugmentTier tier : AugmentTier.values()) {
            int index = tier.ordinal();
            int col = index % layout.columns();
            int row = index / layout.columns();
            int x = layout.gridLeft() + col * (layout.cellWidth() + layout.gapX());
            int y = layout.gridTop() + row * (layout.cellHeight() + layout.gapY());
            drawTierChoice(g, x, y, layout.cellWidth(), layout.cellHeight(), tier, mx, my);
        }

        String state = switch (menu.status()) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 증강을 모두 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "동조 완료 · 보유 증강에 반영되었습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "진행 중인 동조를 먼저 완료하세요.";
            default -> "";
        };
        String footer = !state.isBlank() ? state
                : layout.compact() ? selectionBonusLine() : "후보는 선택을 끝낼 때까지 고정됩니다.";
        if (!footer.isBlank()) {
            centered(g, clip(footer, layout.visibleRight() - layout.visibleLeft() - 20), layout.centerX(), layout.footerY(),
                    menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED ? GOOD
                            : menu.status() == AugmentDeviceMenu.STATUS_IDLE ? FAINT : BAD);
        }
    }

    private String selectionBonusLine() {
        int reroll = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID);
        boolean second = ClientAugmentProgressState.level(AugmentCatalog.SECOND_CHOICE_ID) > 0;
        if (reroll <= 0 && !second) return "";
        if (reroll > 0 && second) return "보유 효과 · 재추첨 " + reroll + "회 / 코어 · 후보 2개 선택";
        if (reroll > 0) return "보유 효과 · 재추첨 " + reroll + "회 / 코어";
        return "보유 효과 · 후보 2개 선택";
    }

    private void drawTierChoice(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(tier);
        int count = coreCount(tier);
        boolean enabled = count > 0;
        boolean compactCell = w < 138 || h < 46;
        g.fill(x, y, x + w, y + h, hover && enabled ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + (compactCell ? 2 : 3), y + h, enabled ? color : shade(color, 0.38));
        g.outline(x, y, w, h, hover && enabled ? brighten(color, 24) : LINE);

        int iconX = x + (compactCell ? 7 : 12);
        int itemY = y + Math.max(4, (h - 16) / 2);
        g.item(new ItemStack(AugmentCoreItems.item(tier)), iconX, itemY);
        int textX = x + (compactCell ? 29 : 38);
        int titleY = compactCell ? y + 5 : y + Math.max(7, h / 2 - 13);
        g.text(font, clip(tierLabel(tier), Math.max(24, w - (compactCell ? 35 : 78))), textX, titleY,
                enabled ? TEXT : 0xFF899197, false);
        g.text(font, clip("보유 " + count, Math.max(24, w - (compactCell ? 35 : 78))), textX, titleY + 13,
                enabled ? MUTED : BAD, false);

        if (!compactCell) {
            String action = enabled ? "선택" : "코어 없음";
            int actionColor = enabled ? color : FAINT;
            g.text(font, action, x + w - 10 - font.width(action), y + (h - 9) / 2, actionColor, false);
        }
    }

    private void drawOffer(GuiGraphicsExtractor g, int mx, int my) {
        OfferLayout layout = offerLayout();
        AugmentTier selectedTier = menu.tierOrdinal() >= 0
                ? AugmentTier.values()[menu.tierOrdinal()] : AugmentTier.COMMON;
        int color = tierColor(selectedTier);
        int selectedCount = Integer.bitCount(menu.selectedMask());
        int rerollLevel = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID);

        centered(g, "증강 선택", layout.centerX(), layout.headerTop(), TEXT);
        String rule = tierLabel(selectedTier) + " 코어 · 3개 중 " + menu.selectionCount() + "개 선택";
        centered(g, clip(rule, layout.visibleRight() - layout.visibleLeft() - 20), layout.centerX(), layout.headerTop() + 14, color);
        if (!layout.compact()) {
            String progress = menu.selectionCount() == 2
                    ? "선택 " + selectedCount + " / 2"
                    : "원하는 증강 하나를 고르세요";
            if (rerollLevel > 0) progress += " · 재추첨 " + menu.rerollsRemaining() + "회 남음";
            centered(g, clip(progress, layout.visibleRight() - layout.visibleLeft() - 20),
                    layout.centerX(), layout.headerTop() + 27, MUTED);
        }

        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            int x = layout.cardLeft() + slot * (layout.cardWidth() + layout.gap());
            drawAugmentCard(g, x, layout.cardTop(), layout.cardWidth(), layout.cardHeight(), slot, d, mx, my, layout.compact());
        }

        String footer = rerollLevel > 0
                ? "카드 클릭: 선택 · 재추첨: 한 장 교체 · Shift: 정확한 수치"
                : "카드 클릭: 선택 · Shift: 정확한 수치";
        if (layout.compact() && rerollLevel > 0) footer = "재추첨 " + menu.rerollsRemaining() + "회 · Shift 상세";
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
        boolean tiny = h < 132;
        int color = tierColor(d.tier());
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);

        drawCardFrame(g, x, y, w, h, d.tier(), color, hover, selected);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF080C10);
        g.fill(x + 6, y + 6, x + w - 6, y + 27, tint(0xFF10161B, color, 9));

        String kind = p.kind().label();
        g.text(font, clip(kind, Math.max(20, w - 18)), x + 9, y + 12,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : 0xFFE8D17B, false);
        if (selected && w >= 112) {
            String picked = "선택됨";
            g.text(font, picked, x + w - 9 - font.width(picked), y + 12, brighten(color, 42), false);
        }

        int iconBox = tiny ? 24 : compact ? 32 : 42;
        int iconY = y + (tiny ? 31 : compact ? 34 : 37);
        int iconX = x + (w - iconBox) / 2;
        g.fill(iconX, iconY, iconX + iconBox, iconY + iconBox, 0xFF0C1216);
        g.outline(iconX, iconY, iconBox, iconBox, hover ? shade(color, 0.82) : 0xFF283239);
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + w / 2 - 8, iconY + (iconBox - 16) / 2);

        int nameY = iconY + iconBox + (tiny ? 4 : compact ? 6 : 8);
        centered(g, clip(p.displayName(), Math.max(24, w - 18)), x + w / 2, nameY, TEXT);
        int now = menu.currentLevel(slot), next = Math.min(now + 1, d.maxLevel());
        String level = now == 0
                ? "신규 · Lv." + next + "/" + d.maxLevel()
                : "Lv." + now + " → " + next + "/" + d.maxLevel();
        centered(g, clip(level, Math.max(24, w - 18)), x + w / 2, nameY + 12, now == 0 ? color : MUTED);

        boolean rerollOwned = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
        boolean rerollAction = rerollOwned && h >= 96;
        int actionY = y + h - 22;

        if (!tiny) {
            int dividerY = nameY + 28;
            g.fill(x + 10, dividerY, x + w - 10, dividerY + 1, 0xFF2A3338);
            int descriptionTop = dividerY + 7;
            int descriptionBottom = rerollAction ? actionY - 5 : y + h - 8;
            int availableLines = Math.max(1, (descriptionBottom - descriptionTop) / 10);
            int lineLimit = Math.min(compact ? 4 : (detail ? 7 : 6), availableLines);
            String description = detail ? p.detail() : p.summary();
            drawWrapped(g, description, x + 10, descriptionTop, Math.max(24, w - 20), lineLimit,
                    detail ? 0xFFDCE3E6 : 0xFFC7D0D5);
        } else if (!rerollAction) {
            g.text(font, clip(detail ? p.detail() : p.summary(), Math.max(24, w - 18)),
                    x + 9, y + h - 16, detail ? 0xFFDCE3E6 : 0xFFC7D0D5, false);
        }

        if (rerollAction) {
            boolean buttonHover = inside(mx, my, x + 8, actionY, w - 16, 15);
            boolean canReroll = menu.rerollsRemaining() > 0 && !selected;
            int buttonBg = canReroll && buttonHover ? tint(SURFACE_HI, color, 18) : 0xFF10161B;
            int buttonLine = canReroll ? (buttonHover ? color : shade(color, 0.62)) : 0xFF30383D;
            g.fill(x + 8, actionY, x + w - 8, actionY + 15, buttonBg);
            g.outline(x + 8, actionY, w - 16, 15, buttonLine);
            String label = selected ? "선택 해제" : menu.rerollsRemaining() > 0 ? "↻ 재추첨" : "재추첨 소진";
            centered(g, clip(label, w - 24), x + w / 2, actionY + 3,
                    canReroll ? (buttonHover ? TEXT : color) : FAINT);
        }
    }

    private void drawCardFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier,
                               int color, boolean hover, boolean selected) {
        boolean roomy = w >= 145 && h >= 150;
        if (tier == AugmentTier.EPIC && roomy) {
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(color, 0x66));
            g.outline(x - 1, y - 1, w + 2, h + 2, withAlpha(brighten(color, 18), 0xA0));
        } else if (tier == AugmentTier.LEGENDARY && roomy) {
            g.fill(x - 4, y - 4, x + w + 4, y + h + 4, withAlpha(color, 0x18));
            g.outline(x - 4, y - 4, w + 8, h + 8, withAlpha(color, 0x36));
            g.outline(x - 3, y - 3, w + 6, h + 6, withAlpha(color, 0x58));
            g.outline(x - 1, y - 1, w + 2, h + 2, withAlpha(brighten(color, 24), 0xC8));
        } else if (tier == AugmentTier.MYTHIC && roomy) {
            double wave = (Math.sin(System.currentTimeMillis() / 230.0) + 1.0) * 0.5;
            int outerAlpha = 0x28 + (int) Math.round(wave * 0x18);
            int midAlpha = 0x58 + (int) Math.round(wave * 0x20);
            g.fill(x - 5, y - 5, x + w + 5, y + h + 5, withAlpha(color, 0x20));
            g.outline(x - 5, y - 5, w + 10, h + 10, withAlpha(color, outerAlpha));
            g.outline(x - 4, y - 4, w + 8, h + 8, withAlpha(brighten(color, 12), midAlpha));
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(brighten(color, 28), 0xB8));
            g.fill(x + 8, y - 3, x + w - 8, y - 1, withAlpha(brighten(color, 36), 0x92));
            g.fill(x + 8, y + h + 1, x + w - 8, y + h + 3, withAlpha(color, 0x72));
        } else if (tier.ordinal() >= AugmentTier.EPIC.ordinal()) {
            g.outline(x - 1, y - 1, w + 2, h + 2, withAlpha(color, 0x86));
        }

        g.fill(x, y, x + w, y + h, 0xFF0B1014);
        g.outline(x, y, w, h, selected ? brighten(color, 55) : hover ? color : shade(color, 0.52));
        g.outline(x + 3, y + 3, w - 6, h - 6, selected ? color : 0xFF2B343A);
        if (selected) g.fill(x + 6, y + h - 7, x + w - 6, y + h - 5, color);
    }

    private OfferLayout offerLayout() {
        VisibleBounds bounds = viewportBounds(8);
        int usableW = bounds.width();
        int usableH = bounds.height();
        int gap = usableW >= 620 ? 14 : usableW >= 480 ? 10 : 6;
        int cardW = Math.min(OFFER_MAX_CARD_W, Math.max(82, (usableW - gap * 2) / 3));
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = bounds.left() + Math.max(0, (usableW - groupW) / 2);
        boolean compact = cardW < 145 || usableH < 270;
        int headerTop = bounds.top() + 5;
        int cardTop = headerTop + (compact ? 33 : 43);
        int footerY = bounds.bottom() - 11;
        int cardH = Math.min(OFFER_MAX_CARD_H, Math.max(92, footerY - cardTop - 6));
        if (cardTop + cardH > footerY - 3) cardH = Math.max(88, footerY - cardTop - 3);
        return new OfferLayout(bounds.left(), bounds.right(), bounds.top(), bounds.bottom(),
                cardLeft, cardTop, cardW, cardH, gap, headerTop, footerY, compact);
    }

    private TierLayout tierLayout() {
        VisibleBounds bounds = viewportBounds(8);
        int usableW = bounds.width();
        int usableH = bounds.height();
        int columns = usableW >= 430 ? 3 : usableW >= 180 ? 2 : 1;
        int rows = (AugmentTier.values().length + columns - 1) / columns;
        int gapX = usableW >= 520 ? 10 : 5;
        int gapY = usableH >= 260 ? 8 : 4;
        boolean compact = usableW < 430 || usableH < 285;
        int headerTop = bounds.top() + 27;
        boolean showGuide = usableH >= 190;
        boolean showBonus = !compact && usableH >= 300;
        int headerSpace = showBonus ? 42 : showGuide ? 27 : 14;
        int gridTop = headerTop + headerSpace;
        int footerY = bounds.bottom() - 8;
        int gridBudget = Math.max(1, footerY - gridTop - 5);
        int rawCellW = Math.max(1, (usableW - gapX * (columns - 1)) / columns);
        int cellW = Math.min(190, Math.max(72, rawCellW));
        int rawCellH = Math.max(1, (gridBudget - gapY * (rows - 1)) / rows);
        int cellH = Math.min(78, Math.max(24, rawCellH));
        int gridW = cellW * columns + gapX * (columns - 1);
        int gridLeft = bounds.left() + Math.max(0, (usableW - gridW) / 2);
        return new TierLayout(bounds.left(), bounds.right(), bounds.top(), bounds.bottom(),
                gridLeft, gridTop, cellW, cellH, gapX, gapY, columns, headerTop, footerY, compact);
    }

    private TabLayout tabLayout() {
        VisibleBounds bounds = viewportBounds(8);
        int gap = bounds.width() >= 300 ? 8 : 4;
        if (!menu.advanced()) {
            int total = 72 + gap + 88;
            int start = bounds.left() + Math.max(0, (bounds.width() - total) / 2);
            return new TabLayout(start, 72, start + 72 + gap, 88, -1, 0, bounds.top() + 3);
        }
        int available = bounds.width() - gap * 2;
        int selectW = Math.min(72, Math.max(54, available / 3));
        int ownedW = Math.min(88, Math.max(60, (available - selectW) / 2));
        int synthesisW = Math.min(88, Math.max(60, available - selectW - ownedW));
        int total = selectW + ownedW + synthesisW + gap * 2;
        int start = bounds.left() + Math.max(0, (bounds.width() - total) / 2);
        return new TabLayout(start, selectW, start + selectW + gap, ownedW,
                start + selectW + gap + ownedW + gap, synthesisW, bounds.top() + 3);
    }

    private VisibleBounds viewportBounds(int margin) {
        int scaledW = minecraft == null ? W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? H : minecraft.getWindow().getGuiScaledHeight();
        int left = Math.max(margin, -leftPos + margin);
        int right = Math.min(W - margin, scaledW - leftPos - margin);
        int top = Math.max(margin, -topPos + margin);
        int bottom = Math.min(H - margin, scaledH - topPos - margin);
        if (right <= left) { left = margin; right = W - margin; }
        if (bottom <= top) { top = margin; bottom = H - margin; }
        return new VisibleBounds(left, right, top, bottom);
    }

    private void drawOwned(GuiGraphicsExtractor g, int mx, int my) {
        List<AugmentDefinition> owned = ownedDefinitions();
        int pages = Math.max(1, (owned.size() + OWNED_PER_PAGE - 1) / OWNED_PER_PAGE);
        ownedPage = Math.max(0, Math.min(ownedPage, pages - 1));
        g.text(font, "보유 증강 " + owned.size() + " / " + AugmentCatalog.allDefinitions().size(), 18, 48, TEXT, false);
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
        g.text(font, clip(text, Math.max(20, w - 8)), x + (w - font.width(clip(text, Math.max(20, w - 8)))) / 2, y + 5, color, false);
        if (active && w > 22) g.fill(x + 10, y + 18, x + w - 10, y + 20, 0xFF8FA3AE);
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
            boolean rerollOwned = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
            for (int slot = 0; slot < 3; slot++) {
                int cx = layout.cardLeft() + slot * (layout.cardWidth() + layout.gap());
                int cy = layout.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
                boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
                if (rerollOwned && layout.cardHeight() >= 96) {
                    int actionY = cy + layout.cardHeight() - 22;
                    if (inside(x, y, cx + 8, actionY, layout.cardWidth() - 16, 15)) {
                        if (selected) {
                            playClick(0.94F);
                            return send(AugmentDeviceMenu.candidateButtonId(slot));
                        }
                        if (menu.rerollsRemaining() > 0) {
                            playClick(1.16F);
                            return send(AugmentDeviceMenu.rerollButtonId(slot));
                        }
                        return true;
                    }
                }
                if (menu.candidate(slot) != null && inside(x, y, cx, cy, layout.cardWidth(), layout.cardHeight())) {
                    playClick(1.05F + slot * 0.04F);
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
                }
            }
            return true;
        }

        TabLayout tabs = tabLayout();
        if (inside(x, y, tabs.selectX(), tabs.y(), tabs.selectW(), 20)) { view = View.SELECT; playClick(1.0F); return true; }
        if (inside(x, y, tabs.ownedX(), tabs.y(), tabs.ownedW(), 20)) { view = View.OWNED; playClick(1.04F); return true; }
        if (menu.advanced() && inside(x, y, tabs.synthesisX(), tabs.y(), tabs.synthesisW(), 20)) { view = View.SYNTHESIS; playClick(1.08F); return true; }

        if (view == View.SELECT) {
            TierLayout layout = tierLayout();
            for (AugmentTier tier : AugmentTier.values()) {
                int index = tier.ordinal();
                int col = index % layout.columns();
                int row = index / layout.columns();
                int xx = layout.gridLeft() + col * (layout.cellWidth() + layout.gapX());
                int yy = layout.gridTop() + row * (layout.cellHeight() + layout.gapY());
                if (inside(x, y, xx, yy, layout.cellWidth(), layout.cellHeight()) && coreCount(tier) > 0) {
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
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            AugmentDefinition d = menu.candidate(i);
            int key = d == null ? 0 : d.id().hashCode();
            if (key != observedCandidateKey[i] && menu.status() == AugmentDeviceMenu.STATUS_READY) {
                revealStarted[i] = now + i * 55L;
            }
            observedCandidateKey[i] = key;
        }
    }

    private double revealProgress(int slot) {
        if (revealStarted[slot] == Long.MIN_VALUE) return 1.0;
        long age = System.currentTimeMillis() - revealStarted[slot];
        if (age <= 0) return 0.0;
        double p = Math.max(0.0, Math.min(1.0, age / 240.0));
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
        while (result.length() > 1 && font.width(result + "…") > maxWidth) result = result.substring(0, result.length() - 1);
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

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
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

    private record VisibleBounds(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private record OfferLayout(
            int visibleLeft, int visibleRight, int visibleTop, int visibleBottom,
            int cardLeft, int cardTop, int cardWidth, int cardHeight, int gap,
            int headerTop, int footerY, boolean compact
    ) {
        int centerX() { return (visibleLeft + visibleRight) / 2; }
    }

    private record TierLayout(
            int visibleLeft, int visibleRight, int visibleTop, int visibleBottom,
            int gridLeft, int gridTop, int cellWidth, int cellHeight,
            int gapX, int gapY, int columns, int headerTop, int footerY, boolean compact
    ) {
        int centerX() { return (visibleLeft + visibleRight) / 2; }
    }

    private record TabLayout(
            int selectX, int selectW, int ownedX, int ownedW,
            int synthesisX, int synthesisW, int y
    ) { }

    private enum View { SELECT, OWNED, SYNTHESIS }
}
