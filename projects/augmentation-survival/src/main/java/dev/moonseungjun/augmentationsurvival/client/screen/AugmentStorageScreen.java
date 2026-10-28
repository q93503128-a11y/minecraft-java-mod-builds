package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.menu.AugmentStorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact storage surface shared by Pockets and Remote Storage. */
public final class AugmentStorageScreen extends AbstractContainerScreen<AugmentStorageMenu> {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 160;
    private static final int DIVIDER_X = 159;
    private static final int REMOTE_LABEL_Y = 42;
    private static final int PLAYER_LABEL_X = AugmentStorageMenu.PLAYER_X;
    private static final int PAGE_PREVIOUS_X = 132;
    private static final int PAGE_NEXT_X = 146;
    private static final int PAGE_BUTTON_Y = 39;
    private static final int PAGE_BUTTON_SIZE = 12;

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
        graphics.fill(
                leftPos + DIVIDER_X,
                topPos + 4,
                leftPos + DIVIDER_X + 1,
                topPos + imageHeight - 4,
                0xFF39414A
        );

        for (int slot = 0; slot < AugmentStorageMenu.POCKET_SLOTS; slot++) {
            drawSlot(
                    graphics,
                    AugmentStorageMenu.STORAGE_X + slot * AugmentStorageMenu.GRID_STEP,
                    AugmentStorageMenu.POCKET_Y,
                    slot < menu.pocketUnlocked()
            );
        }

        if (menu.hasRemote()) {
            for (int row = 0; row < 6; row++) {
                for (int column = 0; column < 9; column++) {
                    int slot = row * 9 + column;
                    drawSlot(
                            graphics,
                            AugmentStorageMenu.STORAGE_X + column * AugmentStorageMenu.GRID_STEP,
                            AugmentStorageMenu.REMOTE_Y + row * AugmentStorageMenu.GRID_STEP,
                            menu.remoteSlotActive(slot)
                    );
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                        graphics,
                        AugmentStorageMenu.PLAYER_X + column * AugmentStorageMenu.GRID_STEP,
                        AugmentStorageMenu.PLAYER_Y + row * AugmentStorageMenu.GRID_STEP,
                        true
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(
                    graphics,
                    AugmentStorageMenu.PLAYER_X + column * AugmentStorageMenu.GRID_STEP,
                    AugmentStorageMenu.HOTBAR_Y,
                    true
            );
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(
                font,
                "주머니",
                AugmentStorageMenu.STORAGE_X,
                8,
                menu.pocketUnlocked() > 0 ? TEXT : MUTED,
                false
        );

        if (menu.hasRemote()) {
            String remoteLabel = "원격 창고 · " + menu.remoteMemberCount() + "명";
            graphics.text(font, remoteLabel, AugmentStorageMenu.STORAGE_X, REMOTE_LABEL_Y, TEXT, false);
            if (menu.remotePageCount() > 1) {
                String page = (menu.remotePage() + 1) + " / " + menu.remotePageCount();
                graphics.text(font, page, 114 - font.width(page) / 2, REMOTE_LABEL_Y, MUTED, false);
                int localMouseX = mouseX - leftPos;
                int localMouseY = mouseY - topPos;
                drawPageButton(
                        graphics,
                        PAGE_PREVIOUS_X,
                        PAGE_BUTTON_Y,
                        "‹",
                        menu.remotePage() > 0,
                        localMouseX,
                        localMouseY
                );
                drawPageButton(
                        graphics,
                        PAGE_NEXT_X,
                        PAGE_BUTTON_Y,
                        "›",
                        menu.remotePage() + 1 < menu.remotePageCount(),
                        localMouseX,
                        localMouseY
                );
            }
        }

        graphics.text(font, "소지품", PLAYER_LABEL_X, 8, TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && menu.hasRemote() && menu.remotePageCount() > 1) {
            double x = event.x() - leftPos;
            double y = event.y() - topPos;
            if (inside(x, y, PAGE_PREVIOUS_X, PAGE_BUTTON_Y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)
                    && menu.remotePage() > 0) {
                return sendMenuButton(AugmentStorageMenu.previousPageButtonId());
            }
            if (inside(x, y, PAGE_NEXT_X, PAGE_BUTTON_Y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE)
                    && menu.remotePage() + 1 < menu.remotePageCount()) {
                return sendMenuButton(AugmentStorageMenu.nextPageButtonId());
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, boolean active) {
        int sx = leftPos + x - 1;
        int sy = topPos + y - 1;
        int size = AugmentStorageMenu.GRID_STEP;
        graphics.fill(sx, sy, sx + size, sy + size, active ? SLOT : SLOT_LOCKED);
        graphics.outline(sx, sy, size, size, active ? 0xFF4A5562 : 0xFF2A3037);
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
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE);
        graphics.fill(
                x,
                y,
                x + PAGE_BUTTON_SIZE,
                y + PAGE_BUTTON_SIZE,
                hovered ? 0xFF3B4652 : 0xFF293039
        );
        graphics.outline(
                x,
                y,
                PAGE_BUTTON_SIZE,
                PAGE_BUTTON_SIZE,
                enabled ? 0xFF65717E : 0xFF353C44
        );
        graphics.text(
                font,
                label,
                x + PAGE_BUTTON_SIZE / 2 - font.width(label) / 2,
                y + 2,
                enabled ? TEXT : MUTED,
                false
        );
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
