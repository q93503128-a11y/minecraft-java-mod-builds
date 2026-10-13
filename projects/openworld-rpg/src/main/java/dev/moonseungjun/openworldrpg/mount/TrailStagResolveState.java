package dev.moonseungjun.openworldrpg.mount;

/** Server-side temporary Resolve buffer for one summoned Trail Stag. */
public record TrailStagResolveState(double maximum, double current) {
    public TrailStagResolveState {
        if (!Double.isFinite(maximum) || maximum <= 0.0) {
            throw new IllegalArgumentException("Trail Stag Resolve maximum must be positive and finite.");
        }
        if (!Double.isFinite(current) || current < 0.0 || current > maximum) {
            throw new IllegalArgumentException("Trail Stag Resolve current value is outside its legal range.");
        }
    }

    public static TrailStagResolveState fresh(double riderMaxHp) {
        double maximum = TrailStagMountRules.resolveMax(riderMaxHp);
        return new TrailStagResolveState(maximum, maximum);
    }

    /**
     * Consumes already-authoritative damage assigned to the mount. Damage mitigation remains owned by
     * the shared combat pipeline; this state only owns the Resolve buffer and reports true overflow.
     */
    public DamageResult apply(double damage) {
        if (!Double.isFinite(damage) || damage < 0.0) {
            throw new IllegalArgumentException("Trail Stag Resolve damage must be finite and non-negative.");
        }
        double consumed = Math.min(current, damage);
        double remaining = current - consumed;
        double overflow = Math.max(0.0, damage - consumed);
        return new DamageResult(
                new TrailStagResolveState(maximum, remaining),
                consumed,
                overflow,
                remaining == 0.0 && current > 0.0 && damage > 0.0
        );
    }

    public record DamageResult(
            TrailStagResolveState state,
            double resolveConsumed,
            double overflowDamage,
            boolean brokeResolve
    ) {
    }
}
