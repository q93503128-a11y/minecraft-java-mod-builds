package dev.moonseungjun.openworldrpg.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PlayerDeathPenaltyStateTest {
    @Test
    void xpPenaltyPersistsExactBeforeAfterValues() {
        var prepared = PlayerDeathPenaltyState.initial().prepare(
                PlayerDeathPenaltyState.PenaltyKind.CURRENT_LEVEL_XP,
                8,
                500,
                30
        );
        var pending = prepared.pendingPenalty().orElseThrow();

        assertEquals(
                "openworld_rpg:death_penalty/1",
                pending.transactionId()
        );
        assertEquals(500L, pending.beforeValue());
        assertEquals(470L, pending.afterValue());
        assertEquals(30L, pending.amount());

        var committed = prepared.commit(pending.transactionId());
        assertTrue(committed.pendingPenalty().isEmpty());
        assertEquals(1L, committed.transactionSequence());
    }

    @Test
    void goldPenaltyMayPrepareNegativeResult() {
        var pending = PlayerDeathPenaltyState.initial().prepare(
                PlayerDeathPenaltyState.PenaltyKind.GOLD,
                20,
                50,
                200
        ).pendingPenalty().orElseThrow();

        assertEquals(-150L, pending.afterValue());
        assertEquals(
                "openworld_rpg:death_penalty/1/gold",
                pending.effectTransactionId()
        );
    }

    @Test
    void pendingPenaltySurvivesCodecRoundTrip() {
        var original = PlayerDeathPenaltyState.initial().prepare(
                PlayerDeathPenaltyState.PenaltyKind.GOLD,
                40,
                -200,
                460
        );

        var encoded = PlayerDeathPenaltyState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerDeathPenaltyState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
