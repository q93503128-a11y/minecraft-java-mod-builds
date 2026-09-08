package kr.moonseungjun.turnboundre.network;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M5BattleResultPayloadTest {
    @Test
    void resultRoundTripPreservesAuthoritativeRewardFacts() {
        var view = new BattleResultNetworkPayloads.ResultView(
                UUID.fromString("00000000-0000-0000-0000-000000009101"),
                42L,
                "VICTORY",
                125,
                7,
                900,
                33,
                List.of(new BattleResultNetworkPayloads.ShardView("turnbound_re:zombie", 3, 11)));

        assertEquals(view, BattleResultNetworkPayloads.ResultS2C.from(view).decode());
        assertTrue(view.victory());
        assertTrue(view.hasRewards());
    }

    @Test
    void rewardlessDefeatStaysExplicitlyRewardless() {
        var view = new BattleResultNetworkPayloads.ResultView(
                UUID.fromString("00000000-0000-0000-0000-000000009102"),
                9L, "DEFEAT", 0, 0, 0, 0, List.of());
        assertFalse(view.victory());
        assertFalse(view.hasRewards());
        assertEquals(view, BattleResultNetworkPayloads.ResultS2C.from(view).decode());
    }

    @Test
    void acknowledgementAndCloseRoundTrip() {
        var view = new BattleResultNetworkPayloads.ResultView(
                UUID.fromString("00000000-0000-0000-0000-000000009103"),
                15L, "VICTORY", 0, 0, 0, 0, List.of());
        var ack = BattleResultNetworkPayloads.AcknowledgeResultC2S.from(view).decode();
        assertEquals(view.battleId(), ack.battleId());
        assertEquals(view.revision(), ack.revision());
        assertEquals(view.outcome(), ack.outcome());

        var closed = BattleResultNetworkPayloads.ResultClosedS2C.of(view.battleId(), false, "STALE_RESULT").decode();
        assertEquals(view.battleId(), closed.battleId());
        assertFalse(closed.accepted());
        assertEquals("STALE_RESULT", closed.code());
    }

    @Test
    void malformedOrImpossibleResultFactsAreRejected() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000009104");
        assertThrows(IllegalArgumentException.class,
                () -> new BattleResultNetworkPayloads.ResultView(id, 0, "ONGOING", 0, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new BattleResultNetworkPayloads.ShardView("turnbound_re:zombie", 5, 4));
    }
}
