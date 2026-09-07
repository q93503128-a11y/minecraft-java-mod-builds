package kr.moonseungjun.riftfrontier.expedition;

/**
 * Immutable authoritative facts captured when a production expedition starts.
 *
 * These values are persisted with the run so later field-play review does not have to reconstruct
 * historical pressure, encounter composition, or hazard tuning from the current world state.
 * Legacy runs may legitimately have no start context and use explicit reconstruction fallback.
 */
public record ExpeditionStartContext(
    int regionPressure,
    int preparationSupplyCost,
    int plannedHunters,
    int plannedScouts,
    int plannedElites,
    int hazardTicks,
    int hazardAmplifier
) {
    public ExpeditionStartContext {
        if (regionPressure < 0) throw new IllegalArgumentException("regionPressure must be >= 0");
        if (preparationSupplyCost <= 0) throw new IllegalArgumentException("preparationSupplyCost must be > 0");
        if (plannedHunters < 0 || plannedScouts < 0 || plannedElites < 0) {
            throw new IllegalArgumentException("planned threat counts must be >= 0");
        }
        if (hazardTicks < 0 || hazardAmplifier < 0) {
            throw new IllegalArgumentException("hazard values must be >= 0");
        }
    }

    public int plannedThreats() {
        return Math.addExact(Math.addExact(plannedHunters, plannedScouts), plannedElites);
    }
}
