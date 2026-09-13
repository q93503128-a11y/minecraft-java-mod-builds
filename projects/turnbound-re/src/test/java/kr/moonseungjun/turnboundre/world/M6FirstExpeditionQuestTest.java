package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6FirstExpeditionQuestTest {
    @Test
    void firstExpeditionStageIsDerivedFromExistingWorldAndProgressState() {
        PlayerProgress fresh = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA,
                0L,
                0L,
                Map.of(),
                Map.of(),
                List.of(),
                1,
                Set.of(),
                Map.of(),
                Map.of());

        assertEquals(
                FirstExpeditionQuestService.Stage.FIND_REGION_WAYPOINT,
                FirstExpeditionQuestService.stage(fresh, Set.of(FirstExpeditionQuestService.HUB_WAYPOINT)));
        assertEquals(
                FirstExpeditionQuestService.Stage.DEFEAT_RIFT_VANGUARD,
                FirstExpeditionQuestService.stage(fresh, Set.of(
                        FirstExpeditionQuestService.HUB_WAYPOINT,
                        FirstExpeditionQuestService.REGION_WAYPOINT)));

        PlayerProgress completed = fresh.completeEncounterLocator(FirstExpeditionQuestService.RIFT_ELITE_LOCATOR);
        assertEquals(
                FirstExpeditionQuestService.Stage.COMPLETE,
                FirstExpeditionQuestService.stage(completed, Set.of()));
    }

    @Test
    void productionRegionKeepsPatrolRepeatableButMakesRiftVanguardOneTime() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var region = registry.regions().get(FunctionalWorldSliceLayout.REGION_ID);
        var patrol = region.encounterAnchors().stream()
                .filter(anchor -> FirstExpeditionQuestService.RIFT_ELITE_LOCATOR.equals(anchor.locator()))
                .findFirst();
        assertTrue(patrol.isPresent());
        assertFalse(patrol.get().repeatable());

        var repeatablePatrol = region.encounterAnchors().stream()
                .filter(anchor -> FunctionalWorldSliceLayout.OVERWORLD_PATROL.locator().equals(anchor.locator()))
                .findFirst();
        assertTrue(repeatablePatrol.isPresent());
        assertTrue(repeatablePatrol.get().repeatable());
    }
}
