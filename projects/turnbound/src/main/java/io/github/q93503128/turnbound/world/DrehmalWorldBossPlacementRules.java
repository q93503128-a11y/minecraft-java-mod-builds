package io.github.q93503128.turnbound.world;

/** Pure constraints for the optional Capital Valley world-boss meadow resolver. */
final class DrehmalWorldBossPlacementRules {
    static final String ENCOUNTER_ID = "CV_WORLD_BOSS_GRAUL";
    static final String SITE_LOCATOR = "turnbound:site/capital_valley/graul_meadow";
    static final String FOOTPRINT_LOCATOR = "turnbound:footprint/capital_valley/graul_meadow";
    static final String SITE_KIND = "WORLD_BOSS_ZONE";

    static final int LOCAL_SEARCH_RADIUS = 24;
    static final int LOCAL_SEARCH_STEP = 6;
    static final int MAX_SHORTLIST = 48;
    static final double MIN_TOWER_DISTANCE = 120.0D;
    static final double MAX_TOWER_DISTANCE = 300.0D;
    static final double MIN_ROUTE_DISTANCE = 48.0D;
    static final double MAX_ROUTE_DISTANCE = 120.0D;
    static final double MIN_HUB_DISTANCE = 300.0D;
    static final int MAX_HEIGHT_SPREAD = 3;

    private DrehmalWorldBossPlacementRules() {}

    static boolean isGraul(DrehmalFirstRouteCatalog.EncounterSlot encounter) {
        return encounter != null && ENCOUNTER_ID.equals(encounter.combatEncounterId());
    }

    static boolean candidate(double towerDistance, double routeDistance, double hubDistance,
                             int heightSpread, boolean insideSafetyZone) {
        return !insideSafetyZone
                && Double.isFinite(towerDistance)
                && Double.isFinite(routeDistance)
                && Double.isFinite(hubDistance)
                && towerDistance >= MIN_TOWER_DISTANCE
                && towerDistance <= MAX_TOWER_DISTANCE
                && routeDistance >= MIN_ROUTE_DISTANCE
                && routeDistance <= MAX_ROUTE_DISTANCE
                && hubDistance >= MIN_HUB_DISTANCE
                && heightSpread >= 0
                && heightSpread <= MAX_HEIGHT_SPREAD;
    }

    static double score(double towerDistance, double routeDistance, int heightSpread) {
        double towerError = towerDistance - 190.0D;
        double roadError = routeDistance - 64.0D;
        return towerError * towerError * 0.03D
                + roadError * roadError * 0.20D
                + Math.max(0, heightSpread) * 45.0D;
    }
}
