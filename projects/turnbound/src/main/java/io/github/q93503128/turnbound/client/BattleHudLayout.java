package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/** Compact edge HUD patterned after dense Minecraft mod panels and the reference game's world-first battle hierarchy. */
final class BattleHudLayout {
    static final int SKILL_COUNT = 5;
    static final int ALLY_COUNT = 4;
    static final int ENEMY_COUNT = 5;

    private BattleHudLayout() {}

    record Rect(int x, int y, int width, int height) {
        Rect { if (width <= 0 || height <= 0) throw new IllegalArgumentException("HUD rectangles must be positive"); }
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
        boolean overlaps(Rect other) { return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y; }
    }

    record Layout(
            int screenWidth, int screenHeight,
            List<Rect> allyBars, List<Rect> enemyBars, List<Rect> skillButtons,
            Rect actionHeader, Rect confirmButton, Rect tooltipArea, Rect timeline,
            Rect autoButton, Rect speedButton, Rect fleeButton, Rect settingsPanel, boolean compact
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
        boolean compact = width < 560 || height < 300;
        int margin = compact ? 4 : 6;
        int gap = compact ? 3 : 4;

        int controlH = compact ? 16 : 19;
        int controlW = compact ? 34 : 42;
        int controlsTotal = controlW * 3 + gap * 2;
        int controlsX = Math.max(margin, width - margin - controlsTotal);
        int controlsY = Math.max(margin, height - margin - controlH);
        Rect auto = inside(width, height, controlsX, controlsY, controlW, controlH);
        Rect speed = inside(width, height, auto.right() + gap, controlsY, controlW, controlH);
        Rect flee = inside(width, height, speed.right() + gap, controlsY, controlW, controlH);

        int allyGap = compact ? 3 : 5;
        int allyH = compact ? 16 : 19;
        int allyAvailable = Math.max(4, controlsX - margin - 10);
        int allyW = Math.max(1, Math.min(compact ? 92 : 130, (allyAvailable - allyGap * 3) / 4));
        int allyY = height - margin - allyH;
        List<Rect> allies = new ArrayList<>(ALLY_COUNT);
        for (int i = 0; i < ALLY_COUNT; i++) allies.add(inside(width, height,
                margin + i * (allyW + allyGap), allyY, allyW, allyH));

        // Enemy state is rendered in world-space. Keep compatibility rectangles tiny and non-obstructive.
        List<Rect> enemies = new ArrayList<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) enemies.add(inside(width, height, width - margin - 1, margin + i, 1, 1));

        int timelineW = Math.min(compact ? 150 : 205, Math.max(1, width - 2 * margin));
        int timelineH = compact ? 15 : 18;
        Rect timeline = inside(width, height, (width - timelineW) / 2, margin, timelineW, timelineH);

        int dockW = compact ? Math.min(128, Math.max(98, width / 3)) : 148;
        int dockX = width - margin - dockW;
        int skillColumns = 2;
        int skillRows = 3;
        int skillH = compact ? 24 : 29;
        int skillW = (dockW - gap) / 2;
        int skillAreaH = skillRows * skillH + (skillRows - 1) * gap;
        int headerH = compact ? 20 : 24;
        int dockBottom = controlsY - 7;
        int dockY = Math.max(timeline.bottom() + 8, dockBottom - headerH - skillAreaH);
        Rect header = inside(width, height, dockX, dockY, dockW, headerH);
        List<Rect> skills = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            int row = i / 2;
            int col = i % 2;
            skills.add(inside(width, height, dockX + col * (skillW + gap), header.bottom() + row * (skillH + gap), skillW, skillH));
        }

        int tooltipW = compact ? 150 : 208;
        int tooltipH = compact ? 70 : 88;
        int tooltipX = Math.max(margin, dockX - tooltipW - 7);
        int tooltipY = Math.min(dockY, Math.max(timeline.bottom() + 6, controlsY - tooltipH - 8));
        Rect tooltip = inside(width, height, tooltipX, tooltipY, Math.min(tooltipW, Math.max(1, dockX - margin - 7)), tooltipH);

        // Kept only as a compatibility geometry slot; alpha.15 has no visible/use button.
        Rect confirm = inside(width, height, dockX, Math.max(margin, controlsY - 1), 1, 1);
        int settingsW = Math.min(250, Math.max(1, width - margin * 2));
        int settingsH = Math.min(110, Math.max(1, height - margin * 2));
        Rect settings = inside(width, height, (width - settingsW) / 2, (height - settingsH) / 2, settingsW, settingsH);
        return new Layout(width, height, allies, enemies, skills, header, confirm, tooltip, timeline, auto, speed, flee, settings, compact);
    }

    private static Rect inside(int width, int height, int x, int y, int w, int h) {
        int sx = clamp(x, 0, width - 1), sy = clamp(y, 0, height - 1);
        return new Rect(sx, sy, Math.max(1, Math.min(w, width - sx)), Math.max(1, Math.min(h, height - sy)));
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
