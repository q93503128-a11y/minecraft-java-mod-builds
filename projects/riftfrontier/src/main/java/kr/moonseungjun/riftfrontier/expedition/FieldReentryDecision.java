package kr.moonseungjun.riftfrontier.expedition;

/**
 * Pure decision boundary for M2 technical-field re-entry.
 *
 * The adapter intentionally does not infer ownership from proximity alone while an expedition is
 * active. A player is only considered stranded when no authoritative expedition remains and their
 * login position is still inside the bounded Region 01 technical field cell.
 */
public record FieldReentryDecision(boolean returnToHub, String reason) {
    public static FieldReentryDecision evaluate(boolean expeditionActive, boolean insideTechnicalRegion) {
        if (expeditionActive) {
            return new FieldReentryDecision(false, "active expedition owns field position");
        }
        if (!insideTechnicalRegion) {
            return new FieldReentryDecision(false, "player is not inside technical field cell");
        }
        return new FieldReentryDecision(true, "terminal or reconciled expedition left player inside technical field cell");
    }
}
