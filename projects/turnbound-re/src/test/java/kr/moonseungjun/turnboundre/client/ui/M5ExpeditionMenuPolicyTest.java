package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5ExpeditionMenuPolicyTest {
    @Test
    void routeSelectionRequiresPartyAndNoPendingStart() {
        ExpeditionNetworkPayloads.EncounterView route =
                new ExpeditionNetworkPayloads.EncounterView("turnbound_re:test", 1, 2, true);
        ExpeditionNetworkPayloads.JournalView emptyParty =
                new ExpeditionNetworkPayloads.JournalView(List.of(), List.of(route), "", "");
        ExpeditionNetworkPayloads.JournalView ready =
                new ExpeditionNetworkPayloads.JournalView(List.of("turnbound_re:zombie"), List.of(route), "", "");

        assertFalse(ExpeditionMenuPolicy.canSelectEncounter(null, false));
        assertFalse(ExpeditionMenuPolicy.canSelectEncounter(emptyParty, false));
        assertTrue(ExpeditionMenuPolicy.canSelectEncounter(ready, false));
        assertFalse(ExpeditionMenuPolicy.canSelectEncounter(ready, true));
    }

    @Test
    void worldTransitionWaitsForBothRequestAndAuthoritativeBattleSnapshot() {
        assertFalse(ExpeditionMenuPolicy.shouldEnterBattle(false, false));
        assertFalse(ExpeditionMenuPolicy.shouldEnterBattle(true, false));
        assertFalse(ExpeditionMenuPolicy.shouldEnterBattle(false, true));
        assertTrue(ExpeditionMenuPolicy.shouldEnterBattle(true, true));
    }
}
