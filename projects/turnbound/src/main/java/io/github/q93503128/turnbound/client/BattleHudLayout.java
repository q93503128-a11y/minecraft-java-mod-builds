package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/** alpha.7 edge HUD geometry: the battlefield center is intentionally left for the 3D scene. */
final class BattleHudLayout {
    static final int SKILL_COUNT = 5;
    static final int ALLY_COUNT = 4;
    static final int ENEMY_COUNT = 5;

    private BattleHudLayout() {}

    record Rect(int x, int y, int width, int height) {
        Rect {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("HUD rectangles must be positive");
        }
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
        boolean overlaps(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    record Layout(
            int screenWidth,
            int screenHeight,
            List<Rect> allyBars,
            List<Rect> enemyBars,
            List<Rect> skillButtons,
            Rect actionHeader,
            Rect confirmButton,
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
        boolean compact = width < 600 || height < 320;
        int margin = clamp(Math.min(width, height) / 70, 4, 7);
        int gap = compact ? 3 : 4;

        int controlHeight = compact ? 16 : 18;
        int controlWidth = compact ? 32 : 38;
        int controlGap = 3;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlsX = Math.max(margin, width - margin - controlsTotal);
        int controlsY = Math.max(margin, height - margin - controlHeight);
        Rect autoButton = inside(width, height, controlsX, controlsY, controlWidth, controlHeight);
        Rect speedButton = inside(width, height, autoButton.right() + controlGap, controlsY, controlWidth, controlHeight);
        Rect fleeButton = inside(width, height, speedButton.right() + controlGap, controlsY, controlWidth, controlHeight);

        int allyColumns = compact ? 2 : 4;
        int allyRows = (ALLY_COUNT + allyColumns - 1) / allyColumns;
        int allyHeight = compact ? 16 : 18;
        int allyRightLimit = Math.max(margin + allyColumns, controlsX - (compact ? 5 : 18));
        int allyAvailable = Math.max(allyColumns, allyRightLimit - margin - gap * (allyColumns - 1));
        int allyWidth = Math.max(1, Math.min(compact ? 108 : 136, allyAvailable / allyColumns));
        int allyTotalH = allyRows * allyHeight + Math.max(0, allyRows - 1) * gap;
        int allyStartY = Math.max(margin, height - margin - allyTotalH);
        List<Rect> allyBars = new ArrayList<>(ALLY_COUNT);
        for (int i = 0; i < ALLY_COUNT; i++) {
            int row = i / allyColumns;
            int col = i % allyColumns;
            allyBars.add(inside(width, height,
                    margin + col * (allyWidth + gap),
                    allyStartY + row * (allyHeight + gap), allyWidth, allyHeight));
        }

        int enemyColumns = compact ? 2 : 2;
        int enemyRows = (ENEMY_COUNT + enemyColumns - 1) / enemyColumns;
        int enemyHeight = compact ? 13 : 14;
        int enemyWidth = compact ? Math.max(56, Math.min(90, (width / 2 - margin * 2 - gap) / 2)) : 98;
        int enemyGridW = enemyColumns * enemyWidth + gap;
        int enemyStartX = Math.max(margin, width - margin - enemyGridW);
        int enemyStartY = margin;
        List<Rect> enemyBars = new ArrayList<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            int row = i / enemyColumns;
            int col = i % enemyColumns;
            enemyBars.add(inside(width, height,
                    enemyStartX + col * (enemyWidth + gap),
                    enemyStartY + row * (enemyHeight + gap), enemyWidth, enemyHeight));
        }

        int timelineWidth = Math.min(compact ? 156 : 196, Math.max(1, width - margin * 2));
        int timelineHeight = compact ? 16 : 18;
        Rect timeline = inside(width, height, Math.max(0, (width - timelineWidth) / 2), margin,
                timelineWidth, timelineHeight);

        int actionWidth = compact ? Math.min(118, Math.max(86, width / 3)) : 118;
        int actionX = Math.max(margin, width - margin - actionWidth);
        int skillColumns = compact ? 2 : 1;
        int skillRows = (SKILL_COUNT + skillColumns - 1) / skillColumns;
        int skillGap = 3;
        int skillHeight = compact ? 17 : 19;
        int headerHeight = compact ? 23 : 28;
        int confirmHeight = compact ? 17 : 19;
        int skillAreaH = skillRows * skillHeight + (skillRows - 1) * skillGap;
        int totalActionH = headerHeight + skillAreaH + 4 + confirmHeight;
        int desiredY = Math.max(enemyStartY + enemyRows * (enemyHeight + gap) + 12, height / 2 - totalActionH / 2);
        int maxY = Math.max(margin, controlsY - 10 - totalActionH);
        int actionY = Math.min(desiredY, maxY);
        actionY = Math.max(margin + timelineHeight + 6, actionY);
        Rect actionHeader = inside(width, height, actionX, actionY, actionWidth, headerHeight);

        int skillWidth = Math.max(1, (actionWidth - (skillColumns - 1) * skillGap) / skillColumns);
        int skillStartY = actionHeader.bottom();
        List<Rect> skillButtons = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            int row = i / skillColumns;
            int col = i % skillColumns;
            skillButtons.add(inside(width, height,
                    actionX + col * (skillWidth + skillGap),
                    skillStartY + row * (skillHeight + skillGap), skillWidth, skillHeight));
        }
        int confirmY = skillStartY + skillAreaH + 4;
        Rect confirmButton = inside(width, height, actionX, confirmY, actionWidth, confirmHeight);

        int settingsWidth = Math.min(246, Math.max(1, width - margin * 2));
        int settingsHeight = Math.min(112, Math.max(1, height - margin * 2));
        Rect settingsPanel = inside(width, height,
                Math.max(0, (width - settingsWidth) / 2),
                Math.max(0, (height - settingsHeight) / 2), settingsWidth, settingsHeight);

        return new Layout(width, height, allyBars, enemyBars, skillButtons, actionHeader, confirmButton,
                timeline, autoButton, speedButton, fleeButton, settingsPanel, compact);
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