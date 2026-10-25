package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Three-card augment picker with dark glass, tier light and beveled metal framing. */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 380, H = 236;
    private static final int BG = 0xFF0E1319, PANEL = 0xFF141B22, PANEL_TOP = 0xFF1E2832;
    private static final int RAISED = 0xFF202A34, HOVER = 0xFF2B3945, SELECTED = 0xFF33424E;
    private static final int METAL_DARK = 0xFF26313B, METAL = 0xFF566674, METAL_LIGHT = 0xFFA9BAC8;
    private static final int GLASS = 0xFF080D12, GLASS_TOP = 0xFF111A22;
    private static final int TEXT = 0xFFF3F7FA, MUTED = 0xFFAAB5BE, FAINT = 0xFF72808B;
    private static final int GOOD = 0xFF8ED9A0, BAD = 0xFFE58C8C, ACTIVE = 0xFFF0D58A;
    private static final int CARD_X = 13, CARD_GAP = 9, CARD_W = 112, CARD_H = 112;
    private boolean upgradeMode;

    public AugmentDeviceScreen(AugmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int l = leftPos, t = topPos;
        g.fill(l + 5, t + 6, l + imageWidth + 5, t + imageHeight + 6, 0x76000000);
        g.fill(l - 2, t - 2, l + imageWidth + 2, t + imageHeight + 2, 0xFF090D12);
        g.fill(l, t, l + imageWidth, t + imageHeight, METAL_DARK);
        g.outline(l, t, imageWidth, imageHeight, METAL);
        g.fill(l + 2, t + 2, l + imageWidth - 2, t + imageHeight - 2, BG);
        g.fill(l + 3, t + 3, l + imageWidth - 3, t + 4, 0xFF7C8C99);
        g.fill(l + 4, t + 4, l + imageWidth - 4, t + 14, PANEL_TOP);
        g.fill(l + 4, t + 14, l + imageWidth - 4, t + 29, 0xFF182129);
        g.fill(l + 4, t + 29, l + imageWidth - 4, t + 33, 0xFF10161C);
        g.fill(l + 4, t + 32, l + imageWidth - 4, t + 33, 0xFF52616D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        g.text(font, title.getString(), 14, 11, TEXT, false);
        String mode = upgradeMode ? "코어 정제" : "증강 동조";
        g.text(font, mode, imageWidth - 14 - font.width(mode), 11, 0xFFD4DEE6, false);
        if (menu.advanced()) drawTabs(g, mx, my);
        if (upgradeMode && menu.advanced()) drawUpgrade(g, mx, my);
        else drawSelection(g, mx, my);
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        drawTab(g, 14, 37, 104, "증강 선택", !upgradeMode, mx, my);
        drawTab(g, 124, 37, 104, "코어 합성", upgradeMode, mx, my);
    }

    private void drawSelection(GuiGraphicsExtractor g, int mx, int my) {
        int tierY = menu.advanced() ? 61 : 40;
        for (AugmentTier tier : AugmentTier.values()) {
            int x = 16 + tier.ordinal() * 58;
            boolean selected = menu.tierOrdinal() == tier.ordinal();
            boolean hover = inside(mx, my, x, tierY, 54, 19);
            drawTierChip(g, x, tierY, 54, 19, tier, selected, hover);
            centered(g, tierLabel(tier), x + 27, tierY + 6, selected ? TEXT : MUTED);
        }

        int cardY = menu.advanced() ? 88 : 67;
        boolean ready = menu.status() == AugmentDeviceMenu.STATUS_READY
                || menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED;
        for (int slot = 0; slot < 3; slot++) {
            if (ready && menu.candidate(slot) != null) drawCard(g, slot, cardY, mx, my);
            else drawEmptyCard(g, slot, cardY);
        }

        if (!ready) {
            centered(g, statusText(menu.status()), imageWidth / 2, cardY + 47, statusColor(menu.status()));
            if (menu.status() == AugmentDeviceMenu.STATUS_IDLE) {
                centered(g, "코어 등급을 고르면 세 가지 증강이 나타납니다.", imageWidth / 2, cardY + 64, MUTED);
            }
        }
        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            centered(g, menu.selectionCount() == 2 ? "서로 다른 증강 2개를 선택" : "원하는 증강을 선택", imageWidth / 2, H - 18, MUTED);
        } else if (menu.status() != AugmentDeviceMenu.STATUS_IDLE) {
            centered(g, statusText(menu.status()), imageWidth / 2, H - 18, statusColor(menu.status()));
        }
    }

    private void drawTierChip(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier, boolean selected, boolean hover) {
        int c = tierColor(tier);
        if (selected) g.outline(x - 1, y - 1, w + 2, h + 2, alpha(c, 72));
        g.fill(x, y, x + w, y + h, METAL_DARK);
        g.outline(x, y, w, h, selected ? brighten(c, 42) : hover ? brighten(c, 14) : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, selected ? tint(SELECTED, c, 24) : hover ? HOVER : PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, alpha(brighten(c, 60), selected ? 185 : 60));
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, shade(c, 0.48));
        if (selected) g.fill(x + 7, y + h - 3, x + w - 7, y + h - 2, c);
    }

    private void drawCard(GuiGraphicsExtractor g, int slot, int y, int mx, int my) {
        AugmentDefinition d = menu.candidate(slot);
        int x = cardX(slot), tier = tierColor(d.tier());
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
        drawAugmentFrame(g, x, y, CARD_W, CARD_H, d.tier(), selected, hover);

        int wellX = x + 40, wellY = y + 10;
        g.fill(wellX, wellY, wellX + 32, wellY + 30, 0xFF05090D);
        g.outline(wellX, wellY, 32, 30, shade(tier, 0.58));
        g.fill(wellX + 2, wellY + 2, wellX + 30, wellY + 4, alpha(brighten(tier, 55), 90));
        g.item(new ItemStack(AugmentCoreItems.item(d.tier())), x + 48, y + 16);

        String raw = d.canonicalNameKo();
        boolean active = raw.contains("(액티브");
        String name = raw.replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
        centered(g, name, x + CARD_W / 2, y + 42, TEXT);
        drawBadge(g, x + 30, y + 55, 52, 13, tierLabel(d.tier()), tier, true);
        drawBadge(g, x + 29, y + 72, 54, 12, active ? "액티브" : "상시 효과", active ? ACTIVE : MUTED, false);

        int now = menu.currentLevel(slot), next = Math.min(now + 1, d.maxLevel());
        centered(g, now == 0 ? "획득  Lv." + next : "Lv." + now + "  →  " + next, x + CARD_W / 2, y + 88, MUTED);
        centered(g, selected ? "선택됨" : "최대 Lv." + d.maxLevel(), x + CARD_W / 2, y + 101, selected ? brighten(tier, 80) : FAINT);
    }

    private void drawAugmentFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier, boolean selected, boolean hover) {
        int c = tierColor(tier);
        int hi = brighten(c, tier.ordinal() >= AugmentTier.EPIC.ordinal() ? 90 : 56);
        int lo = shade(c, 0.46);
        int surface = selected ? tint(SELECTED, c, 18) : hover ? tint(HOVER, c, 10) : GLASS;
        int glow = selected ? 118 : hover ? 72 : 36 + tier.ordinal() * 4;

        g.outline(x - 3, y - 3, w + 6, h + 6, alpha(c, Math.max(18, glow / 3)));
        g.outline(x - 2, y - 2, w + 4, h + 4, alpha(c, Math.max(24, glow / 2)));
        g.outline(x - 1, y - 1, w + 2, h + 2, alpha(hi, glow));
        g.fill(x, y, x + w, y + h, 0xFF080B0F);
        g.outline(x, y, w, h, lo);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, METAL_DARK);
        g.outline(x + 3, y + 3, w - 6, h - 6, hi);
        g.fill(x + 5, y + 5, x + w - 5, y + h - 5, surface);
        g.fill(x + 6, y + 6, x + w - 6, y + 12, GLASS_TOP);
        g.fill(x + 7, y + 6, x + w - 7, y + 7, alpha(hi, selected ? 108 : hover ? 76 : 42));
        g.fill(x + 7, y + h - 7, x + w - 7, y + h - 6, alpha(lo, 190));
        cutCorners(g, x, y, w, h);
        drawCornerBrackets(g, x, y, w, h, hi, c, tier.ordinal());
        if (selected) g.outline(x + 7, y + 7, w - 14, h - 14, alpha(brighten(c, 96), 150));
    }

    private void cutCorners(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + 3, y + 1, BG); g.fill(x, y, x + 1, y + 3, BG);
        g.fill(x + w - 3, y, x + w, y + 1, BG); g.fill(x + w - 1, y, x + w, y + 3, BG);
        g.fill(x, y + h - 1, x + 3, y + h, BG); g.fill(x, y + h - 3, x + 1, y + h, BG);
        g.fill(x + w - 3, y + h - 1, x + w, y + h, BG); g.fill(x + w - 1, y + h - 3, x + w, y + h, BG);
    }

    private void drawCornerBrackets(GuiGraphicsExtractor g, int x, int y, int w, int h, int hi, int c, int tierIndex) {
        int len = tierIndex >= 4 ? 9 : 7;
        int x0 = x + 4, x1 = x + w - 5, y0 = y + 4, y1 = y + h - 5;
        g.fill(x0, y0, x0 + len, y0 + 1, hi); g.fill(x0, y0, x0 + 1, y0 + len, hi);
        g.fill(x1 - len + 1, y0, x1 + 1, y0 + 1, hi); g.fill(x1, y0, x1 + 1, y0 + len, hi);
        int low = shade(c, 0.72);
        g.fill(x0, y1, x0 + len, y1 + 1, low); g.fill(x0, y1 - len + 1, x0 + 1, y1 + 1, low);
        g.fill(x1 - len + 1, y1, x1 + 1, y1 + 1, low); g.fill(x1, y1 - len + 1, x1 + 1, y1 + 1, low);
    }

    private void drawBadge(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int accent, boolean luminous) {
        g.fill(x, y, x + w, y + h, 0xFF080B0F);
        g.outline(x, y, w, h, luminous ? shade(accent, 0.70) : 0xFF39444D);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, tint(0xFF11171D, accent, luminous ? 15 : 7));
        if (luminous) g.fill(x + 2, y + 2, x + w - 2, y + 3, alpha(brighten(accent, 48), 74));
        centered(g, text, x + w / 2, y + 3, luminous ? brighten(accent, 55) : accent);
    }

    private void drawEmptyCard(GuiGraphicsExtractor g, int slot, int y) {
        int x = cardX(slot);
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, 0xFF111820);
        g.outline(x - 1, y - 1, CARD_W + 2, CARD_H + 2, 0xFF27313A);
        g.fill(x + 2, y + 2, x + CARD_W - 2, y + CARD_H - 2, GLASS);
        g.outline(x + 3, y + 3, CARD_W - 6, CARD_H - 6, 0xFF303A43);
        g.fill(x + 5, y + 5, x + CARD_W - 5, y + 11, GLASS_TOP);
        centered(g, "◆", x + CARD_W / 2, y + 43, 0xFF34404A);
        centered(g, "동조 대기", x + CARD_W / 2, y + 61, 0xFF4B5862);
    }

    private void drawUpgrade(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "같은 등급 코어 3개를 다음 등급 코어 1개로 정제합니다.", imageWidth / 2, 65, MUTED);
        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier from = AugmentTier.values()[i], to = AugmentTier.values()[i + 1];
            int y = 84 + i * 25;
            drawUpgradeRow(g, 22, y, imageWidth - 44, 21, from, to, mx, my);
            g.item(new ItemStack(AugmentCoreItems.item(from)), 33, y + 3);
            g.text(font, tierLabel(from) + " ×3", 54, y + 7, brighten(tierColor(from), 38), false);
            g.text(font, "→", 121, y + 7, FAINT, false);
            g.item(new ItemStack(AugmentCoreItems.item(to)), 139, y + 3);
            g.text(font, tierLabel(to) + " ×1", 160, y + 7, brighten(tierColor(to), 38), false);
            drawAction(g, 267, y + 2, 38, "1회", mx, my);
            drawAction(g, 311, y + 2, 45, "전부", mx, my);
        }
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED)
            centered(g, "코어 정제 완료 ×" + Math.max(1, menu.lastOutputCount()), imageWidth / 2, H - 17, GOOD);
        else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES)
            centered(g, "합성할 코어가 부족합니다.", imageWidth / 2, H - 17, BAD);
    }

    private void drawUpgradeRow(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier from, AugmentTier to, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, METAL_DARK);
        g.outline(x, y, w, h, hover ? METAL_LIGHT : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, hover ? HOVER : 0xFF111820);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, alpha(brighten(tierColor(to), 46), hover ? 100 : 52));
        g.fill(x + 2, y + 2, x + 5, y + h - 2, tierColor(from));
        g.fill(x + w - 5, y + 2, x + w - 2, y + h - 2, tierColor(to));
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 18);
        g.fill(x, y, x + w, y + 18, METAL_DARK);
        g.outline(x, y, w, 18, active ? 0xFFCAD7E1 : hover ? 0xFF7F909E : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 16, active ? SELECTED : hover ? HOVER : PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, active ? 0xFF7E93A3 : 0xFF36434E);
        if (active) g.fill(x + 8, y + 15, x + w - 8, y + 17, 0xFFB8CAD7);
        centered(g, text, x + w / 2, y + 5, active ? TEXT : MUTED);
    }

    private void drawAction(GuiGraphicsExtractor g, int x, int y, int w, String text, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 17);
        g.fill(x, y, x + w, y + 17, METAL_DARK);
        g.outline(x, y, w, 17, hover ? METAL_LIGHT : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 15, hover ? HOVER : PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, hover ? 0xFF8195A4 : 0xFF34414C);
        centered(g, text, x + w / 2, y + 5, hover ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double x = event.x() - leftPos, y = event.y() - topPos;
        if (menu.advanced()) {
            if (inside(x, y, 14, 37, 104, 18)) { upgradeMode = false; return true; }
            if (inside(x, y, 124, 37, 104, 18)) { upgradeMode = true; return true; }
        }
        if (upgradeMode && menu.advanced()) {
            for (int i = 0; i < AugmentTier.values().length - 1; i++) {
                int rowY = 84 + i * 25;
                AugmentTier tier = AugmentTier.values()[i];
                if (inside(x, y, 267, rowY + 2, 38, 17)) return send(AugmentDeviceMenu.upgradeOneButtonId(tier));
                if (inside(x, y, 311, rowY + 2, 45, 17)) return send(AugmentDeviceMenu.upgradeAllButtonId(tier));
            }
            return super.mouseClicked(event, doubleClick);
        }
        int tierY = menu.advanced() ? 61 : 40;
        for (AugmentTier tier : AugmentTier.values()) {
            int tx = 16 + tier.ordinal() * 58;
            if (inside(x, y, tx, tierY, 54, 19)) return send(AugmentDeviceMenu.beginButtonId(tier));
        }
        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            int cardY = menu.advanced() ? 88 : 67;
            for (int slot = 0; slot < 3; slot++) {
                if (menu.candidate(slot) != null && inside(x, y, cardX(slot), cardY, CARD_W, CARD_H))
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean send(int id) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, id)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private static int cardX(int slot) { return CARD_X + slot * (CARD_W + CARD_GAP); }
    private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) { g.text(font, s, x - font.width(s) / 2, y, color, false); }
    private static boolean inside(double x, double y, int l, int t, int w, int h) { return x >= l && x < l + w && y >= t && y < t + h; }
    private static int alpha(int argb, int a) { return (clamp(a) << 24) | (argb & 0x00FFFFFF); }
    private static int brighten(int argb, int amount) {
        int r = clamp(((argb >> 16) & 0xFF) + amount), g = clamp(((argb >> 8) & 0xFF) + amount), b = clamp((argb & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int shade(int argb, double f) {
        int r = clamp((int) (((argb >> 16) & 0xFF) * f)), g = clamp((int) (((argb >> 8) & 0xFF) * f)), b = clamp((int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int tint(int base, int tint, int p) {
        int inv = 100 - p;
        int r = (((base >> 16) & 0xFF) * inv + ((tint >> 16) & 0xFF) * p) / 100;
        int g = (((base >> 8) & 0xFF) * inv + ((tint >> 8) & 0xFF) * p) / 100;
        int b = ((base & 0xFF) * inv + (tint & 0xFF) * p) / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static String tierLabel(AugmentTier t) {
        return switch (t) {
            case COMMON -> "일반";
            case UNCOMMON -> "고급";
            case RARE -> "희귀";
            case EPIC -> "영웅";
            case LEGENDARY -> "전설";
            case MYTHIC -> "신화";
        };
    }

    private static int tierColor(AugmentTier t) {
        return switch (t) {
            case COMMON -> 0xFFC6CCD3;
            case UNCOMMON -> 0xFF55D28B;
            case RARE -> 0xFF4A9FF2;
            case EPIC -> 0xFFB35FE3;
            case LEGENDARY -> 0xFFF0B33F;
            case MYTHIC -> 0xFFEC4E78;
        };
    }

    private static String statusText(int s) {
        return switch (s) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 모든 증강을 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "증강을 획득했습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "선택을 완료할 수 없습니다. 다시 시도하세요.";
            case AugmentDeviceMenu.STATUS_UPGRADED -> "코어 정제가 완료되었습니다.";
            case AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> "합성할 코어가 부족합니다.";
            default -> "증강 코어를 선택하세요.";
        };
    }

    private static int statusColor(int s) {
        return switch (s) {
            case AugmentDeviceMenu.STATUS_ACQUIRED, AugmentDeviceMenu.STATUS_UPGRADED -> GOOD;
            case AugmentDeviceMenu.STATUS_NO_CORE, AugmentDeviceMenu.STATUS_INVALID, AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> BAD;
            default -> MUTED;
        };
    }
}
