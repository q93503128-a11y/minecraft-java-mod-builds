package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.TrainingBattleFactory;
import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSnapshotCodecTest {
    @Test
    void trainingBattleHasNineUnitsAndEightPreviewSlots(){
        var s=TrainingBattleFactory.create();
        assertEquals(9,s.combatants().size());
        assertEquals(8,s.timelinePreview(8).size());
    }

    @Test
    void snapshotUsesV1SignatureCountersInsteadOfRetiredV04PresentationKeys() {
        CombatantState lynette = hero("P05");
        lynette.setCounter("shot", 1);
        lynette.setCounter("p05_hunt_actions", 2);
        var lynetteTokens = BattleSnapshotCodec.presentationStates(lynette);
        assertTrue(lynetteTokens.contains("@r:shot:1:2"));
        assertFalse(lynetteTokens.stream().anyMatch(token -> token.startsWith("@r:hunt:")));

        CombatantState morwen = hero("P06");
        morwen.setCounter("records", 4);
        morwen.setCounter("memory", 5);
        var morwenTokens = BattleSnapshotCodec.presentationStates(morwen);
        assertTrue(morwenTokens.contains("@r:records:4:5"));
        assertFalse(morwenTokens.stream().anyMatch(token -> token.startsWith("@r:memory:")));

        CombatantState marion = hero("P07");
        marion.setCounter("bond", 65);
        marion.setCounter("contract_prep", 2);
        var marionTokens = BattleSnapshotCodec.presentationStates(marion);
        assertTrue(marionTokens.contains("@r:bond:65:100"));
        assertFalse(marionTokens.stream().anyMatch(token -> token.startsWith("@r:contract:")));
    }

    private static CombatantState hero(String id) {
        return new CombatantState(id.toLowerCase(), CanonicalData.definition(id), CombatantSide.ALLY, 0);
    }
}
