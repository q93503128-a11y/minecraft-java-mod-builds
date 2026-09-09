package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;

import java.util.Optional;

/** Pure geometry/timing helpers for short action effects inside the virtual battle stage. */
public final class BattleStageActionFx {
    public record Point(int x, int y) {}

    private BattleStageActionFx() {}

    public static Optional<UiLayoutMetrics.Rect> modelBounds(
            UiLayoutMetrics.Rect slot,
            boolean enemy,
            int lineHeight
    ) {
        if (slot == null || lineHeight <= 0) return Optional.empty();
        int modelTop = slot.y() + (enemy ? 30 : 11);
        int modelBottom = slot.bottom() - lineHeight;
        int width = Math.max(0, slot.width() - UiLayoutMetrics.SPACE_4);
        int height = modelBottom - modelTop;
        if (width <= 2 || height <= 2) return Optional.empty();
        return Optional.of(new UiLayoutMetrics.Rect(
                slot.x() + UiLayoutMetrics.SPACE_2,
                modelTop,
                width,
                height));
    }

    public static Point center(UiLayoutMetrics.Rect bounds) {
        if (bounds == null) throw new IllegalArgumentException("bounds required");
        return new Point(bounds.x() + bounds.width() / 2, bounds.y() + bounds.height() / 2);
    }

    /** Smooth travel that reaches the authoritative target exactly when IMPACT begins. */
    public static Point travel(Point from, Point to, double progress) {
        if (from == null || to == null) throw new IllegalArgumentException("points required");
        double p = clamp(progress);
        double eased = 1.0D - Math.pow(1.0D - p, 2.0D);
        int x = (int) Math.round(from.x() + (to.x() - from.x()) * eased);
        int y = (int) Math.round(from.y() + (to.y() - from.y()) * eased);
        return new Point(x, y);
    }

    public static boolean hasTravel(BattleActionTimelineState.ImpactStyle style) {
        if (style == null) return false;
        return switch (style) {
            case PROJECTILE, FIRE, ARCANE, VOID -> true;
            case NONE, MELEE, BLAST -> false;
        };
    }

    public static BattleActionTimelineState.Phase soundTriggerPhase(BattleActionTimelineState.ImpactStyle style) {
        if (style == null) return BattleActionTimelineState.Phase.IMPACT;
        return switch (style) {
            case PROJECTILE, FIRE, ARCANE, VOID -> BattleActionTimelineState.Phase.WINDUP;
            case NONE, MELEE, BLAST -> BattleActionTimelineState.Phase.IMPACT;
        };
    }

    public static boolean impactFlashVisible(BattleActionTimelineState.Cue cue) {
        return cue != null
                && cue.impactStyle() != BattleActionTimelineState.ImpactStyle.NONE
                && cue.phase() == BattleActionTimelineState.Phase.IMPACT
                && cue.phaseProgress() < 0.78D;
    }

    public static int impactInset(double progress) {
        double p = clamp(progress);
        return Math.max(0, Math.min(3, (int) Math.floor(p * 4.0D)));
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
