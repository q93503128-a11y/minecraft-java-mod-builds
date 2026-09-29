package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class PlayerClassSwitchStateTest {
    @Test
    void pendingSwitchUsesMonotonicTransactionSequence() {
        var first = PlayerClassSwitchState.initial().prepare(
                RootClass.CLERIC,
                RootClass.MAGE,
                200
        );
        var pending = first.pendingSwitch().orElseThrow();

        assertEquals(1L, pending.sequence());
        assertEquals("openworld_rpg:class_switch/1", pending.transactionId());
        assertEquals(
                "openworld_rpg:class_switch/1/gold",
                pending.debitTransactionId()
        );
        assertEquals(RootClass.CLERIC, pending.sourceClass());
        assertEquals(RootClass.MAGE, pending.targetClass());
        assertEquals(200L, pending.goldCost());

        var committed = first.commit(pending.transactionId());
        assertTrue(committed.pendingSwitch().isEmpty());

        var second = committed.prepare(
                RootClass.MAGE,
                RootClass.WARRIOR,
                510
        );
        assertEquals(
                "openworld_rpg:class_switch/2",
                second.pendingSwitch().orElseThrow().transactionId()
        );
    }

    @Test
    void cancelledSequenceIsNeverReused() {
        var prepared = PlayerClassSwitchState.initial().prepare(
                RootClass.WARRIOR,
                RootClass.HUNTER,
                200
        );
        var cancelled = prepared.cancel(
                prepared.pendingSwitch().orElseThrow().transactionId()
        );
        var next = cancelled.prepare(
                RootClass.WARRIOR,
                RootClass.GUARDIAN,
                200
        );

        assertEquals(2L, next.transactionSequence());
    }

    @Test
    void invalidOrConflictingSwitchesFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerClassSwitchState.initial().prepare(
                        RootClass.MAGE,
                        RootClass.MAGE,
                        200
                )
        );

        var prepared = PlayerClassSwitchState.initial().prepare(
                RootClass.MAGE,
                RootClass.CLERIC,
                200
        );
        assertThrows(
                IllegalStateException.class,
                () -> prepared.prepare(
                        RootClass.MAGE,
                        RootClass.WARRIOR,
                        200
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> prepared.commit(
                        "openworld_rpg:class_switch/999"
                )
        );
    }

    @Test
    void insufficientGoldResultMayReportCanonicalDeathDebt() {
        var result = PlayerClassSwitchService.SwitchResult.rejected(
                PlayerClassSwitchService.SwitchStatus.INSUFFICIENT_GOLD,
                RootClass.MAGE,
                200,
                -80
        );

        assertEquals(-80L, result.goldAfter());
    }

    @Test
    void stateSurvivesCodecRoundTrip() {
        var original = PlayerClassSwitchState.initial().prepare(
                RootClass.GUARDIAN,
                RootClass.CLERIC,
                1290
        );

        var encoded = PlayerClassSwitchState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerClassSwitchState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
