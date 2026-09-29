package io.github.q93503128.turnbound.world;

/** Pure scoring rules for source-aware first-route placement. */
final class DrehmalRoutePlacementRules {
    private DrehmalRoutePlacementRules() {}

    static double preferredRoadDistance(String siteKind) {
        return switch (siteKind == null ? "" : siteKind) {
            case "GUIDE_CANDIDATE", "NPC_ZONE" -> 3.5D;
            case "ENCOUNTER_ZONE", "PATROL_ZONE" -> 5.0D;
            case "ELITE_ZONE" -> 7.0D;
            default -> 0.0D;
        };
    }

    static boolean roadAware(String siteKind) {
        return preferredRoadDistance(siteKind) > 0.0D;
    }

    static boolean acceptableRoadDistance(String siteKind, double roadDistance) {
        if (!roadAware(siteKind)) return true;
        if (!Double.isFinite(roadDistance)) return false;
        return roadDistance >= 2.0D && roadDistance <= 18.0D;
    }

    static double score(String siteKind, double seedDistanceSq, double roadDistance) {
        double base = Math.max(0.0D, seedDistanceSq);
        double preferred = preferredRoadDistance(siteKind);
        if (preferred <= 0.0D || !Double.isFinite(roadDistance)) return base;
        double roadError = roadDistance - preferred;
        return base + roadError * roadError * 8.0D;
    }
}
