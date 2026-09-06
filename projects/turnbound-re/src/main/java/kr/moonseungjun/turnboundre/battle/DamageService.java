package kr.moonseungjun.turnboundre.battle;

import java.util.Locale;

/** Canonical deterministic HP/Poise damage calculation for M1. */
public final class DamageService {
    public static final double DEFAULT_CRIT_CHANCE = 0.05D;
    public static final double DEFAULT_CRIT_MULTIPLIER = 1.50D;
    public static final double EXPOSED_MULTIPLIER = 1.20D;
    public static final double VARIANCE_MIN = 0.95D;
    public static final double VARIANCE_MAX = 1.05D;

    private DamageService() {}

    public static DamageResult resolve(DamageRequest request, DeterministicBattleRng rng) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        if (rng == null) throw new IllegalArgumentException("rng must not be null");

        // Every damage resolution consumes exactly two draws in this order.
        boolean critical = rng.nextUnitDouble() < request.criticalChance();
        double variance = rng.nextDouble(VARIANCE_MIN, VARIANCE_MAX);
        double raw = request.hpPower() * request.attack() / (request.defense() + 100.0D);
        double affinity = request.affinity().hpMultiplier();
        double criticalMultiplier = critical ? request.criticalMultiplier() : 1.0D;
        double exposedMultiplier = request.exposed() ? EXPOSED_MULTIPLIER : 1.0D;
        double combined = affinity * criticalMultiplier * exposedMultiplier * variance * request.otherMultiplier();

        int finalDamage;
        if (request.affinity() == AffinityGrade.IMMUNE) {
            finalDamage = 0;
        } else {
            finalDamage = (int) Math.floor(Math.max(1.0D, raw * combined));
        }

        int poiseDamage = request.affinity() == AffinityGrade.IMMUNE
                ? 0
                : Math.max(0, (int) Math.floor(request.poisePower() * request.affinity().poiseMultiplier()));

        return new DamageResult(
                request.tag(), request.affinity(), raw, affinity, critical, criticalMultiplier,
                exposedMultiplier, variance, request.otherMultiplier(), finalDamage, poiseDamage, rng.draws());
    }

    public record DamageRequest(
            DamageTag tag,
            AffinityGrade affinity,
            int hpPower,
            int poisePower,
            int attack,
            int defense,
            boolean exposed,
            double criticalChance,
            double criticalMultiplier,
            double otherMultiplier
    ) {
        public DamageRequest {
            if (tag == null) throw new IllegalArgumentException("tag must not be null");
            if (affinity == null) throw new IllegalArgumentException("affinity must not be null");
            if (hpPower < 0) throw new IllegalArgumentException("hpPower must be >= 0");
            if (poisePower < 0) throw new IllegalArgumentException("poisePower must be >= 0");
            if (attack < 0) throw new IllegalArgumentException("attack must be >= 0");
            if (defense < 0) throw new IllegalArgumentException("defense must be >= 0");
            if (criticalChance < 0.0D || criticalChance > 1.0D) throw new IllegalArgumentException("criticalChance must be in [0,1]");
            if (criticalMultiplier < 1.0D) throw new IllegalArgumentException("criticalMultiplier must be >= 1");
            if (otherMultiplier < 0.0D || !Double.isFinite(otherMultiplier)) throw new IllegalArgumentException("otherMultiplier must be finite and >= 0");
        }

        public static DamageRequest standard(
                DamageTag tag, AffinityGrade affinity, int hpPower, int poisePower,
                int attack, int defense, boolean exposed
        ) {
            return new DamageRequest(tag, affinity, hpPower, poisePower, attack, defense, exposed,
                    DEFAULT_CRIT_CHANCE, DEFAULT_CRIT_MULTIPLIER, 1.0D);
        }
    }

    public record DamageResult(
            DamageTag tag,
            AffinityGrade affinity,
            double raw,
            double affinityMultiplier,
            boolean critical,
            double criticalMultiplier,
            double exposedMultiplier,
            double variance,
            double otherMultiplier,
            int finalDamage,
            int poiseDamage,
            long rngDrawsAfter
    ) {
        public String breakdown() {
            return String.format(Locale.ROOT,
                    "tag=%s affinity=%s raw=%.6f affinityMult=%.2f crit=%s critMult=%.2f exposedMult=%.2f variance=%.6f otherMult=%.6f final=%d poise=%d rngDraws=%d",
                    tag, affinity, raw, affinityMultiplier, critical, criticalMultiplier,
                    exposedMultiplier, variance, otherMultiplier, finalDamage, poiseDamage, rngDrawsAfter);
        }
    }
}
