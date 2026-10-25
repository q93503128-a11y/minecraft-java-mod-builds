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

/** Compact three-card augment selection screen inspired by proven roguelite choice layouts. */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 380, H = 236;
    private static final int BG = 0xFF101419, SURFACE = 0xFF181E25, RAISED = 0xFF202832;
    private static final int HOVER = 0xFF28333F, SELECTED = 0xFF303C49, BORDER = 0xFF4B5866;
    private static final int TEXT = 0xFFF3F6F8, MUTED = 0xFFAAB4BE, FAINT = 0xFF737E89;
    private static final int GOOD = 0xFF8ED9A0, BAD = 0xFFE58C8C, ACTIVE = 0xFFF0D58A;
    private static final int CARD_X = 13, CARD_GAP = 9, CARD_W = 112, CARD_H = 112;
    private boolean upgradeMode;

    public AugmentDeviceScreen(AugmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        g.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF667482);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 31, RAISED);
        g.fill(leftPos + 3, topPos + 31, leftPos + imageWidth - 3, topPos + 33, 0xFF35404B);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        g.text(font, title.getString(), 14, 12, TEXT, false);
        String mode = upgradeMode ? "코어 정제" : "증강 동조";
        g.text(font, mode, imageWidth - 14 - font.width(mode), 12, FAINT, false);
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
            g.fill(x, tierY, x + 54, tierY + 19, selected ? SELECTED : hover ? HOVER : SURFACE);
            g.outline(x, tierY, 54, 19, selected ? tierColor(tier) : BORDER);
            if (selected) g.fill(x + 2, tierY + 16, x + 52, tierY + 18, tierColor(tier));
            centered(g, tierLabel(tier), x + 27, tierY + 6, selected ? TEXT : MUTED);
        }

        int cardY = menu.advanced() ? 88 : 67;
        boolean ready = menu.status() == AugmentDeviceMenu.STATUS_READY || menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED;
        for (int slot = 0; slot < 3; slot++) {
            if (ready && menu.candidate(slot) != null) drawCard(g, slot, cardY, mx, my);
            else drawEmptyCard(g, slot, cardY);
        }

        if (!ready) {
            centered(g, statusText(menu.status()), imageWidth / 2, cardY + 48, statusColor(menu.status()));
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

    private void drawCard(GuiGraphicsExtractor g, int slot, int y, int mx, int my) {
        AugmentDefinition d = menu.candidate(slot);
        int x = cardX(slot), tier = tierColor(d.tier());
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
        g.fill(x, y, x + CARD_W, y + CARD_H, selected ? SELECTED : hover ? HOVER : RAISED);
        g.outline(x, y, CARD_W, CARD_H, selected ? TEXT : tier);
        g.fill(x + 1, y + 1, x + CARD_W - 1, y + 5, tier);
        if (selected) g.outline(x + 3, y + 3, CARD_W - 6, CARD_H - 6, tier);

        ItemStack icon = new ItemStack(AugmentCoreItems.item(d.tier()));
        g.item(icon, x + 48, y + 16);
        String raw = d.canonicalNameKo();
        boolean active = raw.contains("(액티브");
        String name = raw.replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
        centered(g, name, x + CARD_W / 2, y + 39, TEXT);
        centered(g, tierLabel(d.tier()), x + CARD_W / 2, y + 54, tier);
        centered(g, active ? "액티브" : "상시 효과", x + CARD_W / 2, y + 68, active ? ACTIVE : MUTED);

        int now = menu.currentLevel(slot), next = Math.min(now + 1, d.maxLevel());
        centered(g, now == 0 ? "획득  Lv." + next : "Lv." + now + "  →  " + next, x + CARD_W / 2, y + 83, MUTED);
        centered(g, selected ? "선택" : "최대 Lv." + d.maxLevel(), x + CARD_W / 2, y + 98, selected ? TEXT : FAINT);
    }

    private void drawEmptyCard(GuiGraphicsExtractor g, int slot, int y) {
        int x = cardX(slot);
        g.fill(x, y, x + CARD_W, y + CARD_H, SURFACE);
        g.outline(x, y, CARD_W, CARD_H, 0xFF313A43);
        g.fill(x + 1, y + 1, x + CARD_W - 1, y + 4, 0xFF252D35);
        centered(g, "?", x + CARD_W / 2, y + 48, 0xFF48525C);
    }

    private void drawUpgrade(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "같은 등급 코어 3개를 다음 등급 코어 1개로 정제합니다.", imageWidth / 2, 65, MUTED);
        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier from = AugmentTier.values()[i], to = AugmentTier.values()[i + 1];
            int y = 84 + i * 25;
            g.fill(22, y, imageWidth - 22, y + 21, RAISED);
            g.outline(22, y, imageWidth - 44, 21, BORDER);
            g.fill(23, y + 1, 27, y + 20, tierColor(from));
            g.item(new ItemStack(AugmentCoreItems.item(from)), 33, y + 3);
            g.text(font, tierLabel(from) + " ×3", 54, y + 7, tierColor(from), false);
            g.text(font, "→", 121, y + 7, FAINT, false);
            g.item(new ItemStack(AugmentCoreItems.item(to)), 139, y + 3);
            g.text(font, tierLabel(to) + " ×1", 160, y + 7, tierColor(to), false);
            drawAction(g, 267, y + 2, 38, "1회", mx, my);
            drawAction(g, 311, y + 2, 45, "전부", mx, my);
        }
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED)
            centered(g, "코어 정제 완료 ×" + Math.max(1, menu.lastOutputCount()), imageWidth / 2, H - 17, GOOD);
        else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES)
            centered(g, "합성할 코어가 부족합니다.", imageWidth / 2, H - 17, BAD);
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 18);
        g.fill(x, y, x + w, y + 18, active ? SELECTED : hover ? HOVER : SURFACE);
        g.outline(x, y, w, 18, active ? 0xFFCAD4DD : BORDER);
        centered(g, text, x + w / 2, y + 5, active ? TEXT : MUTED);
    }

    private void drawAction(GuiGraphicsExtractor g, int x, int y, int w, String text, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 17);
        g.fill(x, y, x + w, y + 17, hover ? HOVER : SURFACE);
        g.outline(x, y, w, 17, hover ? 0xFFB7C4CF : BORDER);
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
            for (int slot = 0; slot < 3; slot++)
                if (menu.candidate(slot) != null && inside(x, y, cardX(slot), cardY, CARD_W, CARD_H))
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
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
    private static String tierLabel(AugmentTier t) { return switch (t) { case COMMON -> "일반"; case UNCOMMON -> "고급"; case RARE -> "희귀"; case EPIC -> "영웅"; case LEGENDARY -> "전설"; case MYTHIC -> "신화"; }; }
    private static int tierColor(AugmentTier t) { return switch (t) { case COMMON -> 0xFFC6CCD3; case UNCOMMON -> 0xFF72C986; case RARE -> 0xFF5BA5E3; case EPIC -> 0xFFB978E5; case LEGENDARY -> 0xFFE4B257; case MYTHIC -> 0xFFE16161; }; }
    private static String statusText(int s) { return switch (s) { case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다."; case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 모든 증강을 완성했습니다."; case AugmentDeviceMenu.STATUS_ACQUIRED -> "증강을 획득했습니다."; case AugmentDeviceMenu.STATUS_INVALID -> "선택을 완료할 수 없습니다. 다시 시도하세요."; case AugmentDeviceMenu.STATUS_UPGRADED -> "코어 정제가 완료되었습니다."; case AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> "합성할 코어가 부족합니다."; default -> "증강 코어를 선택하세요."; }; }
    private static int statusColor(int s) { return switch (s) { case AugmentDeviceMenu.STATUS_ACQUIRED, AugmentDeviceMenu.STATUS_UPGRADED -> GOOD; case AugmentDeviceMenu.STATUS_NO_CORE, AugmentDeviceMenu.STATUS_INVALID, AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> BAD; default -> MUTED; }; }
}
