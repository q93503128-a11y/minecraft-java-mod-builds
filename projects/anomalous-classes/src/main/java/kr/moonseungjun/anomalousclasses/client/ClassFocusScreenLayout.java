package kr.moonseungjun.anomalousclasses.client;

import java.util.ArrayList;
import java.util.List;

record ClassFocusScreenLayout(MenuUi.Box outer, MenuUi.Box recipePanel, MenuUi.Box infoPanel,
                              List<MenuUi.Box> tierButtons, List<MenuUi.Box> recipeCells,
                              MenuUi.Box arrowMarker, MenuUi.Box resultSlot, MenuUi.Box backButton,
                              MenuUi.Box equipButton, MenuUi.Box unequipButton,
                              MenuUi.Box alternativeButton) {
    static ClassFocusScreenLayout calculate(int screenWidth, int screenHeight) {
        int width = Math.max(1, screenWidth);
        int height = Math.max(1, screenHeight);
        int margin = Math.max(5, width / 20);
        int top = height < 220 ? 4 : 8;
        MenuUi.Box outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2),
                Math.max(1, height - top * 2));

        int innerX = outer.x() + Math.min(14, Math.max(7, outer.width() / 24));
        int innerRight = outer.right() - Math.min(14, Math.max(7, outer.width() / 24));
        int innerWidth = Math.max(1, innerRight - innerX);
        int tierGap = Math.min(5, Math.max(2, innerWidth / 100));
        int tierWidth = Math.min(56, Math.max(30, (innerWidth - tierGap * 4) / 5));
        int tierTotal = tierWidth * 5 + tierGap * 4;
        int tierX = outer.x() + (outer.width() - tierTotal) / 2;
        int tierY = outer.y() + 29;
        int tierHeight = Math.max(16, Math.min(22, height / 12));
        List<MenuUi.Box> tierButtons = new ArrayList<>(5);
        for (int index = 0; index < 5; index++) {
            tierButtons.add(new MenuUi.Box(tierX + index * (tierWidth + tierGap), tierY, tierWidth, tierHeight));
        }

        int footerHeight = Math.max(17, Math.min(22, height / 12));
        int footerY = outer.bottom() - footerHeight - 6;
        int bodyY = tierY + tierHeight + 5;
        int bodyBottom = Math.max(bodyY + 1, footerY - 5);
        int availableBodyHeight = Math.max(1, bodyBottom - bodyY);
        int preferredBodyHeight = Math.max(150, Math.min(280, Math.round(height * 0.44f)));
        int bodyHeight = Math.min(availableBodyHeight, preferredBodyHeight);
        int panelY = bodyY + (availableBodyHeight - bodyHeight) / 2;
        int bodyGap = Math.min(10, Math.max(5, innerWidth / 40));
        int recipeWidth = Math.max(1, Math.round((innerWidth - bodyGap) * 0.47f));
        int infoWidth = Math.max(1, innerWidth - recipeWidth - bodyGap);
        MenuUi.Box recipePanel = new MenuUi.Box(innerX, panelY, recipeWidth, bodyHeight);
        MenuUi.Box infoPanel = new MenuUi.Box(recipePanel.right() + bodyGap, panelY, infoWidth, bodyHeight);

        int cellGap = Math.max(2, Math.min(4, width / 150));
        int maxCellWidth = Math.max(12, (recipeWidth - 66) / 3);
        int maxCellHeight = Math.max(12, (bodyHeight - 14) / 3);
        int cell = Math.max(12, Math.min(32, Math.min(maxCellWidth, maxCellHeight)));
        int gridWidth = cell * 3 + cellGap * 2;
        int gridHeight = gridWidth;
        int gridX = recipePanel.x() + 6;
        int gridY = recipePanel.y() + Math.max(4, (bodyHeight - gridHeight) / 2);
        List<MenuUi.Box> recipeCells = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                recipeCells.add(new MenuUi.Box(gridX + column * (cell + cellGap),
                        gridY + row * (cell + cellGap), cell, cell));
            }
        }

        int resultSize = Math.max(18, Math.min(26, cell));
        int resultX = recipePanel.right() - resultSize - 8;
        int resultY = recipePanel.y() + (bodyHeight - resultSize) / 2;
        MenuUi.Box resultSlot = new MenuUi.Box(resultX, resultY, resultSize, resultSize);
        MenuUi.Box arrowMarker = new MenuUi.Box(resultX - 18, resultY + Math.max(0, (resultSize - 14) / 2), 12, 14);
        int backWidth = Math.min(82, Math.max(58, innerWidth / 4));
        MenuUi.Box backButton = new MenuUi.Box(innerX, footerY, backWidth, footerHeight);
        int actionGap = Math.min(6, Math.max(3, infoWidth / 30));
        int actionWidth = Math.max(1, (infoWidth - 14 - actionGap) / 2);
        int actionX = infoPanel.x() + 7;
        MenuUi.Box equipButton = new MenuUi.Box(actionX, footerY, actionWidth, footerHeight);
        MenuUi.Box unequipButton = new MenuUi.Box(equipButton.right() + actionGap, footerY,
                actionWidth, footerHeight);
        MenuUi.Box alternativeButton = new MenuUi.Box(infoPanel.x() + 7, infoPanel.y() + 38,
                Math.max(1, infoWidth - 14), Math.max(16, Math.min(20, footerHeight)));
        return new ClassFocusScreenLayout(outer, recipePanel, infoPanel, List.copyOf(tierButtons),
                List.copyOf(recipeCells), arrowMarker, resultSlot, backButton, equipButton, unequipButton,
                alternativeButton);
    }
}
