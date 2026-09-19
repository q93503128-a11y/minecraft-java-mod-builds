package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;

/** Pure fail-closed activation rules for surveyed Drehmal field encounters. */
final class DrehmalEncounterActivationRules {
    private DrehmalEncounterActivationRules() {}

    static boolean ready(DrehmalFirstRouteCatalog.EncounterSlot encounter) {
        if (encounter == null) return false;
        DrehmalFirstRouteCatalog.Site site = DrehmalFirstRouteCatalog.site(encounter.siteLocator());
        DrehmalFirstRouteCatalog.Footprint footprint = DrehmalFirstRouteCatalog.footprint(encounter.footprintLocator());
        DrehmalFirstRouteCatalog.Patrol patrol = encounter.patrolLocator().isBlank()
                ? null
                : DrehmalFirstRouteCatalog.patrol(encounter.patrolLocator());
        return ready(encounter, site, footprint, patrol);
    }

    static boolean ready(
            DrehmalFirstRouteCatalog.EncounterSlot encounter,
            DrehmalFirstRouteCatalog.Site site,
            DrehmalFirstRouteCatalog.Footprint footprint,
            DrehmalFirstRouteCatalog.Patrol patrol
    ) {
        if (encounter == null || site == null || footprint == null) return false;
        if (!encounter.productionEnabled() || !encounter.verifiedIn26_2()
                || encounter.combatEncounterId().isBlank()) return false;
        if (!site.productionEnabled() || !site.verifiedIn26_2() || site.runtimePosition() == null) return false;
        if (!footprint.productionEnabled() || !footprint.verifiedIn26_2()
                || footprint.candidates().size() < 2) return false;
        if (!encounter.patrolLocator().isBlank()) {
            if (patrol == null || !patrol.productionEnabled() || !patrol.verifiedIn26_2()
                    || patrol.points().size() < 2) return false;
        }
        return true;
    }

    static int respawnTicks(int authoredSeconds, BattleOutcome outcome) {
        if (outcome == BattleOutcome.ALLY_VICTORY) return Math.max(40, authoredSeconds * 20);
        return 40;
    }
}
