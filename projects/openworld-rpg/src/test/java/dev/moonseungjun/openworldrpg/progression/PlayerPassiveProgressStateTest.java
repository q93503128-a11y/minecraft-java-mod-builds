package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerPassiveProgressStateTest {
    @Test
    void insightsGrantOnlyFivePointCreditsPerRoot() {
        var state = PlayerPassiveProgressState.initial();
        for (ClassInsight insight : ClassInsight.forRoot(RootClass.HUNTER)) {
            state = state.beginInsight(insight);
            state = state.commitInsight(insight);
        }
        assertEquals(8, state.completedInsightCount(RootClass.HUNTER));
        assertEquals(30, ClassPassiveAllocationRules.availablePoints(50, 8));
        assertEquals(6, ClassPassiveAllocationRules.availablePoints(2, 8));
    }

    @Test
    void pendingRespecPersistsStableTargetAndTransaction() {
        var state = PlayerPassiveProgressState.initial()
                .prepareRespec(
                        RootClass.MAGE,
                        Optional.of(ClassSpecialization.MAGE_ARCANIST),
                        260L
                );
        var pending = state.pendingRespec().orElseThrow();
        assertEquals("openworld_rpg:passive_respec/1", pending.transactionId());
        assertEquals("openworld_rpg:passive_respec/1/gold", pending.debitTransactionId());
        assertEquals(1L, pending.sequence());

        var committed = state.commitRespec(pending.transactionId(), Map.of());
        assertTrue(committed.pendingRespec().isEmpty());
        assertEquals(1L, committed.respecSequence());
    }

    @Test
    void stateSurvivesCodecRoundTrip() {
        ClassPassiveNodeSpec node =
                ClassPassiveCatalog.bundled().rootNodes(RootClass.WARRIOR).get(0);
        var state = PlayerPassiveProgressState.initial()
                .withAllocationRank(node.id(), 1)
                .beginInsight(ClassInsight.WARRIOR_R01_BREAK_THE_CHARGE);
        state.validateAgainst(ClassPassiveCatalog.bundled());

        var encoded = PlayerPassiveProgressState.CODEC
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow();
        var decoded = PlayerPassiveProgressState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(state, decoded);
        assertFalse(decoded.pendingInsightIds().isEmpty());
    }
}
