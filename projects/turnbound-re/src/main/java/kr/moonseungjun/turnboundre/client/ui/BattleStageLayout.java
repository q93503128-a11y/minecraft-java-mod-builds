package kr.moonseungjun.turnboundre.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pure layout math for the virtual battle stage inside the reserved world viewport. */
public final class BattleStageLayout {
    public static final int MAX_PLAYER_SLOTS = 4;
    public static final int MAX_ENEMY_SLOTS = 5;
    private static final int MAX_SLOT_WIDTH = 104;

    public enum Side { ENEMY, PLAYER }

    public record Slot(Side side, int participantIndex, UiLayoutMetrics.Rect bounds) {
        public Slot {
            if (side == null) throw new IllegalArgumentException("side required");
            if (participantIndex < 0) throw new IllegalArgumentException("participantIndex must be >= 0");
            if (bounds == null) throw new IllegalArgumentException("bounds required");
        }
    }

    public record Layout(
            UiLayoutMetrics.Rect enemyBand,
            UiLayoutMetrics.Rect playerBand,
            List<Slot> enemies,
            List<Slot> players
    ) {
        public Layout {
            if (enemyBand == null || playerBand == null) throw new IllegalArgumentException("stage bands required");
            enemies = enemies == null ? List.of() : List.copyOf(enemies);
            players = players == null ? List.of() : List.copyOf(players);
        }
    }

    private BattleStageLayout() {}

    public static Layout arrange(UiLayoutMetrics.Rect viewport, int playerCount, int enemyCount) {
        if (viewport == null) throw new IllegalArgumentException("viewport required");
        if (playerCount < 0 || playerCount > MAX_PLAYER_SLOTS) {
            throw new IllegalArgumentException("playerCount must be 0.." + MAX_PLAYER_SLOTS);
        }
        if (enemyCount < 0 || enemyCount > MAX_ENEMY_SLOTS) {
            throw new IllegalArgumentException("enemyCount must be 0.." + MAX_ENEMY_SLOTS);
        }

        int bandGap = Math.min(UiLayoutMetrics.SPACE_4, Math.max(0, viewport.height() - 2));
        int enemyHeight = Math.max(1, (viewport.height() - bandGap) / 2);
        int playerY = viewport.y() + enemyHeight + bandGap;
        int playerHeight = Math.max(1, viewport.bottom() - playerY);

        UiLayoutMetrics.Rect enemyBand = new UiLayoutMetrics.Rect(
                viewport.x(), viewport.y(), viewport.width(), enemyHeight);
        UiLayoutMetrics.Rect playerBand = new UiLayoutMetrics.Rect(
                viewport.x(), playerY, viewport.width(), playerHeight);

        return new Layout(
                enemyBand,
                playerBand,
                slots(enemyBand, Side.ENEMY, enemyCount),
                slots(playerBand, Side.PLAYER, playerCount));
    }

    /**
     * Resolves pointer input against the exact same participant slots used for stage rendering.
     * Target legality is intentionally not decided here; callers must still intersect the hit with
     * the server-authored eligible target set before treating it as interactive.
     */
    public static Optional<Slot> slotAt(Layout layout, double x, double y) {
        if (layout == null || !Double.isFinite(x) || !Double.isFinite(y)) return Optional.empty();
        for (Slot slot : layout.enemies()) {
            if (contains(slot.bounds(), x, y)) return Optional.of(slot);
        }
        for (Slot slot : layout.players()) {
            if (contains(slot.bounds(), x, y)) return Optional.of(slot);
        }
        return Optional.empty();
    }

    private static boolean contains(UiLayoutMetrics.Rect bounds, double x, double y) {
        return x >= bounds.x() && x < bounds.right() && y >= bounds.y() && y < bounds.bottom();
    }

    private static List<Slot> slots(UiLayoutMetrics.Rect band, Side side, int count) {
        if (count == 0) return List.of();
        int gap = UiLayoutMetrics.SPACE_2;
        int usableWidth = band.width() - gap * Math.max(0, count - 1);
        int slotWidth = Math.max(1, Math.min(MAX_SLOT_WIDTH, usableWidth / count));
        int totalWidth = slotWidth * count + gap * Math.max(0, count - 1);
        int startX = band.x() + Math.max(0, (band.width() - totalWidth) / 2);

        List<Slot> out = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int x = startX + index * (slotWidth + gap);
            out.add(new Slot(side, index,
                    new UiLayoutMetrics.Rect(x, band.y(), slotWidth, band.height())));
        }
        return List.copyOf(out);
    }
}
