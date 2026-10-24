package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Functional card-selection screen for the augmentation workstations.
 *
 * <p>This establishes the interaction layout only. Dedicated art, final typography, card effects,
 * and motion polish are intentionally deferred to the visual pass rather than being faked here.</p>
 */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int SCREEN_WIDTH = 356;
    private static final int SCREEN_HEIGHT = 220;
    private static final int PANEL = 0xFF15191F;
    private static final int PANEL_INNER = 0xFF20262E;
    private static final int PANEL_MUTED = 0xFF2A313A;
    private static final int TEXT = 0xFFF1F4F6;
    private static final int MUTED_TEXT = 0xFFADB5BF;
    private static final int SUCCESS_TEXT = 0xFF80D993;
    private static final int ERROR_TEXT = 0xFFE47F7F;

    private boolean upgradeMode;

    public AugmentDeviceScreen(
            AugmentDeviceMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF59616C);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + 31, PANEL_INNER);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawCentered(graphics, title.getString(), imageWidth / 2, 12, TEXT);

        if (menu.advanced()) {
            drawModeTabs(graphics, mouseX - leftPos, mouseY - topPos);
        }

        if (upgradeMode && menu.advanced()) {
            drawUpgradeView(graphics, mouseX - leftPos, mouseY - topPos);
        } else {
            drawSelectionView(graphics, mouseX - leftPos, mouseY - topPos);
        }
    }

    private void drawModeTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int y = 34;
        drawTab(graphics, 16, y, 96, 19, "증강 선택", !upgradeMode, mouseX, mouseY);
        drawTab(graphics, 118, y, 96, 19, "코어 합성", upgradeMode, mouseX, mouseY);
    }

    private void drawSelectionView(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int tierY = menu.advanced() ? 58 : 38;
        for (AugmentTier tier : AugmentTier.values()) {
            int x = 16 + tier.ordinal() * 52;
            boolean selected = menu.tierOrdinal() == tier.ordinal();
            boolean hovered = inside(mouseX, mouseY, x, tierY, 48, 19);
            int fill = selected ? 0xFF313A45 : (hovered ? 0xFF29313A : PANEL_MUTED);
            graphics.fill(x, tierY, x + 48, tierY + 19, fill);
            graphics.outline(x, tierY, 48, 19, selected ? tierColor(tier) : 0xFF454E59);
            drawCentered(graphics, tierLabel(tier), x + 24, tierY + 6, selected ? TEXT : MUTED_TEXT);
        }

        int cardY = menu.advanced() ? 86 : 68;
        if (menu.status() == AugmentDeviceMenu.STATUS_READY
                || menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED) {
            for (int slot = 0; slot < 3; slot++) {
                drawCandidateCard(graphics, slot, cardY, mouseX, mouseY);
            }
        } else {
            String prompt = statusText(menu.status());
            int color = statusColor(menu.status());
            drawCentered(graphics, prompt, imageWidth / 2, cardY + 44, color);
            if (menu.status() == AugmentDeviceMenu.STATUS_IDLE) {
                drawCentered(
                        graphics,
                        "등급을 선택하면 보유 코어로 후보를 확인합니다.",
                        imageWidth / 2,
                        cardY + 61,
                        MUTED_TEXT
                );
            }
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            String instruction = menu.selectionCount() == 2
                    ? "서로 다른 후보 2개를 선택합니다."
                    : "후보 하나를 선택합니다.";
            drawCentered(graphics, instruction, imageWidth / 2, imageHeight - 20, MUTED_TEXT);
        } else if (menu.status() != AugmentDeviceMenu.STATUS_IDLE) {
            drawCentered(
                    graphics,
                    statusText(menu.status()),
                    imageWidth / 2,
                    imageHeight - 20,
                    statusColor(menu.status())
            );
        }
    }

    private void drawCandidateCard(
            GuiGraphicsExtractor graphics,
            int slot,
            int y,
            int mouseX,
            int mouseY
    ) {
        AugmentDefinition definition = menu.candidate(slot);
        if (definition == null) {
            return;
        }

        int x = 14 + slot * 112;
        int width = 104;
        int height = 92;
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        int fill = hovered ? 0xFF29313B : PANEL_INNER;
        int border = selected ? 0xFFFFFFFF : tierColor(definition.tier());

        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
        if (selected) {
            graphics.outline(x + 2, y + 2, width - 4, height - 4, tierColor(definition.tier()));
        }

        String rawName = definition.canonicalNameKo();
        boolean active = rawName.contains("(액티브");
        String name = rawName
                .replace(" (액티브 · 토글)", "")
                .replace(" (액티브)", "");

        drawCentered(graphics, name, x + width / 2, y + 13, TEXT);
        drawCentered(graphics, tierLabel(definition.tier()), x + width / 2, y + 32, tierColor(definition.tier()));
        if (active) {
            drawCentered(graphics, "액티브", x + width / 2, y + 47, 0xFFD8C47B);
        }

        int current = menu.currentLevel(slot);
        int next = Math.min(current + 1, definition.maxLevel());
        String levelText = current == 0
                ? "획득 → Lv." + next
                : "Lv." + current + " → " + next;
        drawCentered(graphics, levelText, x + width / 2, y + 66, MUTED_TEXT);
        drawCentered(
                graphics,
                "최대 Lv." + definition.maxLevel(),
                x + width / 2,
                y + 78,
                0xFF7E8792
        );
    }

    private void drawUpgradeView(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawCentered(graphics, "같은 등급 코어 3개를 바로 위 등급 코어 1개로 합성합니다.", imageWidth / 2, 62, MUTED_TEXT);
        for (int index = 0; index < AugmentTier.values().length - 1; index++) {
            AugmentTier source = AugmentTier.values()[index];
            AugmentTier target = AugmentTier.values()[index + 1];
            int y = 82 + index * 24;
            graphics.fill(22, y, imageWidth - 22, y + 20, PANEL_INNER);
            graphics.outline(22, y, imageWidth - 44, 20, 0xFF3B444F);
            graphics.text(font, tierLabel(source) + " 3", 34, y + 6, tierColor(source), false);
            graphics.text(font, "→", 105, y + 6, MUTED_TEXT, false);
            graphics.text(font, tierLabel(target) + " 1", 125, y + 6, tierColor(target), false);

            drawSmallAction(graphics, 238, y + 2, 40, 16, "1회", mouseX, mouseY);
            drawSmallAction(graphics, 284, y + 2, 46, 16, "전부", mouseX, mouseY);
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED) {
            drawCentered(
                    graphics,
                    "코어 합성 완료 ×" + Math.max(1, menu.lastOutputCount()),
                    imageWidth / 2,
                    imageHeight - 19,
                    SUCCESS_TEXT
            );
        } else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) {
            drawCentered(graphics, "합성할 코어가 부족합니다.", imageWidth / 2, imageHeight - 19, ERROR_TEXT);
        }
    }

    private void drawTab(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String label,
            boolean active,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        int fill = active ? 0xFF323B46 : (hovered ? 0xFF29313A : PANEL_MUTED);
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, active ? 0xFFD7DEE6 : 0xFF49525D);
        drawCentered(graphics, label, x + width / 2, y + 6, active ? TEXT : MUTED_TEXT);
    }

    private void drawSmallAction(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String label,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        graphics.fill(x, y, x + width, y + height, hovered ? 0xFF3A4652 : PANEL_MUTED);
        graphics.outline(x, y, width, height, hovered ? 0xFFB9C4D0 : 0xFF56616D);
        drawCentered(graphics, label, x + width / 2, y + 5, hovered ? TEXT : MUTED_TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        double localX = event.x() - leftPos;
        double localY = event.y() - topPos;

        if (menu.advanced()) {
            if (inside(localX, localY, 16, 34, 96, 19)) {
                upgradeMode = false;
                return true;
            }
            if (inside(localX, localY, 118, 34, 96, 19)) {
                upgradeMode = true;
                return true;
            }
        }

        if (upgradeMode && menu.advanced()) {
            for (int index = 0; index < AugmentTier.values().length - 1; index++) {
                int y = 82 + index * 24;
                AugmentTier source = AugmentTier.values()[index];
                if (inside(localX, localY, 238, y + 2, 40, 16)) {
                    return sendMenuButton(AugmentDeviceMenu.upgradeOneButtonId(source));
                }
                if (inside(localX, localY, 284, y + 2, 46, 16)) {
                    return sendMenuButton(AugmentDeviceMenu.upgradeAllButtonId(source));
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        int tierY = menu.advanced() ? 58 : 38;
        for (AugmentTier tier : AugmentTier.values()) {
            int x = 16 + tier.ordinal() * 52;
            if (inside(localX, localY, x, tierY, 48, 19)) {
                return sendMenuButton(AugmentDeviceMenu.beginButtonId(tier));
            }
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            int cardY = menu.advanced() ? 86 : 68;
            for (int slot = 0; slot < 3; slot++) {
                int x = 14 + slot * 112;
                if (menu.candidate(slot) != null && inside(localX, localY, x, cardY, 104, 92)) {
                    return sendMenuButton(AugmentDeviceMenu.candidateButtonId(slot));
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean sendMenuButton(int buttonId) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return false;
        }
        if (!menu.clickMenuButton(minecraft.player, buttonId)) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        return true;
    }

    private void drawCentered(
            GuiGraphicsExtractor graphics,
            String text,
            int centerX,
            int y,
            int color
    ) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
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

    private static int tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFC6CCD3;
            case UNCOMMON -> 0xFF72C986;
            case RARE -> 0xFF5BA5E3;
            case EPIC -> 0xFFB978E5;
            case LEGENDARY -> 0xFFE4B257;
            case MYTHIC -> 0xFFE16161;
        };
    }

    private static String statusText(int status) {
        return switch (status) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 모든 증강을 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "증강을 획득했습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "선택을 완료할 수 없습니다. 다시 시도하세요.";
            case AugmentDeviceMenu.STATUS_UPGRADED -> "코어 합성이 완료되었습니다.";
            case AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> "합성할 코어가 부족합니다.";
            default -> "증강 코어를 선택하세요.";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case AugmentDeviceMenu.STATUS_ACQUIRED, AugmentDeviceMenu.STATUS_UPGRADED -> SUCCESS_TEXT;
            case AugmentDeviceMenu.STATUS_NO_CORE,
                    AugmentDeviceMenu.STATUS_INVALID,
                    AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> ERROR_TEXT;
            default -> MUTED_TEXT;
        };
    }
}
