package dev.moonseungjun.augmentationsurvival.threat;

/** Canonical save-wide power-score boundaries for threat stages 0 through 8. */
public final class ThreatCurve {
    private static final double[] MIN_SCORE = {
            0.0,
            25.0,
            60.0,
            110.0,
            175.0,
            260.0,
            365.0,
            490.0,
            640.0
    };

    private ThreatCurve() {
    }

    public static int stageForScore(double score) {
        if (!Double.isFinite(score) || score < 0.0) {
            throw new IllegalArgumentException("Threat power score must be finite and non-negative");
        }
        for (int stage = MIN_SCORE.length - 1; stage >= 0; stage--) {
            if (score >= MIN_SCORE[stage]) {
                return stage;
            }
        }
        return 0;
    }

    public static double minScore(int stage) {
        if (stage < 0 || stage >= MIN_SCORE.length) {
            throw new IllegalArgumentException("Threat stage out of range: " + stage);
        }
        return MIN_SCORE[stage];
    }

    public static int maxStage() {
        return MIN_SCORE.length - 1;
    }
}
