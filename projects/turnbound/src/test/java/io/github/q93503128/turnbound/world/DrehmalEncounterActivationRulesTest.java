package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalEncounterActivationRulesTest {
    @Test
    void currentFirstRouteStaysDormantUntilSurveyPromotesSpatialData() {
        var first = DrehmalFirstRouteCatalog.encounterByCombatId("CV_FIRST_COMMON");
        assertFalse(DrehmalEncounterActivationRules.ready(first));
    }

    @Test
    void productionEncounterRequiresVerifiedSiteAndTwoArenaCandidates() {
        var position = new DrehmalFirstRouteCatalog.Position(10, 70, 20);
        var encounter = new DrehmalFirstRouteCatalog.EncounterSlot(
                "slot", "site", "COMMON", "footprint", "", "CV_FIRST_COMMON", "적", true, true);
        var site = new DrehmalFirstRouteCatalog.Site(
                "site", "ENCOUNTER_ZONE", "seed", "장소", position, 0, 24, true, true);
        var oneArena = new DrehmalFirstRouteCatalog.Footprint(
                "footprint", "site", 14, 4, 2,
                List.of(new DrehmalFirstRouteCatalog.ArenaCandidate(position, 0.0F)), true, true);
        var twoArenas = new DrehmalFirstRouteCatalog.Footprint(
                "footprint", "site", 14, 4, 2,
                List.of(
                        new DrehmalFirstRouteCatalog.ArenaCandidate(position, 0.0F),
                        new DrehmalFirstRouteCatalog.ArenaCandidate(new DrehmalFirstRouteCatalog.Position(14, 70, 24), 30.0F)),
                true, true);

        assertFalse(DrehmalEncounterActivationRules.ready(encounter, site, oneArena, null));
        assertTrue(DrehmalEncounterActivationRules.ready(encounter, site, twoArenas, null));
    }

    @Test
    void patrolBindingAlsoRequiresTwoVerifiedPatrolPoints() {
        var position = new DrehmalFirstRouteCatalog.Position(10, 70, 20);
        var encounter = new DrehmalFirstRouteCatalog.EncounterSlot(
                "slot", "site", "COMMON", "footprint", "patrol", "CV_FIRST_COMMON", "적", true, true);
        var site = new DrehmalFirstRouteCatalog.Site(
                "site", "PATROL_ZONE", "seed", "장소", position, 0, 24, true, true);
        var footprint = new DrehmalFirstRouteCatalog.Footprint(
                "footprint", "site", 14, 4, 2,
                List.of(
                        new DrehmalFirstRouteCatalog.ArenaCandidate(position, 0.0F),
                        new DrehmalFirstRouteCatalog.ArenaCandidate(new DrehmalFirstRouteCatalog.Position(14, 70, 24), 30.0F)),
                true, true);
        var invalidPatrol = new DrehmalFirstRouteCatalog.Patrol(
                "patrol", "site", List.of(position), true, true);
        var validPatrol = new DrehmalFirstRouteCatalog.Patrol(
                "patrol", "site", List.of(position, new DrehmalFirstRouteCatalog.Position(18, 70, 20)), true, true);

        assertFalse(DrehmalEncounterActivationRules.ready(encounter, site, footprint, invalidPatrol));
        assertTrue(DrehmalEncounterActivationRules.ready(encounter, site, footprint, validPatrol));
    }

    @Test
    void victoryUsesAuthoredRespawnWhileRetreatGetsShortRetryDelay() {
        assertEquals(1800, DrehmalEncounterActivationRules.respawnTicks(90, BattleOutcome.ALLY_VICTORY));
        assertEquals(40, DrehmalEncounterActivationRules.respawnTicks(90, BattleOutcome.RUNNING));
    }
}
