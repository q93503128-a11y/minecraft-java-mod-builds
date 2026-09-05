package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference-driven battle HUD geometry.
 *
 * The battlefield stays visually dominant: party state is a thin bottom strip, enemy state lives in world-space,
 * and the current actor's actions form one compact vertical stack at the lower-right. Utility controls reserve enough
 * width for Korean labels so AUTO/speed/flee state never relies on ellipsis for normal campaign text.
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
        boolean compact = width < 700 || height < 400;
        boolean tiny = height < 200;
        int margin = compact ? 4 : 7;
        int xs = tiny ? 2 : compact ? 3 : 4;
        int s = compact ? 5 : 7;

        // Utility controls stay visually subordinate, but their widths fit the longest normal Korean state labels.
        int controlH = tiny ? 15 : compact ? 16 : 17;
        int autoW = tiny ? 48 : compact ? 52 : 56;
        int speedW = tiny ? 50 : compact ? 54 : 58;
        int fleeW = tiny ? 50 : compact ? 54 : 58;
        int controlsTotal = autoW + speedW + fleeW + xs * 2;
        int controlsX = Math.max(margin, width - margin - controlsTotal);
        int controlsY = Math.max(margin, height - margin - controlH);
        Rect flee = inside(width, height, controlsX, controlsY, fleeW, controlH);
        Rect auto = inside(width, height, flee.right() + xs, controlsY, autoW, controlH);
        Rect speed = inside(width, height, auto.right() + xs, controlsY, speedW, controlH);

        // Party state remains one low-profile strip instead of four cards.
        int allyH = tiny ? 15 : compact ? 16 : 17;
        int allyGap = compact ? 3 : 4;
        int allyAreaRight = Math.max(margin + 4, controlsX - s);
        int allyAvailable = Math.max(4, allyAreaRight - margin);
        int allyW = Math.max(1, Math.min(compact ? 92 : 118,
                (allyAvailable - allyGap * (ALLY_COUNT - 1)) / ALLY_COUNT));
        int allyY = height - margin - allyH;
        List<Rect> allies = new ArrayList<>(ALLY_COUNT);
        for (int i = 0; i < ALLY_COUNT; i++) {
            allies.add(inside(width, height, margin + i * (allyW + allyGap), allyY, allyW, allyH));
        }

        // Enemy state is projected above actors. Compatibility rectangles intentionally stay negligible.
        List<Rect> enemies = new ArrayList<>(ENEMY_COUNT);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemies.add(inside(width, height, width - margin - 1, margin + i, 1, 1));
        }

        // Restrained top-center turn queue, slightly narrower than the previous playtest pass.
        int timelineW = Math.min(compact ? 164 : 214, Math.max(1, width - margin * 2));
        int timelineH = tiny ? 12 : compact ? 13 : 14;
        Rect timeline = inside(width, height, (width - timelineW) / 2, margin, timelineW, timelineH);

        // Current actor actions use one vertical scan path and leave more world visible than the older wide dock.
        int dockW = tiny ? Math.min(102, Math.max(90, width / 3))
                : compact ? Math.min(118, Math.max(100, width / 4))
                : Math.min(148, Math.max(136, width / 7));
        int dockX = width - margin - dockW;
        int skillH = tiny ? 17 : compact ? 19 : 22;
        int skillGap = tiny ? 1 : 2;
        int headerH = tiny ? 14 : compact ? 15 : 16;
        int skillAreaH = SKILL_COUNT * skillH + (SKILL_COUNT - 1) * skillGap;
        int dockBottom = controlsY - s;
        int dockY = Math.max(timeline.bottom() + s, dockBottom - headerH - skillAreaH);
        Rect header = inside(width, height, dockX, dockY, dockW, headerH);
        List<Rect> skills = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            skills.add(inside(width, height, dockX, header.bottom() + i * (skillH + skillGap), dockW, skillH));
        }

        int tooltipW = tiny ? 134 : compact ? 158 : 212;
        int tooltipH = tiny ? 46 : compact ? 66 : 84;
        int tooltipX = Math.max(margin, dockX - tooltipW - s);
        int tooltipY = Math.max(timeline.bottom() + s,
                Math.min(dockY + (tiny ? 3 : 10), controlsY - tooltipH - s));
        Rect tooltip = inside(width, height, tooltipX, tooltipY,
                Math.min(tooltipW, Math.max(1, dockX - margin - s)), tooltipH);

        // Kept for codec/test compatibility; actions commit directly by skill/target confirmation.
        Rect confirm = inside(width, height, dockX, Math.max(margin, controlsY - 1), 1, 1);
        int settingsW = Math.min(248, Math.max(1, width - margin * 2));
        int settingsH = Math.min(106, Math.max(1, height - margin * 2));
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
