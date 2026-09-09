package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;

/**
 * Pure presentation contract for representative virtual-stage character identity.
 * It never decides damage, legality, targets, or turn order.
 */
public final class BattleStageCharacterPresentation {
    static final String SKELETON = "turnbound_re:skeleton";
    static final String ENDERMAN = "turnbound_re:enderman";
    static final float DEFAULT_X_ANGLE = 0.0F;
    static final float DEFAULT_Y_ANGLE = 0.35F;

    public record Pose(
            int offsetX,
            int offsetY,
            float xAngle,
            float yAngle,
            boolean controlsAggressive,
            boolean aggressive
    ) {
        static final Pose DEFAULT = new Pose(
                0, 0, DEFAULT_X_ANGLE, DEFAULT_Y_ANGLE, false, false);
    }

    private BattleStageCharacterPresentation() {}

    static boolean usesBow(String characterId) {
        return SKELETON.equals(characterId);
    }

    static Pose pose(
            String characterId,
            String participantId,
            BattleActionTimelineState.Cue cue
    ) {
        if (SKELETON.equals(characterId)) {
            boolean aiming = isActor(participantId, cue)
                    && cue.impactStyle() == BattleActionTimelineState.ImpactStyle.PROJECTILE
                    && (cue.phase() == BattleActionTimelineState.Phase.WINDUP
                    || (cue.phase() == BattleActionTimelineState.Phase.IMPACT && cue.phaseProgress() < 0.36D));
            return new Pose(
                    0,
                    0,
                    aiming ? -0.04F : DEFAULT_X_ANGLE,
                    aiming ? 0.52F : DEFAULT_Y_ANGLE,
                    true,
                    aiming);
        }

        if (ENDERMAN.equals(characterId)
                && isActor(participantId, cue)
                && cue.impactStyle() == BattleActionTimelineState.ImpactStyle.VOID) {
            return enderPose(cue);
        }
        return Pose.DEFAULT;
    }

    private static Pose enderPose(BattleActionTimelineState.Cue cue) {
        double p = clamp(cue.phaseProgress());
        int offsetX;
        float xAngle;
        float yAngle;
        switch (cue.phase()) {
            case WINDUP -> {
                double wave = Math.sin(p * Math.PI * 3.0D);
                offsetX = (int) Math.round(wave * (1.0D + 2.0D * p));
                xAngle = (float) (-0.07D * Math.sin(p * Math.PI));
                yAngle = (float) (DEFAULT_Y_ANGLE + Math.sin(p * Math.PI * 2.0D) * 0.18D);
            }
            case IMPACT -> {
                int direction = (cue.beatIndex() & 1) == 0 ? 1 : -1;
                offsetX = direction * (int) Math.round(2.0D * (1.0D - p));
                xAngle = -0.05F * (float) (1.0D - p);
                yAngle = (float) (DEFAULT_Y_ANGLE + 0.13D * (1.0D - p));
            }
            case RECOVERY -> {
                offsetX = 0;
                xAngle = -0.02F * (float) (1.0D - p);
                yAngle = (float) (DEFAULT_Y_ANGLE + 0.06D * (1.0D - p));
            }
            default -> throw new IllegalStateException("Unexpected phase: " + cue.phase());
        }
        return new Pose(offsetX, 0, xAngle, yAngle, false, false);
    }

    private static boolean isActor(String participantId, BattleActionTimelineState.Cue cue) {
        return participantId != null
                && cue != null
                && participantId.equals(cue.actorId());
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
