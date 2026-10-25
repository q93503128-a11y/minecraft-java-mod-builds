package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.menu.AugmentStorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact storage surface shared by Pockets and Remote Storage. */
public final class AugmentStorageScreen extends AbstractContainerScreen<AugmentStorageMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 264;
    private static final int PANEL = 0xFF15191F;
    private static final int INNER = 0xFF20262E;
    private static final int SLOT = 0xFF2B323B;
    private static final int SLOT_LOCKED = 0xFF1B2026;
    private static final int TEXT = 0xFFF1F4F6;
    private static final int MUTED = 0xFF9EA8B3;

    public AugmentStorageScreen(AugmentStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF59616C);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, INNER);
        for (int slot = 0; slot < AugmentStorageMenu.POCKET_SLOTS; slot++) {
            drawSlot(graphics, 7 + slot * 18, 21, slot < menu.pocketUnlocked());
        }
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = row * 9 + column;
                drawSlot(graphics, 7 + column * 18, 55 + row * 18, menu.remoteSlotActive(slot));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics, 7 + column * 18, 183 + row * 18, true);
        }
        for (int column = 0; column < 9; column++) drawSlot(graphics, 7 + column * 18, 241, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, "주머니", 7, 8, menu.pocketUnlocked() > 0 ? TEXT : MUTED, false);
        String remoteLabel = menu.hasRemote()
                ? "원격 창고 · " + menu.remoteMemberCount() + "명"
                : "원격 창고";
        graphics.text(font, remoteLabel, 7, 42, menu.hasRemote() ? TEXT : MUTED, false);
        if (menu.remotePageCount() > 1) {
            String page = (menu.remotePage() + 1) + " / " + menu.remotePageCount();
            graphics.text(font, page, 129 - font.width(page) / 2, 42, MUTED, false);
            drawPageButton(graphics, 142, 39, "‹", menu.remotePage() > 0, mouseX, mouseY);
            drawPageButton(graphics, 158, 39, "›", menu.remotePage() + 1 < menu.remotePageCount(), mouseX, mouseY);
        }
        graphics.text(font, "소지품", 7, 170, TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && menu.remotePageCount() > 1) {
            double x = event.x() - leftPos;
            double y = event.y() - topPos;
            if (inside(x, y, 142, 39, 14, 13) && menu.remotePage() > 0) {
                return sendMenuButton(AugmentStorageMenu.previousPageButtonId());
            }
            if (inside(x, y, 158, 39, 14, 13) && menu.remotePage() + 1 < menu.remotePageCount()) {
                return sendMenuButton(AugmentStorageMenu.nextPageButtonId());
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, boolean active) {
        int sx = leftPos + x - 1;
        int sy = topPos + y - 1;
        graphics.fill(sx, sy, sx + 18, sy + 18, active ? SLOT : SLOT_LOCKED);
        graphics.outline(sx, sy, 18, 18, active ? 0xFF4A5562 : 0xFF2A3037);
    }

    private void drawPageButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String label,
            boolean enabled,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, 14, 13);
        graphics.fill(x, y, x + 14, y + 13, hovered ? 0xFF3B4652 : 0xFF293039);
        graphics.outline(x, y, 14, 13, enabled ? 0xFF65717E : 0xFF353C44);
        graphics.text(font, label, x + 7 - font.width(label) / 2, y + 3, enabled ? TEXT : MUTED, false);
    }

    private boolean sendMenuButton(int buttonId) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, buttonId)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        return true;
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
