package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

final class BattleScreenLayout {
    static final int SKILL_COUNT = 5;
    static final int ALLY_COUNT = 4;
    static final int ENEMY_COUNT = 5;

    private BattleScreenLayout() {
    }

    record Rect(int x, int y, int width, int height) {
        Rect {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Battle HUD rectangles must have positive size");
            }
        }

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }

    record Layout(
            int screenWidth,
            int screenHeight,
            List<Rect> skillButtons,
            List<Rect> allyHud,
            List<Rect> enemyHud,
            Rect autoButton,
            Rect speedButton,
            Rect fleeButton,
            Rect actionPanel,
            Rect timelinePanel,
            Rect settingsPanel,
            boolean compact
    ) {
        Layout {
            skillButtons = List.copyOf(skillButtons);
            allyHud = List.copyOf(allyHud);
            enemyHud = List.copyOf(enemyHud);
        }
    }

    static Layout calculate(int requestedWidth, int requestedHeight) {
        int width = Math.max(1, requestedWidth);
        int height = Math.max(1, requestedHeight);
        int margin = Math.max(4, Math.min(10, width / 70));
        int gap = Math.max(2, Math.min(5, width / 180));
        boolean compact = width < 560 || height < 300;

        int actionWidth = compact
                ? Math.max(112, Math.min(168, width / 3))
                : Math.max(150, Math.min(190, width / 5));
        actionWidth = Math.min(actionWidth, Math.max(1, width - margin * 2));

        int allyColumns = compact ? 2 : 4;
        int allyRows = (ALLY_COUNT + allyColumns - 1) / allyColumns;
        int allyAreaRight = Math.max(margin + 1, width - actionWidth - margin - 8);
        int allyAvailable = Math.max(ALLY_COUNT, allyAreaRight - margin - gap * (allyColumns - 1));
        int allyWidth = Math.max(1, Math.min(compact ? 154 : 184, allyAvailable / allyColumns));
        int allyHeight = compact ? Math.max(18, Math.min(25, height / 8)) : Math.max(24, Math.min(32, height / 12));
        int allyTotalHeight = allyRows * allyHeight + (allyRows - 1) * gap;
        int allyStartY = Math.max(0, height - margin - allyTotalHeight);

        List<Rect> allyHud = new ArrayList<>(ALLY_COUNT);
        for (int i = 0; i < ALLY_COUNT; i++) {
            int row = i / allyColumns;
            int col = i % allyColumns;
            allyHud.add(rectInside(width, height,
                    margin + col * (allyWidth + gap),
                    allyStartY + row * (allyHeight + gap),
                    allyWidth,
                    allyHeight));
        }

        int enemyColumns = width >= 700 ? 3 : 2;
        int enemyRows = (ENEMY_COUNT + enemyColumns - 1) / enemyColumns;
        int enemyGap = 4;
        int enemyHeight = compact ? 20 : 24;
        int enemyAreaWidth = Math.max(1, Math.min(width / 2 - margin, compact ? width - margin * 2 : 450));
        int enemyWidth = Math.max(1, Math.min(145,
                (enemyAreaWidth - enemyGap * (enemyColumns - 1)) / enemyColumns));
        int enemyGridWidth = enemyColumns * enemyWidth + enemyGap * (enemyColumns - 1);
        int enemyStartX = Math.max(margin, width - margin - enemyGridWidth);
        int enemyStartY = margin;

        List<Rect> enemyHud = new ArrayList<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            int row = i / enemyColumns;
            int col = i % enemyColumns;
            enemyHud.add(rectInside(width, height,
                    enemyStartX + col * (enemyWidth + enemyGap),
                    enemyStartY + row * (enemyHeight + enemyGap),
                    enemyWidth,
                    enemyHeight));
        }

        int skillColumns = compact ? 2 : 1;
        int skillRows = (SKILL_COUNT + skillColumns - 1) / skillColumns;
        int skillGap = compact ? 3 : 4;
        int skillHeight = compact ? 20 : 27;
        int panelPadding = compact ? 6 : 8;
        int controlsHeight = compact ? 18 : 20;
        int headerHeight = compact ? 20 : 26;
        int actionPanelHeight = panelPadding + headerHeight
                + skillRows * skillHeight + (skillRows - 1) * skillGap
                + 6 + controlsHeight + panelPadding;
        int maxPanelHeight = Math.max(1, allyStartY - margin - 4);
        if (actionPanelHeight > maxPanelHeight) {
            skillHeight = Math.max(16, skillHeight - (actionPanelHeight - maxPanelHeight + skillRows - 1) / skillRows);
            actionPanelHeight = panelPadding + headerHeight
                    + skillRows * skillHeight + (skillRows - 1) * skillGap
                    + 6 + controlsHeight + panelPadding;
        }
        actionPanelHeight = Math.max(1, Math.min(actionPanelHeight, Math.max(1, allyStartY - margin)));
        int actionX = Math.max(0, width - margin - actionWidth);
        int actionY = Math.max(margin, allyStartY - actionPanelHeight - 6);
        Rect actionPanel = rectInside(width, height, actionX, actionY, actionWidth, actionPanelHeight);

        int skillAreaWidth = Math.max(skillColumns,
                actionPanel.width() - panelPadding * 2 - skillGap * (skillColumns - 1));
        int skillWidth = Math.max(1, skillAreaWidth / skillColumns);
        int skillStartY = actionPanel.y() + panelPadding + headerHeight;
        List<Rect> skillButtons = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            int row = i / skillColumns;
            int col = i % skillColumns;
            skillButtons.add(rectInside(width, height,
                    actionPanel.x() + panelPadding + col * (skillWidth + skillGap),
                    skillStartY + row * (skillHeight + skillGap),
                    skillWidth,
                    skillHeight));
        }

        int controlY = Math.min(actionPanel.bottom() - panelPadding - controlsHeight,
                skillStartY + skillRows * skillHeight + Math.max(0, skillRows - 1) * skillGap + 6);
        int controlGap = 3;
        int controlWidth = Math.max(1,
                (actionPanel.width() - panelPadding * 2 - controlGap * 2) / 3);
        Rect autoButton = rectInside(width, height,
                actionPanel.x() + panelPadding, controlY, controlWidth, controlsHeight);
        Rect speedButton = rectInside(width, height,
                autoButton.right() + controlGap, controlY, controlWidth, controlsHeight);
        Rect fleeButton = rectInside(width, height,
                speedButton.right() + controlGap, controlY, controlWidth, controlsHeight);

        int timelineWidth = Math.max(1, Math.min(compact ? width / 2 : 340, width - margin * 2));
        int timelineHeight = compact ? 24 : 28;
        Rect timelinePanel = rectInside(width, height, margin, margin, timelineWidth, timelineHeight);

        int settingsWidth = Math.max(1, Math.min(270, width - margin * 2));
        int settingsHeight = Math.max(1, Math.min(150, height - margin * 2));
        Rect settingsPanel = rectInside(width, height,
                Math.max(0, (width - settingsWidth) / 2),
                Math.max(0, (height - settingsHeight) / 2),
                settingsWidth,
                settingsHeight);

        return new Layout(width, height, skillButtons, allyHud, enemyHud,
                autoButton, speedButton, fleeButton, actionPanel, timelinePanel, settingsPanel, compact);
    }

    private static Rect rectInside(int width, int height, int x, int y, int rectWidth, int rectHeight) {
        int safeX = Math.max(0, Math.min(x, width - 1));
        int safeY = Math.max(0, Math.min(y, height - 1));
        int safeWidth = Math.max(1, Math.min(rectWidth, width - safeX));
        int safeHeight = Math.max(1, Math.min(rectHeight, height - safeY));
        return new Rect(safeX, safeY, safeWidth, safeHeight);
    }
}
