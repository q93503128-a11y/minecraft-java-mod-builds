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

/**
 * Augment device presentation:
 * - tier selection behaves like a tier chooser, not a card catalogue;
 * - offers keep the three-choice focus of roguelike augment screens;
 * - owned augments read as a collection browser;
 * - synthesis reads as material -> result.
 * Gameplay state remains server-authoritative.
 */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704, H = 360;

    private static final int SCRIM = 0xA0000000;
    private static final int BG = 0xF20A0D10;
    private static final int TOP = 0xFF14191D;
    private static final int SURFACE = 0xFF111619;
    private static final int SURFACE_HI = 0xFF192126;
    private static final int SURFACE_DEEP = 0xFF090D10;
    private static final int LINE = 0xFF354047;
    private static final int LINE_SOFT = 0xFF273137;
    private static final int LINE_HI = 0xFF85949B;
    private static final int TEXT = 0xFFF2F3F1;
    private static final int MUTED = 0xFFA2AAAD;
    private static final int FAINT = 0xFF667075;
    private static final int GOOD = 0xFF79D496;
    private static final int BAD = 0xFFE67F86;
    private static final int DEVICE_ACCENT = 0xFF91B9B3;
    private static final int GOLD = 0xFFE2C679;

    private static final int OFFER_MAX_CARD_W = 196;
    private static final int OFFER_MAX_CARD_H = 238;

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
        g.fill(0, 0, scaledW, scaledH, SCRIM);
        VisibleBounds b = viewportBounds(8);
        drawShell(g, b);
    }

    private void drawShell(GuiGraphicsExtractor g, VisibleBounds b) {
        int l = leftPos + b.left() - 5;
        int t = topPos + b.top() - 5;
        int r = leftPos + b.right() + 5;
        int bot = topPos + b.bottom() + 5;

        g.fill(l, t, r, bot, BG);
        g.outline(l, t, r - l, bot - t, 0xFF48545A);
        g.fill(l + 1, t + 1, r - 1, t + 30, TOP);
        g.fill(l + 2, t + 2, r - 2, t + 4, DEVICE_ACCENT);

        drawCorner(g, l + 4, t + 4, 1, 1);
        drawCorner(g, r - 5, t + 4, -1, 1);
        drawCorner(g, l + 4, bot - 5, 1, -1);
        drawCorner(g, r - 5, bot - 5, -1, -1);
    }

    private void drawCorner(GuiGraphicsExtractor g, int x, int y, int dx, int dy) {
        int hx = x + dx * 10;
        int hy = y + dy * 2;
        int vx = x + dx * 2;
        int vy = y + dy * 10;
        g.fill(Math.min(x, hx), Math.min(y, hy), Math.max(x, hx), Math.max(y, hy), LINE_HI);
        g.fill(Math.min(x, vx), Math.min(y, vy), Math.max(x, vx), Math.max(y, vy), LINE_HI);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        syncReveal();
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            drawOffer(g, mx, my);
            return;
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

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 20);
        int label = active ? TEXT : hover ? 0xFFD9DFE0 : MUTED;
        String clipped = clip(text, Math.max(20, w - 12));

        if (active) {
            g.fill(x + 4, y + 1, x + w - 4, y + 19, 0x491E2A2D);
            g.fill(x + 9, y + 18, x + w - 9, y + 20, DEVICE_ACCENT);
        } else if (hover) {
            g.fill(x + 8, y + 18, x + w - 8, y + 19, LINE_HI);
        }
        centered(g, clipped, x + w / 2, y + 5, label);
    }

    private void drawTierSelect(GuiGraphicsExtractor g, int mx, int my) {
        TierLayout l = tierLayout();
        centered(g, "코어 등급 선택", l.centerX(), l.headerTop(), TEXT);

        int usableH = l.visibleBottom() - l.visibleTop();
        boolean showGuide = usableH >= 190;
        boolean showBonus = !l.compact() && usableH >= 300;
        if (showGuide) {
            String guide = l.compact()
                    ? "코어 1개 → 같은 등급 후보 3개"
                    : "사용할 코어 등급을 선택하세요. 선택한 등급의 증강 후보 3개가 제시됩니다.";
            centered(g, clip(guide, l.visibleRight() - l.visibleLeft() - 22),
                    l.centerX(), l.headerTop() + 14, MUTED);
        }

        String bonus = selectionBonusLine();
        if (!bonus.isBlank() && showBonus) {
            centered(g, clip(bonus, l.visibleRight() - l.visibleLeft() - 22),
                    l.centerX(), l.headerTop() + 28, 0xFFC0C8C9);
        }

        for (AugmentTier tier : AugmentTier.values()) {
            int index = tier.ordinal();
            int col = index % l.columns();
            int row = index / l.columns();
            int x = l.gridLeft() + col * (l.cellWidth() + l.gapX());
            int y = l.gridTop() + row * (l.cellHeight() + l.gapY());
            drawTierChoice(g, x, y, l.cellWidth(), l.cellHeight(), tier, mx, my);
        }

        String state = switch (menu.status()) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 증강을 모두 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "동조 완료 · 보유 증강에 반영되었습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "진행 중인 동조를 먼저 완료하세요.";
            default -> "";
        };
        String footer = !state.isBlank()
                ? state
                : l.compact() ? selectionBonusLine() : "후보는 동조를 끝낼 때까지 유지됩니다.";
        if (!footer.isBlank()) {
            centered(g, clip(footer, l.visibleRight() - l.visibleLeft() - 20), l.centerX(), l.footerY(),
                    menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED ? GOOD
                            : menu.status() == AugmentDeviceMenu.STATUS_IDLE ? FAINT : BAD);
        }
    }

    private String selectionBonusLine() {
        int reroll = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID);
        boolean second = ClientAugmentProgressState.level(AugmentCatalog.SECOND_CHOICE_ID) > 0;
        if (reroll <= 0 && !second) return "";
        if (reroll > 0 && second) return "보유 효과 · 카드별 재추첨 " + reroll + "회 · 후보 2개 선택";
        if (reroll > 0) return "보유 효과 · 카드별 재추첨 " + reroll + "회";
        return "보유 효과 · 후보 2개 선택";
    }

    private void drawTierChoice(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(tier);
        int count = coreCount(tier);
        boolean enabled = count > 0;
        boolean compact = w < 138 || h < 46;

        if (tier == AugmentTier.LEGENDARY && !compact) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, withAlpha(color, 0x12));
        } else if (tier == AugmentTier.MYTHIC && !compact) {
            double wave = (Math.sin(System.currentTimeMillis() / 260.0) + 1.0) * 0.5;
            int alpha = 0x12 + (int) Math.round(wave * 0x10);
            g.fill(x - 3, y - 3, x + w + 3, y + h + 3, withAlpha(color, alpha));
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(color, 0x55));
        }

        g.fill(x, y, x + w, y + h, hover && enabled ? SURFACE_HI : SURFACE);
        g.outline(x, y, w, h, hover && enabled ? brighten(color, 24) : LINE_SOFT);
        g.fill(x, y, x + w, y + 2, enabled ? color : shade(color, 0.35));

        int socket = compact ? 22 : Math.min(34, h - 12);
        int sx = x + (compact ? 7 : 10);
        int sy = y + (h - socket) / 2;
        g.fill(sx, sy, sx + socket, sy + socket, SURFACE_DEEP);
        g.outline(sx, sy, socket, socket, enabled ? shade(color, 0.86) : LINE_SOFT);
        g.item(new ItemStack(AugmentCoreItems.item(tier)), sx + socket / 2 - 8, sy + socket / 2 - 8);

        int textX = sx + socket + (compact ? 6 : 10);
        int titleY = compact ? y + 5 : y + Math.max(7, h / 2 - 12);
        int reserve = compact ? 7 : 55;
        g.text(font, clip(tierLabel(tier), Math.max(20, x + w - reserve - textX)), textX, titleY,
                enabled ? TEXT : 0xFF858D90, false);
        g.text(font, clip("보유 " + count, Math.max(20, x + w - reserve - textX)), textX, titleY + 13,
                enabled ? MUTED : FAINT, false);

        if (!compact) {
            String state = enabled ? "선택" : "없음";
            g.text(font, state, x + w - 10 - font.width(state), y + h / 2 - 4,
                    enabled ? color : FAINT, false);
        }
    }

    private void drawOffer(GuiGraphicsExtractor g, int mx, int my) {
        OfferLayout l = offerLayout();
        AugmentTier selectedTier = menu.tierOrdinal() >= 0
                ? AugmentTier.values()[menu.tierOrdinal()]
                : AugmentTier.COMMON;
        int color = tierColor(selectedTier);
        int selectedCount = Integer.bitCount(menu.selectedMask());
        int rerollLevel = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID);

        centered(g, "증강 선택", l.centerX(), l.headerTop(), TEXT);
        String rule = tierLabel(selectedTier) + " 코어 · 3개 중 " + menu.selectionCount() + "개 선택";
        centered(g, clip(rule, l.visibleRight() - l.visibleLeft() - 20),
                l.centerX(), l.headerTop() + 14, color);

        if (!l.compact()) {
            String progress = menu.selectionCount() == 2
                    ? "선택 " + selectedCount + " / 2"
                    : "원하는 증강 하나를 고르세요";
            if (rerollLevel > 0) progress += " · 재추첨 " + menu.rerollsRemaining() + "회";
            centered(g, clip(progress, l.visibleRight() - l.visibleLeft() - 20),
                    l.centerX(), l.headerTop() + 27, MUTED);
        }

        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            int x = l.cardLeft() + slot * (l.cardWidth() + l.gap());
            drawAugmentCard(g, x, l.cardTop(), l.cardWidth(), l.cardHeight(),
                    slot, d, mx, my, l.compact());
        }

        String footer = rerollLevel > 0
                ? "카드 선택 · 카드별 재추첨 · Shift: 정확한 수치"
                : "카드 선택 · Shift: 정확한 수치";
        if (l.compact() && rerollLevel > 0) footer = "재추첨 " + menu.rerollsRemaining() + "회 · Shift 상세";
        centered(g, clip(footer, l.visibleRight() - l.visibleLeft() - 12),
                l.centerX(), l.footerY(), MUTED);
    }

    private void drawAugmentCard(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            int slot,
            AugmentDefinition d,
            int mx,
            int my,
            boolean compact
    ) {
        double reveal = revealProgress(slot);
        y += (int) Math.round((1.0 - reveal) * 10.0);

        boolean hover = inside(mx, my, x, y, w, h);
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean detail = hover && shiftDown();
        boolean tiny = h < 132;
        int color = tierColor(d.tier());
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);

        drawCardFrame(g, x, y, w, h, d.tier(), color, hover, selected);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, SURFACE_DEEP);
        g.fill(x + 6, y + 6, x + w - 6, y + 28, tint(TOP, color, 10));

        String kind = p.kind().label();
        g.text(font, clip(kind, Math.max(20, w - 18)), x + 9, y + 12,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : GOLD, false);
        if (selected && w >= 112) {
            String picked = "선택됨";
            g.text(font, picked, x + w - 9 - font.width(picked), y + 12, brighten(color, 45), false);
        }

        int iconBox = tiny ? 26 : compact ? 34 : 44;
        int iconY = y + (tiny ? 31 : compact ? 35 : 39);
        int iconX = x + (w - iconBox) / 2;

        drawAugmentSocket(g, iconX, iconY, iconBox, color, d.tier(), hover);
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()),
                iconX + iconBox / 2 - 8, iconY + iconBox / 2 - 8);

        int nameY = iconY + iconBox + (tiny ? 4 : compact ? 6 : 9);
        centered(g, clip(p.displayName(), Math.max(24, w - 18)), x + w / 2, nameY, TEXT);

        int now = menu.currentLevel(slot);
        int next = Math.min(now + 1, d.maxLevel());
        String level = now == 0
                ? "신규 · Lv." + next + "/" + d.maxLevel()
                : "Lv." + now + " → " + next + "/" + d.maxLevel();
        centered(g, clip(level, Math.max(24, w - 18)), x + w / 2, nameY + 12,
                now == 0 ? color : MUTED);

        boolean rerollOwned = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
        boolean rerollAction = rerollOwned && h >= 96;
        int actionY = y + h - 22;

        if (!tiny) {
            int dividerY = nameY + 29;
            g.fill(x + 10, dividerY, x + w - 10, dividerY + 1, LINE_SOFT);
            int descriptionTop = dividerY + 7;
            int descriptionBottom = rerollAction ? actionY - 5 : y + h - 8;
            int availableLines = Math.max(1, (descriptionBottom - descriptionTop) / 10);
            int lineLimit = Math.min(compact ? 4 : (detail ? 7 : 6), availableLines);
            drawWrapped(g, detail ? p.detail() : p.summary(),
                    x + 10, descriptionTop, Math.max(24, w - 20), lineLimit,
                    detail ? 0xFFDCE2E2 : 0xFFC7CFD0);
        } else if (!rerollAction) {
            g.text(font, clip(detail ? p.detail() : p.summary(), Math.max(24, w - 18)),
                    x + 9, y + h - 16, detail ? 0xFFDCE2E2 : 0xFFC7CFD0, false);
        }

        if (rerollAction) {
            boolean buttonHover = inside(mx, my, x + 8, actionY, w - 16, 15);
            boolean canReroll = menu.rerollsRemaining() > 0 && !selected;
            int line = canReroll ? (buttonHover ? color : shade(color, 0.72)) : LINE_SOFT;
            int fill = canReroll && buttonHover ? tint(SURFACE_HI, color, 18) : SURFACE;
            g.fill(x + 8, actionY, x + w - 8, actionY + 15, fill);
            g.outline(x + 8, actionY, w - 16, 15, line);
            String label = selected ? "선택 해제" : menu.rerollsRemaining() > 0 ? "↻ 재추첨" : "재추첨 소진";
            centered(g, clip(label, w - 24), x + w / 2, actionY + 3,
                    canReroll ? (buttonHover ? TEXT : color) : FAINT);
        }
    }

    private void drawAugmentSocket(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int size,
            int color,
            AugmentTier tier,
            boolean hover
    ) {
        if (tier == AugmentTier.LEGENDARY) {
            g.fill(x - 3, y - 3, x + size + 3, y + size + 3, withAlpha(color, 0x16));
        } else if (tier == AugmentTier.MYTHIC) {
            double wave = (Math.sin(System.currentTimeMillis() / 230.0) + 1.0) * 0.5;
            int alpha = 0x18 + (int) Math.round(wave * 0x12);
            g.fill(x - 4, y - 4, x + size + 4, y + size + 4, withAlpha(color, alpha));
            g.outline(x - 3, y - 3, size + 6, size + 6, withAlpha(color, 0x68));
        }
        g.fill(x, y, x + size, y + size, 0xFF0C1114);
        g.outline(x, y, size, size, hover ? brighten(color, 12) : shade(color, 0.76));
        g.outline(x + 3, y + 3, size - 6, size - 6, LINE_SOFT);
    }

    private void drawCardFrame(
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
        boolean roomy = w >= 145 && h >= 150;

        if (tier == AugmentTier.EPIC && roomy) {
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(color, 0x66));
            g.outline(x - 1, y - 1, w + 2, h + 2, withAlpha(brighten(color, 18), 0xA0));
        } else if (tier == AugmentTier.LEGENDARY && roomy) {
            g.fill(x - 4, y - 4, x + w + 4, y + h + 4, withAlpha(color, 0x18));
            g.outline(x - 4, y - 4, w + 8, h + 8, withAlpha(color, 0x36));
            g.outline(x - 2, y - 2, w + 4, h + 4, withAlpha(brighten(color, 26), 0xB8));
        } else if (tier == AugmentTier.MYTHIC && roomy) {
            double wave = (Math.sin(System.currentTimeMillis() / 230.0) + 1.0) * 0.5;
            int outerAlpha = 0x2A + (int) Math.round(wave * 0x18);
            int midAlpha = 0x58 + (int) Math.round(wave * 0x20);
            g.fill(x - 5, y - 5, x + w + 5, y + h + 5, withAlpha(color, 0x22));
            g.outline(x - 5, y - 5, w + 10, h + 10, withAlpha(color, outerAlpha));
            g.outline(x - 3, y - 3, w + 6, h + 6, withAlpha(brighten(color, 12), midAlpha));
            g.fill(x + 9, y - 3, x + w - 9, y - 1, withAlpha(brighten(color, 38), 0x96));
            g.fill(x + 9, y + h + 1, x + w - 9, y + h + 3, withAlpha(color, 0x78));
        } else if (tier.ordinal() >= AugmentTier.EPIC.ordinal()) {
            g.outline(x - 1, y - 1, w + 2, h + 2, withAlpha(color, 0x82));
        }

        g.fill(x, y, x + w, y + h, 0xFF0B1013);
        g.outline(x, y, w, h, selected ? brighten(color, 55) : hover ? color : shade(color, 0.54));
        g.outline(x + 3, y + 3, w - 6, h - 6, selected ? color : LINE_SOFT);
        if (selected) g.fill(x + 7, y + h - 7, x + w - 7, y + h - 5, color);
    }

    private void drawOwned(GuiGraphicsExtractor g, int mx, int my) {
        OwnedLayout l = ownedLayout();
        List<AugmentDefinition> owned = ownedDefinitions();
        int pages = Math.max(1, (owned.size() + l.pageSize() - 1) / l.pageSize());
        ownedPage = Math.max(0, Math.min(ownedPage, pages - 1));
        hoveredOwnedId = "";

        String title = "보유 증강";
        g.text(font, title, l.left(), l.headerY(), TEXT, false);
        String count = owned.size() + " / " + AugmentCatalog.allDefinitions().size();
        g.text(font, count, l.left() + font.width(title) + 8, l.headerY(), DEVICE_ACCENT, false);

        String help = l.width() >= 390 ? "마우스: 정보 · Shift: 정확한 수치" : "Shift: 상세";
        g.text(font, help, l.right() - font.width(help), l.headerY(), MUTED, false);

        int start = ownedPage * l.pageSize();
        int end = Math.min(owned.size(), start + l.pageSize());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % l.columns();
            int row = local / l.columns();
            int x = l.gridLeft() + col * (l.cellW() + l.gapX());
            int y = l.gridTop() + row * (l.cellH() + l.gapY());
            drawOwnedCell(g, x, y, l.cellW(), l.cellH(), owned.get(i), mx, my);
        }

        if (owned.isEmpty()) {
            centered(g, "아직 획득한 증강이 없습니다.", l.centerX(), l.gridTop() + 24, MUTED);
        }
        if (pages > 1) drawOwnedPager(g, l, ownedPage, pages, mx, my);

        if (!hoveredOwnedId.isBlank()) {
            AugmentDefinition d = AugmentCatalog.require(hoveredOwnedId);
            drawOwnedDetail(g, l, d, ClientAugmentProgressState.level(d.id()), shiftDown());
        } else {
            drawOwnedEmptyDetail(g, l);
        }
    }

    private void drawOwnedCell(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentDefinition d, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(d.tier());
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int level = ClientAugmentProgressState.level(d.id());

        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + w, y + 2, color);
        g.outline(x, y, w, h, hover ? shade(color, 0.88) : LINE_SOFT);

        if (w >= 52) {
            g.fill(x + 6, y + 6, x + 28, y + h - 5, SURFACE_DEEP);
            g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + 9, y + Math.max(7, (h - 16) / 2));
        }

        int textX = x + (w >= 52 ? 34 : 8);
        int typeReserve = w >= 108 ? 55 : 8;
        g.text(font, clip(p.displayName(), Math.max(20, w - (textX - x) - typeReserve)), textX, y + 6,
                hover ? TEXT : 0xFFD5DCDE, false);
        if (h >= 28) {
            g.text(font, "Lv." + level + "/" + d.maxLevel(), textX, y + 18, MUTED, false);
        }

        if (w >= 108) {
            String kind = p.kind().label();
            int kw = font.width(kind) + 10;
            int kx = x + w - kw - 7;
            int ky = y + Math.max(7, h / 2 - 7);
            g.fill(kx, ky, kx + kw, ky + 14,
                    p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? 0xFF171D20 : 0xFF2B2618);
            centered(g, kind, kx + kw / 2, ky + 3,
                    p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? FAINT : GOLD);
        }

        if (hover) hoveredOwnedId = d.id();
    }

    private void drawOwnedDetail(
            GuiGraphicsExtractor g,
            OwnedLayout l,
            AugmentDefinition d,
            int level,
            boolean detailed
    ) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int x = l.detailLeft();
        int y = l.detailY();
        int w = l.detailRight() - l.detailLeft();
        int color = tierColor(d.tier());

        g.fill(x, y, x + w, y + l.detailH(), 0xED0D1215);
        g.outline(x, y, w, l.detailH(), LINE);
        g.fill(x, y, x + 4, y + l.detailH(), color);

        int iconRoom = l.detailH() >= 42 ? 28 : 0;
        int textX = x + 10;
        if (iconRoom > 0) {
            g.fill(x + 10, y + 7, x + 38, y + 35, SURFACE_DEEP);
            g.outline(x + 10, y + 7, 28, 28, shade(color, 0.78));
            g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + 16, y + 13);
            textX = x + 46;
        }

        String head = p.displayName() + " · " + tierLabel(d.tier()) + " · " + p.kind().label()
                + " · Lv." + level + "/" + d.maxLevel();
        g.text(font, clip(head, Math.max(24, x + w - 10 - textX)), textX, y + 6, TEXT, false);

        if (l.detailH() >= 30) {
            String body = detailed ? p.detail() : p.summary();
            int bodyX = iconRoom > 0 ? textX : x + 10;
            int bodyY = y + (iconRoom > 0 ? 19 : 18);
            int bodyW = x + w - 10 - bodyX;
            int maxLines = Math.max(1, (l.detailH() - (bodyY - y) - 5) / 10);
            drawWrapped(g, body, bodyX, bodyY, Math.max(24, bodyW), maxLines,
                    detailed ? 0xFFDCE2E2 : 0xFFC7CFD0);
        }
    }

    private void drawOwnedEmptyDetail(GuiGraphicsExtractor g, OwnedLayout l) {
        int x = l.detailLeft();
        int y = l.detailY();
        int w = l.detailRight() - l.detailLeft();
        g.fill(x, y, x + w, y + l.detailH(), 0xED0D1215);
        g.outline(x, y, w, l.detailH(), LINE);
        String message = l.detailH() >= 44
                ? "증강에 마우스를 올리면 효과를 확인할 수 있습니다. Shift를 누르면 정확한 수치가 표시됩니다."
                : "마우스로 효과 확인 · Shift 상세";
        g.text(font, clip(message, w - 18), x + 9, y + Math.max(6, l.detailH() / 2 - 4), MUTED, false);
    }

    private void drawOwnedPager(GuiGraphicsExtractor g, OwnedLayout l, int page, int pages, int mx, int my) {
        boolean left = page > 0 && inside(mx, my, l.pagerLeftX(), l.pagerY(), 28, 16);
        boolean right = page + 1 < pages && inside(mx, my, l.pagerRightX(), l.pagerY(), 28, 16);
        g.text(font, "‹", l.pagerLeftX() + 10, l.pagerY() + 3, left ? TEXT : FAINT, false);
        centered(g, (page + 1) + " / " + pages, l.centerX(), l.pagerY() + 3, MUTED);
        g.text(font, "›", l.pagerRightX() + 10, l.pagerY() + 3, right ? TEXT : FAINT, false);
    }

    private void drawSynthesis(GuiGraphicsExtractor g, int mx, int my) {
        SynthesisLayout l = synthesisLayout();
        centered(g, "코어 합성", l.centerX(), l.headerY(), TEXT);
        if (l.showGuide()) {
            centered(g, clip("같은 등급 코어 3개를 다음 등급 코어 1개로 정제합니다.", l.width() - 18),
                    l.centerX(), l.headerY() + 14, MUTED);
        }

        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier tier = AugmentTier.values()[i];
            int col = i % l.columns();
            int row = i / l.columns();
            int x = l.gridLeft() + col * (l.cellW() + l.gapX());
            int y = l.gridTop() + row * (l.cellH() + l.gapY());
            boolean selected = synthesisTier == i;
            boolean hover = inside(mx, my, x, y, l.cellW(), l.cellH());
            int color = tierColor(tier);

            g.fill(x, y, x + l.cellW(), y + l.cellH(), selected || hover ? SURFACE_HI : SURFACE);
            g.outline(x, y, l.cellW(), l.cellH(), selected ? color : hover ? LINE_HI : LINE_SOFT);
            if (selected) g.fill(x + 5, y + l.cellH() - 3, x + l.cellW() - 5, y + l.cellH() - 1, color);
            centered(g, clip(tierLabel(tier), l.cellW() - 8), x + l.cellW() / 2,
                    y + Math.max(4, l.cellH() / 2 - 4), selected ? TEXT : MUTED);
        }

        AugmentTier from = AugmentTier.values()[Math.max(0, Math.min(synthesisTier, AugmentTier.values().length - 2))];
        AugmentTier to = from.next().orElse(AugmentTier.MYTHIC);
        int ownedCount = coreCount(from);

        int flowY = l.flowY();
        int fromX = l.centerX() - (l.compactFlow() ? 56 : 78);
        int toX = l.centerX() + (l.compactFlow() ? 40 : 62);
        drawCorePedestal(g, fromX, flowY, from, "×3", l.compactFlow());
        centered(g, "→", l.centerX(), flowY + 6, tierColor(to));
        drawCorePedestal(g, toX, flowY, to, "×1", l.compactFlow());

        if (l.flowLabelY() >= 0) {
            centered(g, tierLabel(from) + "  →  " + tierLabel(to), l.centerX(), l.flowLabelY(), TEXT);
        }

        String cost = l.compactFlow()
                ? "보유 " + ownedCount + " · 필요 3"
                : "보유 " + ownedCount + " · 정제 1회당 3개 필요";
        centered(g, clip(cost, l.width() - 16), l.centerX(), l.countY(),
                ownedCount >= 3 ? MUTED : BAD);

        drawButton(g, l.oneButtonX(), l.buttonY(), l.buttonW(), l.buttonH(),
                "1회 정제", mx, my, ownedCount >= 3, tierColor(to));
        drawButton(g, l.allButtonX(), l.allButtonY(), l.buttonW(), l.buttonH(),
                "모두 정제", mx, my, ownedCount >= 3, tierColor(to));

        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED) {
            centered(g, "정제 완료 ×" + Math.max(1, menu.lastOutputCount()), l.centerX(), l.statusY(), GOOD);
        } else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) {
            centered(g, "코어가 부족합니다.", l.centerX(), l.statusY(), BAD);
        }
    }

    private void drawCorePedestal(GuiGraphicsExtractor g, int x, int y, AugmentTier tier, String count, boolean compact) {
        int color = tierColor(tier);
        int size = compact ? 26 : 42;
        if (!compact) {
            g.fill(x - 6, y - 6, x + size + 6, y + size + 6, withAlpha(color, 0x10));
            g.outline(x - 4, y - 4, size + 8, size + 8, withAlpha(color, 0x42));
        }
        g.fill(x, y, x + size, y + size, SURFACE_DEEP);
        g.outline(x, y, size, size, shade(color, 0.82));
        g.item(new ItemStack(AugmentCoreItems.item(tier)), x + size / 2 - 8, y + size / 2 - 8);
        g.text(font, count, x + size + 5, y + size / 2 - 4, MUTED, false);
    }

    private void drawButton(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            String text,
            int mx,
            int my,
            boolean enabled,
            int accent
    ) {
        boolean hover = enabled && inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? tint(SURFACE_HI, accent, 12) : SURFACE);
        g.outline(x, y, w, h, enabled ? (hover ? accent : LINE) : LINE_SOFT);
        if (enabled) g.fill(x + 7, y + h - 3, x + w - 7, y + h - 1, shade(accent, hover ? 0.95 : 0.68));
        centered(g, clip(text, w - 10), x + w / 2, y + Math.max(4, h / 2 - 4),
                enabled ? (hover ? TEXT : MUTED) : FAINT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        double x = event.x() - leftPos;
        double y = event.y() - topPos;

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            OfferLayout l = offerLayout();
            boolean rerollOwned = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
            for (int slot = 0; slot < 3; slot++) {
                int cx = l.cardLeft() + slot * (l.cardWidth() + l.gap());
                int cy = l.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
                boolean selected = (menu.selectedMask() & (1 << slot)) != 0;

                if (rerollOwned && l.cardHeight() >= 96) {
                    int actionY = cy + l.cardHeight() - 22;
                    if (inside(x, y, cx + 8, actionY, l.cardWidth() - 16, 15)) {
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

                if (menu.candidate(slot) != null && inside(x, y, cx, cy, l.cardWidth(), l.cardHeight())) {
                    playClick(1.05F + slot * 0.04F);
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
                }
            }
            return true;
        }

        TabLayout tabs = tabLayout();
        if (inside(x, y, tabs.selectX(), tabs.y(), tabs.selectW(), 20)) {
            view = View.SELECT;
            playClick(1.0F);
            return true;
        }
        if (inside(x, y, tabs.ownedX(), tabs.y(), tabs.ownedW(), 20)) {
            view = View.OWNED;
            playClick(1.04F);
            return true;
        }
        if (menu.advanced() && inside(x, y, tabs.synthesisX(), tabs.y(), tabs.synthesisW(), 20)) {
            view = View.SYNTHESIS;
            playClick(1.08F);
            return true;
        }

        if (view == View.SELECT) {
            TierLayout l = tierLayout();
            for (AugmentTier tier : AugmentTier.values()) {
                int index = tier.ordinal();
                int col = index % l.columns();
                int row = index / l.columns();
                int xx = l.gridLeft() + col * (l.cellWidth() + l.gapX());
                int yy = l.gridTop() + row * (l.cellHeight() + l.gapY());
                if (inside(x, y, xx, yy, l.cellWidth(), l.cellHeight()) && coreCount(tier) > 0) {
                    playClick(0.92F + tier.ordinal() * 0.04F);
                    return send(AugmentDeviceMenu.beginButtonId(tier));
                }
            }
        } else if (view == View.OWNED) {
            OwnedLayout l = ownedLayout();
            List<AugmentDefinition> owned = ownedDefinitions();
            int pages = Math.max(1, (owned.size() + l.pageSize() - 1) / l.pageSize());
            if (pages > 1 && inside(x, y, l.pagerLeftX(), l.pagerY(), 28, 16) && ownedPage > 0) {
                ownedPage--;
                playClick(0.96F);
                return true;
            }
            if (pages > 1 && inside(x, y, l.pagerRightX(), l.pagerY(), 28, 16) && ownedPage + 1 < pages) {
                ownedPage++;
                playClick(1.04F);
                return true;
            }
        } else if (view == View.SYNTHESIS && menu.advanced()) {
            SynthesisLayout l = synthesisLayout();
            for (int i = 0; i < AugmentTier.values().length - 1; i++) {
                int col = i % l.columns();
                int row = i / l.columns();
                int xx = l.gridLeft() + col * (l.cellW() + l.gapX());
                int yy = l.gridTop() + row * (l.cellH() + l.gapY());
                if (inside(x, y, xx, yy, l.cellW(), l.cellH())) {
                    synthesisTier = i;
                    playClick(0.95F + i * 0.05F);
                    return true;
                }
            }

            AugmentTier tier = AugmentTier.values()[Math.max(0, Math.min(synthesisTier, AugmentTier.values().length - 2))];
            if (coreCount(tier) >= 3 && inside(x, y, l.oneButtonX(), l.buttonY(), l.buttonW(), l.buttonH())) {
                return send(AugmentDeviceMenu.upgradeOneButtonId(tier));
            }
            if (coreCount(tier) >= 3 && inside(x, y, l.allButtonX(), l.allButtonY(), l.buttonW(), l.buttonH())) {
                return send(AugmentDeviceMenu.upgradeAllButtonId(tier));
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private OfferLayout offerLayout() {
        VisibleBounds b = viewportBounds(8);
        int usableW = b.width();
        int usableH = b.height();
        int gap = usableW >= 620 ? 14 : usableW >= 480 ? 10 : 6;
        int rawCardW = Math.max(1, (usableW - gap * 2) / 3);
        int cardW = Math.min(OFFER_MAX_CARD_W, Math.max(42, rawCardW));
        if (cardW * 3 + gap * 2 > usableW) cardW = rawCardW;
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = b.left() + Math.max(0, (usableW - groupW) / 2);
        boolean compact = cardW < 145 || usableH < 270;
        int headerTop = b.top() + 5;
        int cardTop = headerTop + (compact ? 33 : 43);
        int footerY = b.bottom() - 11;
        int cardH = Math.min(OFFER_MAX_CARD_H, Math.max(92, footerY - cardTop - 6));
        if (cardTop + cardH > footerY - 3) cardH = Math.max(88, footerY - cardTop - 3);
        return new OfferLayout(
                b.left(), b.right(), b.top(), b.bottom(),
                cardLeft, cardTop, cardW, cardH, gap, headerTop, footerY, compact);
    }

    private TierLayout tierLayout() {
        VisibleBounds b = viewportBounds(8);
        int usableW = b.width();
        int usableH = b.height();
        int columns = usableW >= 430 ? 3 : usableW >= 180 ? 2 : 1;
        int rows = (AugmentTier.values().length + columns - 1) / columns;
        int gapX = usableW >= 520 ? 10 : 5;
        int gapY = usableH >= 260 ? 8 : 4;
        boolean compact = usableW < 430 || usableH < 285;
        int headerTop = b.top() + 27;
        boolean showGuide = usableH >= 190;
        boolean showBonus = !compact && usableH >= 300;
        int headerSpace = showBonus ? 42 : showGuide ? 27 : 14;
        int gridTop = headerTop + headerSpace;
        int footerY = b.bottom() - 8;
        int gridBudget = Math.max(1, footerY - gridTop - 5);
        int rawCellW = Math.max(1, (usableW - gapX * (columns - 1)) / columns);
        int cellW = Math.min(190, Math.max(72, rawCellW));
        int rawCellH = Math.max(1, (gridBudget - gapY * (rows - 1)) / rows);
        int cellH = Math.min(78, Math.max(24, rawCellH));
        int gridW = cellW * columns + gapX * (columns - 1);
        int gridLeft = b.left() + Math.max(0, (usableW - gridW) / 2);
        return new TierLayout(
                b.left(), b.right(), b.top(), b.bottom(),
                gridLeft, gridTop, cellW, cellH, gapX, gapY, columns,
                headerTop, footerY, compact);
    }

    private OwnedLayout ownedLayout() {
        VisibleBounds b = viewportBounds(8);
        int left = b.left() + 10;
        int right = b.right() - 10;
        int width = Math.max(40, right - left);
        int headerY = b.top() + 31;
        int gridTop = headerY + 20;

        int detailH = b.height() >= 245 ? 58 : b.height() >= 190 ? 40 : 28;
        int detailY = b.bottom() - detailH - 5;
        int pagerY = detailY - 19;
        int gridBottom = pagerY - 4;

        int columns = width >= 560 ? 3 : width >= 330 ? 2 : 1;
        int gapX = width >= 430 ? 8 : 5;
        int gapY = 5;
        int cellH = gridBottom - gridTop >= 70 ? 32 : 26;
        int cellW = Math.max(54, (width - gapX * (columns - 1)) / columns);
        int rows = Math.max(1, (gridBottom - gridTop + gapY) / (cellH + gapY));
        int pageSize = Math.max(1, columns * rows);
        int gridWidth = cellW * columns + gapX * (columns - 1);
        int gridLeft = left + Math.max(0, (width - gridWidth) / 2);
        int centerX = (left + right) / 2;

        return new OwnedLayout(
                left, right, width, centerX, headerY,
                gridLeft, gridTop, cellW, cellH, gapX, gapY,
                columns, rows, pageSize,
                pagerY, centerX - 48, centerX + 20,
                left, right, detailY, detailH);
    }

    private SynthesisLayout synthesisLayout() {
        VisibleBounds b = viewportBounds(8);
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
        int gapX = width >= 430 ? 7 : 4;
        int gapY = height >= 260 ? 5 : 3;
        int rawCellW = Math.max(1, (width - gapX * (columns - 1)) / columns);
        int cellW = Math.min(92, Math.max(compactFlow ? 40 : 48, rawCellW));
        if (cellW * columns + gapX * (columns - 1) > width) cellW = rawCellW;

        int buttonH = compactFlow ? 18 : 24;
        int buttonW = Math.min(96, Math.max(compactFlow ? 58 : 68, (width - 16) / 2));
        int oneButtonX = center - buttonW - 4;
        int allButtonX = center + 4;
        int buttonY;
        int allButtonY;
        int flowY;
        int flowLabelY;
        int countY;
        int cellH;

        if (compactFlow) {
            buttonY = b.bottom() - 8 - buttonH;
            allButtonY = buttonY;
            countY = buttonY - 12;
            flowY = countY - 25;
            flowLabelY = -1;
            int gridBottom = flowY - 5;
            int availableGrid = Math.max(rows * 8, gridBottom - gridTop - gapY * Math.max(0, rows - 1));
            cellH = Math.max(8, Math.min(18, availableGrid / rows));
        } else {
            cellH = height >= 260 ? 28 : 20;
            int gridEnd = gridTop + rows * cellH + Math.max(0, rows - 1) * gapY;
            flowY = gridEnd + (height >= 260 ? 20 : 10);
            flowLabelY = flowY + 48;
            countY = flowLabelY + 15;
            buttonY = countY + 18;
            allButtonY = buttonY;
            if (width < buttonW * 2 + 10) {
                oneButtonX = center - buttonW / 2;
                allButtonX = oneButtonX;
                allButtonY = buttonY + buttonH + 5;
            }
            int maxBottom = b.bottom() - 8;
            if (allButtonY + buttonH > maxBottom) {
                int shift = allButtonY + buttonH - maxBottom;
                buttonY -= shift;
                allButtonY -= shift;
                countY -= shift;
                flowLabelY -= shift;
                flowY -= shift;
            }
        }

        int gridW = cellW * columns + gapX * (columns - 1);
        int gridLeft = left + Math.max(0, (width - gridW) / 2);
        int statusY = Math.min(b.bottom() - 8, allButtonY + buttonH + 8);
        return new SynthesisLayout(
                left, right, width, center, headerY, showGuide, compactFlow,
                gridLeft, gridTop, cellW, cellH, gapX, gapY, columns,
                flowY, flowLabelY, countY,
                oneButtonX, allButtonX, buttonY, allButtonY,
                buttonW, buttonH, statusY);
    }

    private TabLayout tabLayout() {
        VisibleBounds b = viewportBounds(8);
        int gap = b.width() >= 300 ? 8 : 4;

        if (!menu.advanced()) {
            int selectW = Math.min(72, Math.max(52, (b.width() - gap) / 2));
            int ownedW = Math.min(88, Math.max(60, b.width() - gap - selectW));
            int total = selectW + gap + ownedW;
            int start = b.left() + Math.max(0, (b.width() - total) / 2);
            return new TabLayout(start, selectW, start + selectW + gap, ownedW, -1, 0, b.top() + 3);
        }

        int available = Math.max(90, b.width() - gap * 2);
        int each = Math.max(42, available / 3);
        int selectW = Math.min(72, each);
        int ownedW = Math.min(88, each);
        int synthesisW = Math.min(88, Math.max(42, available - selectW - ownedW));
        int total = selectW + ownedW + synthesisW + gap * 2;
        int start = b.left() + Math.max(0, (b.width() - total) / 2);
        return new TabLayout(
                start, selectW,
                start + selectW + gap, ownedW,
                start + selectW + gap + ownedW + gap, synthesisW,
                b.top() + 3);
    }

    private VisibleBounds viewportBounds(int margin) {
        int scaledW = minecraft == null ? W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? H : minecraft.getWindow().getGuiScaledHeight();
        int left = Math.max(margin, -leftPos + margin);
        int right = Math.min(W - margin, scaledW - leftPos - margin);
        int top = Math.max(margin, -topPos + margin);
        int bottom = Math.min(H - margin, scaledH - topPos - margin);
        if (right <= left) {
            left = margin;
            right = W - margin;
        }
        if (bottom <= top) {
            top = margin;
            bottom = H - margin;
        }
        return new VisibleBounds(left, right, top, bottom);
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
            try {
                owned.add(AugmentCatalog.require(id));
            } catch (RuntimeException ignored) {
            }
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
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch));
        }
    }

    private void drawWrapped(
            GuiGraphicsExtractor g,
            String value,
            int x,
            int y,
            int maxWidth,
            int maxLines,
            int color
    ) {
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

    private String clip(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        return ellipsize(value, maxWidth);
    }

    private String ellipsize(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
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

    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount);
        int gg = Math.min(255, ((color >> 8) & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor);
        int gg = (int) (((color >> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
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

    private record OwnedLayout(
            int left, int right, int width, int centerX, int headerY,
            int gridLeft, int gridTop, int cellW, int cellH, int gapX, int gapY,
            int columns, int rows, int pageSize, int pagerY, int pagerLeftX, int pagerRightX,
            int detailLeft, int detailRight, int detailY, int detailH
    ) { }

    private record SynthesisLayout(
            int left, int right, int width, int centerX, int headerY, boolean showGuide, boolean compactFlow,
            int gridLeft, int gridTop, int cellW, int cellH, int gapX, int gapY, int columns,
            int flowY, int flowLabelY, int countY,
            int oneButtonX, int allButtonX, int buttonY, int allButtonY,
            int buttonW, int buttonH, int statusY
    ) { }

    private record TabLayout(
            int selectX, int selectW, int ownedX, int ownedW,
            int synthesisX, int synthesisW, int y
    ) { }

    private enum View { SELECT, OWNED, SYNTHESIS }
}
