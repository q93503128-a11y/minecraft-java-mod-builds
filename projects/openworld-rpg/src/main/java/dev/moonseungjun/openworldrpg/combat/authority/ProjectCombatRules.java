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

    /**
     * Canonical player MaxHP. The final value is rounded once after level scale and VIT scaling,
     * matching the locked reference anchors in COMBAT_BALANCE.md (Lv8/VIT7=143, Lv80/VIT60=935).
     */
    public static int maxPlayerHealth(
            int level,
            double vitality,
            double maxHealthPercentBonus
    ) {
        requireContentLevel(level);
        requireFinite("vitality", vitality);
        requireFinite("maxHealthPercentBonus", maxHealthPercentBonus);
        if (vitality < 0.0) {
            throw new IllegalArgumentException("vitality must be non-negative.");
        }
        if (1.0 + maxHealthPercentBonus <= 0.0) {
            throw new IllegalArgumentException("MaxHP percent bonus must keep MaxHP positive.");
        }

        double x = Math.max(0.0, vitality - 5.0);
        double vitalityMultiplier = 1.0
                + 0.018 * Math.min(x, 25.0)
                + 0.010 * Math.min(Math.max(x - 25.0, 0.0), 30.0)
                + 0.005 * Math.max(x - 55.0, 0.0);
        double unroundedBaseHp = 100.0 * gearScale(level);
        return (int) Math.round(
                unroundedBaseHp
                        * vitalityMultiplier
                        * (1.0 + maxHealthPercentBonus)
        );
    }

    public static int benchmarkVitality(int level) {
        requireContentLevel(level);
        return 5 + (int) Math.round(0.25 * (level - 1));
    }

    public static int benchmarkPlayerHealth(int level) {
        return maxPlayerHealth(level, benchmarkVitality(level), 0.0);
    }

    /**
     * Converts an authored post-mitigation benchmark share into raw physical enemy damage.
     *
     * <p>COMBAT_BALANCE.md locks physical enemy-damage authoring to a neutral same-level player
     * with 25% expected physical mitigation. Runtime still applies the real target Defense.</p>
     */
    public static double rawEnemyPhysicalDamageFromBenchmarkShare(
            int attackerLevel,
            double benchmarkHpShare
    ) {
        requireContentLevel(attackerLevel);
        requireFiniteNonNegative("benchmarkHpShare", benchmarkHpShare);
        if (benchmarkHpShare > 1.0) {
            throw new IllegalArgumentException(
                    "benchmarkHpShare must be inside [0, 1]."
            );
        }
        return benchmarkPlayerHealth(attackerLevel) * benchmarkHpShare / 0.75;
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
