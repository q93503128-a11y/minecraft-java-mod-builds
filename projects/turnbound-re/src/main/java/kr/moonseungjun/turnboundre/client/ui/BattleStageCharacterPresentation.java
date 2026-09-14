package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;

/**
 * Pure presentation contract for representative virtual-stage character identity.
 * It never decides damage, legality, targets, or turn order.
 */
public final class BattleStageCharacterPresentation {
    static final String ZOMBIE = "turnbound_re:zombie";
    static final String SKELETON = "turnbound_re:skeleton";
    static final String BLAZE = "turnbound_re:blaze";
    static final String ENDERMAN = "turnbound_re:enderman";
    static final float DEFAULT_X_ANGLE = 0.0F;
    static final float DEFAULT_Y_ANGLE = 0.35F;

    private static final String BLAZE_EMBER_BOLT = "turnbound_re:blaze_ember_bolt";
    private static final String BLAZE_SEARING_VOLLEY = "turnbound_re:blaze_searing_volley";
    private static final String BLAZE_HEAT_UP = "turnbound_re:blaze_heat_up";
    private static final String BLAZE_INFERNO_BURST = "turnbound_re:blaze_inferno_burst";

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
        if (ZOMBIE.equals(characterId)) {
            boolean attacking = isActor(participantId, cue)
                    && cue.impactStyle() == BattleActionTimelineState.ImpactStyle.MELEE
                    && targetsOtherParticipant(cue)
                    && (cue.phase() == BattleActionTimelineState.Phase.WINDUP
                    || cue.phase() == BattleActionTimelineState.Phase.IMPACT);
            return new Pose(
                    0,
                    attacking ? -1 : 0,
                    attacking ? -0.05F : DEFAULT_X_ANGLE,
                    attacking ? 0.43F : DEFAULT_Y_ANGLE,
                    true,
                    attacking);
        }

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

        if (BLAZE.equals(characterId)) {
            return blazePose(participantId, cue);
        }

        if (ENDERMAN.equals(characterId)
                && isActor(participantId, cue)
                && cue.impactStyle() == BattleActionTimelineState.ImpactStyle.VOID) {
            return enderPose(cue);
        }
        return Pose.DEFAULT;
    }

    private static Pose blazePose(String participantId, BattleActionTimelineState.Cue cue) {
        if (!isActor(participantId, cue)) return neutralControlledPose();

        boolean activeBeat = cue.phase() == BattleActionTimelineState.Phase.WINDUP
                || cue.phase() == BattleActionTimelineState.Phase.IMPACT;
        boolean offensiveFire = activeBeat
                && cue.impactStyle() == BattleActionTimelineState.ImpactStyle.FIRE
                && isBlazeOffensiveAction(cue.actionId())
                && targetsOtherParticipant(cue);
        if (offensiveFire) {
            return new Pose(0, -1, -0.06F, 0.47F, true, true);
        }

        boolean heatUp = activeBeat
                && BLAZE_HEAT_UP.equals(cue.actionId())
                && cue.impactStyle() == BattleActionTimelineState.ImpactStyle.FIRE
                && targetsOnlyActor(cue);
        if (heatUp) {
            // Heat Up reads as a short lift/charge, never as the forward firing silhouette.
            return new Pose(0, -2, -0.08F, DEFAULT_Y_ANGLE, true, false);
        }
        return neutralControlledPose();
    }

    private static Pose neutralControlledPose() {
        return new Pose(0, 0, DEFAULT_X_ANGLE, DEFAULT_Y_ANGLE, true, false);
    }

    private static boolean isBlazeOffensiveAction(String actionId) {
        return BLAZE_EMBER_BOLT.equals(actionId)
                || BLAZE_SEARING_VOLLEY.equals(actionId)
                || BLAZE_INFERNO_BURST.equals(actionId);
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
                float remaining = (float) (1.0D - p);
                xAngle = remaining <= 0.0F ? DEFAULT_X_ANGLE : -0.02F * remaining;
                yAngle = remaining <= 0.0F
                        ? DEFAULT_Y_ANGLE
                        : (float) (DEFAULT_Y_ANGLE + 0.06D * remaining);
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

    private static boolean targetsOtherParticipant(BattleActionTimelineState.Cue cue) {
        if (cue == null || cue.targetIds().isEmpty()) return false;
        for (String targetId : cue.targetIds()) {
            if (targetId != null && !targetId.isBlank() && !targetId.equals(cue.actorId())) return true;
        }
        return false;
    }

    private static boolean targetsOnlyActor(BattleActionTimelineState.Cue cue) {
        if (cue == null || cue.targetIds().isEmpty()) return false;
        for (String targetId : cue.targetIds()) {
            if (targetId == null || targetId.isBlank() || !targetId.equals(cue.actorId())) return false;
        }
        return true;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
