package kr.moonseungjun.anomalousclasses.state;

/** Class-specific human HP from reviewed level-growth proposals. */
public final class LevelHealthGrowth {
    private LevelHealthGrowth() {}
    public static double bonus(ClassId classId, int level) {
        double perLevel = switch (classId) {
            case MENTAL_DOMINATOR -> .12;
            case PUPPETEER -> .06;
            case BARRIER_CASTER -> .045;
            // Parasite human form is governed by ParasiteRules.humanMaximum().
            // Pact and swarm formulas are intentionally not invented.
            default -> 0.0;
        };
        return Math.min(500.0, Math.max(0.0, Math.max(1, level) - 1.0) * perLevel);
    }
}
