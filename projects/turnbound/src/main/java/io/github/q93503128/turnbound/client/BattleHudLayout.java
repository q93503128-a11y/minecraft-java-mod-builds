package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Fresh alpha.6 HUD geometry. No legacy alpha.4/5 panel geometry is retained.
 * The center of the viewport is intentionally left unused so the 3D battle remains primary.
 */
final class BattleHudLayout {
    static final int SKILL_COUNT = 5;
    static final int ALLY_COUNT = 4;
    static final int ENEMY_COUNT = 5;

    private BattleHudLayout() {
    }

    record Rect(int x, int y, int width, int height) {
        Rect {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("HUD rectangles must be positive");
        }

        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }

    record Layout(
            int screenWidth,
            int screenHeight,
            List<Rect> allyBars,
            List<Rect> enemyBars,
            List<Rect> skillButtons,
            Rect actionHeader,
            Rect timeline,
            Rect autoButton,
            Rect speedButton,
            Rect fleeButton,
            Rect settingsPanel,
            boolean compact
    ) {
        Layout {
            allyBars = List.copyOf(allyBars);
            enemyBars = List.copyOf(enemyBars);
            skillButtons = List.copyOf(skillButtons);
        }
    }

    static Layout calculate(int requestedWidth, int requestedHeight) {
        int width = Math.max(1, requestedWidth);
        int height = Math.max(1, requestedHeight);
        boolean compact = width < 520 || height < 300;
        int margin = clamp(Math.min(width, height) / 55, 4, 8);
        int gap = compact ? 3 : 4;

        int controlHeight = compact ? 16 : 18;
        int controlWidth = compact ? 34 : 40;
        int controlGap = 3;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlsX = Math.max(0, width - margin - controlsTotal);
        int controlsY = Math.max(0, height - margin - controlHeight);
        Rect autoButton = inside(width, height, controlsX, controlsY, controlWidth, controlHeight);
        Rect speedButton = inside(width, height, autoButton.right() + controlGap, controlsY, controlWidth, controlHeight);
        Rect fleeButton = inside(width, height, speedButton.right() + controlGap, controlsY, controlWidth, controlHeight);

        int allyColumns = compact ? 2 : 4;
        int allyRows = (ALLY_COUNT + allyColumns - 1) / allyColumns;
        int allyHeight = compact ? 16 : 19;
        int allyRightLimit = compact ? controlsX - 6 : width - margin - controlsTotal - 12;
        allyRightLimit = Math.max(margin + allyColumns, allyRightLimit);
        int allyAvailable = Math.max(allyColumns, allyRightLimit - margin - gap * (allyColumns - 1));
        int allyWidth = Math.max(1, Math.min(compact ? 112 : 142, allyAvailable / allyColumns));
        int allyTotalH = allyRows * allyHeight + Math.max(0, allyRows - 1) * gap;
        int allyStartY = Math.max(0, height - margin - allyTotalH);
        List<Rect> allyBars = new ArrayList<>(ALLY_COUNT);
        for (int i = 0; i < ALLY_COUNT; i++) {
            int row = i / allyColumns;
            int col = i % allyColumns;
            allyBars.add(inside(width, height,
                    margin + col * (allyWidth + gap),
                    allyStartY + row * (allyHeight + gap),
                    allyWidth,
                    allyHeight));
        }

        int enemyColumns = compact ? 2 : 3;
        int enemyRows = (ENEMY_COUNT + enemyColumns - 1) / enemyColumns;
        int enemyHeight = compact ? 14 : 16;
        int enemyWidth = compact ? Math.min(96, Math.max(1, (width - margin * 2 - gap) / 2)) : 108;
        int enemyGridW = enemyColumns * enemyWidth + (enemyColumns - 1) * gap;
        int enemyStartX = Math.max(margin, width - margin - enemyGridW);
        int enemyStartY = margin;
        List<Rect> enemyBars = new ArrayList<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            int row = i / enemyColumns;
            int col = i % enemyColumns;
            enemyBars.add(inside(width, height,
                    enemyStartX + col * (enemyWidth + gap),
                    enemyStartY + row * (enemyHeight + gap),
                    enemyWidth,
                    enemyHeight));
        }

        int timelineWidth = Math.min(compact ? 180 : 230, Math.max(1, width - margin * 2));
        int timelineHeight = compact ? 17 : 19;
        Rect timeline = inside(width, height,
                Math.max(0, (width - timelineWidth) / 2), margin,
                timelineWidth, timelineHeight);

        int actionWidth = compact ? Math.min(104, Math.max(76, width / 3)) : 126;
        int skillHeight = compact ? 17 : 20;
        int skillGap = 3;
        int actionX = Math.max(0, width - margin - actionWidth);
        int actionBodyH = SKILL_COUNT * skillHeight + (SKILL_COUNT - 1) * skillGap;
        int headerHeight = compact ? 20 : 24;
        int actionBottom = Math.max(enemyStartY + enemyRows * (enemyHeight + gap) + 6, controlsY - 7);
        int actionY = Math.max(margin + timelineHeight + 6, actionBottom - actionBodyH - headerHeight);
        Rect actionHeader = inside(width, height, actionX, actionY, actionWidth, headerHeight);
        int skillStartY = actionHeader.bottom();
        List<Rect> skillButtons = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            skillButtons.add(inside(width, height,
                    actionX,
                    skillStartY + i * (skillHeight + skillGap),
                    actionWidth,
                    skillHeight));
        }

        int settingsWidth = Math.min(250, Math.max(1, width - margin * 2));
        int settingsHeight = Math.min(118, Math.max(1, height - margin * 2));
        Rect settingsPanel = inside(width, height,
                Math.max(0, (width - settingsWidth) / 2),
                Math.max(0, (height - settingsHeight) / 2),
                settingsWidth,
                settingsHeight);

        return new Layout(width, height, allyBars, enemyBars, skillButtons, actionHeader, timeline,
                autoButton, speedButton, fleeButton, settingsPanel, compact);
    }

    private static Rect inside(int width, int height, int x, int y, int rectWidth, int rectHeight) {
        int safeX = clamp(x, 0, width - 1);
        int safeY = clamp(y, 0, height - 1);
        int safeW = Math.max(1, Math.min(rectWidth, width - safeX));
        int safeH = Math.max(1, Math.min(rectHeight, height - safeY));
        return new Rect(safeX, safeY, safeW, safeH);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
