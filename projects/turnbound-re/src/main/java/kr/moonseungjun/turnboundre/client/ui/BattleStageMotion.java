package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;

/** Pure presentation math for short virtual-stage action movement. */
public final class BattleStageMotion {
    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);
    }

    private BattleStageMotion() {}

    public static Offset actorOffset(BattleActionTimelineState.Cue cue, boolean enemy) {
        if (cue == null) return Offset.ZERO;
        int towardCenter = enemy ? 1 : -1;
        int distance = switch (cue.motionStyle()) {
            case CLOSE -> closeDistance(cue.phase(), cue.phaseProgress());
            case RANGED -> rangedDistance(cue.phase(), cue.phaseProgress());
            case CAST -> castDistance(cue.phase(), cue.phaseProgress());
            case UTILITY -> 0;
        };
        return new Offset(0, towardCenter * distance);
    }

    static int closeDistance(BattleActionTimelineState.Phase phase, double progress) {
        double p = clamp(progress);
        return switch (phase) {
            case WINDUP -> -(int) Math.round(2.0D * p);
            case IMPACT -> (int) Math.round(10.0D - 4.0D * p);
            case RECOVERY -> (int) Math.round(6.0D * (1.0D - p));
        };
    }

    static int rangedDistance(BattleActionTimelineState.Phase phase, double progress) {
        double p = clamp(progress);
        return switch (phase) {
            case WINDUP -> -(int) Math.round(2.0D * p);
            case IMPACT -> -(int) Math.round(3.0D * (1.0D - p));
            case RECOVERY -> -(int) Math.round(2.0D * (1.0D - p));
        };
    }

    static int castDistance(BattleActionTimelineState.Phase phase, double progress) {
        double p = clamp(progress);
        return switch (phase) {
            case WINDUP -> (int) Math.round(2.0D * p);
            case IMPACT -> (int) Math.round(3.0D - p);
            case RECOVERY -> (int) Math.round(2.0D * (1.0D - p));
        };
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
