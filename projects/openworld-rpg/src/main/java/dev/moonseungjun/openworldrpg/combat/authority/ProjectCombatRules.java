package dev.moonseungjun.openworldrpg.combat.authority;

/**
 * Canonical pure combat math shared by server-authoritative impact transactions.
 */
public final class ProjectCombatRules {
    public static final int MIN_CONTENT_LEVEL = 1;
    public static final int MAX_CONTENT_LEVEL = 80;
    public static final double MAX_NORMAL_DEFENSE_MITIGATION = 0.70;
    public static final double MAX_ROUTINE_TOTAL_MITIGATION = 0.80;

    private ProjectCombatRules() {
    }

    public static double gearScale(int level) {
        requireContentLevel(level);
        return 1.0 + 0.055 * (level - 1);
    }

    public static double baseHp(int level) {
        return Math.round(100.0 * gearScale(level));
    }

    public static double attributeDamageMultiplier(double weightedStat) {
        requireFinite("weightedStat", weightedStat);
        double x = weightedStat - 5.0;
        double multiplier = 1.0
                + 0.012 * Math.min(x, 25.0)
                + 0.008 * Math.min(Math.max(x - 25.0, 0.0), 30.0)
                + 0.004 * Math.max(x - 55.0, 0.0);
        return Math.max(0.70, multiplier);
    }

    public static double defenseTakenMultiplier(int attackerLevel, double effectiveDefense) {
        requireContentLevel(attackerLevel);
        requireFiniteNonNegative("effectiveDefense", effectiveDefense);

        double mitigationK = 75.0 * gearScale(attackerLevel);
        double uncappedTaken = mitigationK / (mitigationK + effectiveDefense);
        return Math.max(1.0 - MAX_NORMAL_DEFENSE_MITIGATION, uncappedTaken);
    }

    public static double routineTakenMultiplier(
            double defenseTakenMultiplier,
            double authoredDamageTakenMultiplier,
            double authoredDamageReduction
    ) {
        requireFinitePositive("defenseTakenMultiplier", defenseTakenMultiplier);
        requireFinitePositive("authoredDamageTakenMultiplier", authoredDamageTakenMultiplier);
        requireFinite("authoredDamageReduction", authoredDamageReduction);
        if (authoredDamageReduction < 0.0 || authoredDamageReduction >= 1.0) {
            throw new IllegalArgumentException("authoredDamageReduction must be inside [0, 1).");
        }

        double combined = defenseTakenMultiplier
                * authoredDamageTakenMultiplier
                * (1.0 - authoredDamageReduction);
        return Math.max(1.0 - MAX_ROUTINE_TOTAL_MITIGATION, combined);
    }

    public static double roundFinal(double amount) {
        requireFiniteNonNegative("amount", amount);
        return Math.round(amount);
    }

    private static void requireContentLevel(int level) {
        if (level < MIN_CONTENT_LEVEL || level > MAX_CONTENT_LEVEL) {
            throw new IllegalArgumentException("Content level must be inside [1, 80]: " + level);
        }
    }

    static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }

    static void requireFiniteNonNegative(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }

    static void requireFinitePositive(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
