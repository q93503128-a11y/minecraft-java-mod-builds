package kr.moonseungjun.riftfrontier.expedition;

/** Pure presentation policy: derives relay readiness from existing authoritative objective progress. */
final class FieldRelayPresentationPolicy {
    private FieldRelayPresentationPolicy() {}

    static boolean extractionReady(int recoveredSalvage, int requiredSalvage) {
        if (requiredSalvage <= 0) throw new IllegalArgumentException("requiredSalvage must be positive");
        return recoveredSalvage >= requiredSalvage;
    }
}
