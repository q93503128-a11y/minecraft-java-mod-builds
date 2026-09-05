package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference-driven battle HUD geometry.
 *
 * The battlefield stays visually dominant: party state is a thin bottom strip, enemy state lives in world-space,
 * and the current actor's actions form one compact vertical stack at the lower-right.  This follows the same
 * information hierarchy as the reference screenshots without copying their art assets.
 */
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

        int margin = compact ? 4 : 7;
        int xs = compact ? 3 : 4;
        int s = compact ? 4 : 6;

        // Small reference-style utility controls occupy only the extreme lower-right corner.
        int controlH = compact ? 15 : 17;
        int autoW = compact ? 32 : 38;
        int speedW = compact ? 30 : 36;
        int fleeW = compact ? 38 : 46;
        int controlsTotal = autoW + speedW + fleeW + xs * 2;
        int controlsX = Math.max(margin, width - margin - controlsTotal);
        int controlsY = Math.max(margin, height - margin - controlH);
        Rect flee = inside(width, height, controlsX, controlsY, fleeW, controlH);
        Rect auto = inside(width, height, flee.right() + xs, controlsY, autoW, controlH);
        Rect speed = inside(width, height, auto.right() + xs, controlsY, speedW, controlH);

        // Party state is one thin horizontal status row, not four large cards.
        int allyH = compact ? 15 : 17;
        int allyGap = compact ? 3 : 5;
        int allyAreaRight = Math.max(margin + 4, controlsX - s);
        int allyAvailable = Math.max(4, allyAreaRight - margin);
        int allyW = Math.max(1, Math.min(compact ? 92 : 118,
                (allyAvailable - allyGap * (ALLY_COUNT - 1)) / ALLY_COUNT));
        int allyY = height - margin - allyH;
        List<Rect> allies = new ArrayList<>(ALLY_COUNT);
        for (int i = 0; i < ALLY_COUNT; i++) {
            allies.add(inside(width, height, margin + i * (allyW + allyGap), allyY, allyW, allyH));
        }

        // Enemy state is projected above actors. Compatibility rectangles remain intentionally negligible.
        List<Rect> enemies = new ArrayList<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemies.add(inside(width, height, width - margin - 1, margin + i, 1, 1));
        }

        // TURNBOUND still benefits from a turn queue, but the reference has no giant top HUD wall.
        int timelineW = Math.min(compact ? 168 : 228, Math.max(1, width - margin * 2));
        int timelineH = compact ? 13 : 15;
        Rect timeline = inside(width, height, (width - timelineW) / 2, margin, timelineW, timelineH);

        // Current actor actions: one readable vertical list, matching the reference's lower-right interaction flow.
        int dockW = compact ? Math.min(112, Math.max(94, width / 4)) : 132;
        int dockX = width - margin - dockW;
        int skillH = compact ? 20 : 22;
        int skillGap = compact ? 2 : 3;
        int headerH = compact ? 16 : 18;
        int skillAreaH = SKILL_COUNT * skillH + (SKILL_COUNT - 1) * skillGap;
        int dockBottom = controlsY - s;
        int dockY = Math.max(timeline.bottom() + s, dockBottom - headerH - skillAreaH);
        Rect header = inside(width, height, dockX, dockY, dockW, headerH);
        List<Rect> skills = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            skills.add(inside(width, height, dockX, header.bottom() + i * (skillH + skillGap), dockW, skillH));
        }

        int tooltipW = compact ? 154 : 214;
        int tooltipH = compact ? 68 : 86;
        int tooltipX = Math.max(margin, dockX - tooltipW - s);
        int tooltipY = Math.max(timeline.bottom() + s, Math.min(dockY + 10, controlsY - tooltipH - s));
        Rect tooltip = inside(width, height, tooltipX, tooltipY,
                Math.min(tooltipW, Math.max(1, dockX - margin - s)), tooltipH);

        // Kept for codec/test compatibility; actions commit directly by skill/target confirmation.
        Rect confirm = inside(width, height, dockX, Math.max(margin, controlsY - 1), 1, 1);
        int settingsW = Math.min(258, Math.max(1, width - margin * 2));
        int settingsH = Math.min(112, Math.max(1, height - margin * 2));
        Rect settings = inside(width, height, (width - settingsW) / 2, (height - settingsH) / 2, settingsW, settingsH);
        return new Layout(width, height, allies, enemies, skills, header, confirm, tooltip, timeline,
                auto, speed, flee, settings, compact);
    }

    private static Rect inside(int width, int height, int x, int y, int w, int h) {
        int sx = clamp(x, 0, width - 1);
        int sy = clamp(y, 0, height - 1);
        return new Rect(sx, sy, Math.max(1, Math.min(w, width - sx)), Math.max(1, Math.min(h, height - sy)));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
