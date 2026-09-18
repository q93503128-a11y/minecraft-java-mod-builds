package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6ExpeditionRouteTrackingTest {
    private static final ExpeditionNetworkPayloads.EncounterView PATROL =
            new ExpeditionNetworkPayloads.EncounterView(
                    "turnbound_re:debug_overworld_patrol",
                    1,
                    3,
                    true,
                    List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider"),
                    "turnbound_re:region_01/overworld_patrol",
                    "minecraft:overworld",
                    325,
                    71,
                    290);

    @AfterEach
    void reset() {
        ExpeditionJournalClientState.clearTracking();
    }

    @Test
    void routeToggleSurvivesJournalViewRefresh() {
        assertTrue(ExpeditionJournalClientState.toggleTracking(PATROL));
        assertTrue(ExpeditionJournalClientState.isTracking(PATROL.locator()));

        ExpeditionJournalClientState.clearView();

        assertTrue(ExpeditionJournalClientState.isTracking(PATROL.locator()));
        assertFalse(ExpeditionJournalClientState.toggleTracking(PATROL));
        assertFalse(ExpeditionJournalClientState.isTracking(PATROL.locator()));
    }
}
