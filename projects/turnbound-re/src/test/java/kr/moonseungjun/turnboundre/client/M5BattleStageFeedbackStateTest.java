package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleStageFeedbackStateTest {
    private static final long T0 = 1_000_000_000L;

    @AfterEach
    void clearState() {
        BattleStageFeedbackState.clear();
    }

    @Test
    void initialSnapshotDoesNotManufactureCombatFeedback() {
        UUID battleId = UUID.randomUUID();
        BattleNetworkPayloads.DecodedSnapshot first = snapshot(battleId, 1L, participant("p1", 100, 50, false, true));

        BattleStageFeedbackState.acceptSnapshot(null, first, T0);

        assertTrue(BattleStageFeedbackState.cue(battleId, "p1", T0 + 1L).isEmpty());
    }

    @Test
    void authoritativeHpAndPoiseLossProduceShortLivedImpact() {
        UUID battleId = UUID.randomUUID();
        BattleNetworkPayloads.DecodedSnapshot before = snapshot(battleId, 1L, participant("p1", 100, 50, false, true));
        BattleNetworkPayloads.DecodedSnapshot after = snapshot(battleId, 2L, participant("p1", 72, 18, false, true));
        BattleStageFeedbackState.acceptSnapshot(null, before, T0);
        BattleStageFeedbackState.acceptSnapshot(before, after, T0 + 10_000_000L);

        BattleStageFeedbackState.Cue cue = BattleStageFeedbackState
                .cue(battleId, "p1", T0 + 110_000_000L).orElseThrow();
        assertEquals(-28, cue.hpDelta());
        assertEquals(-32, cue.poiseDelta());
        assertTrue(cue.hpStrength() > 0.0D);
        assertTrue(cue.poiseStrength() > 0.0D);
        assertTrue(cue.impactStrength() > 0.0D);

        assertTrue(BattleStageFeedbackState.cue(battleId, "p1", T0 + 1_000_000_000L).isEmpty());
    }

    @Test
    void healingFloatsPositiveHpWithoutHurtRecoil() {
        UUID battleId = UUID.randomUUID();
        BattleNetworkPayloads.DecodedSnapshot before = snapshot(battleId, 4L, participant("p1", 40, 30, false, true));
        BattleNetworkPayloads.DecodedSnapshot after = snapshot(battleId, 5L, participant("p1", 65, 30, false, true));
        BattleStageFeedbackState.acceptSnapshot(null, before, T0);
        BattleStageFeedbackState.acceptSnapshot(before, after, T0 + 10_000_000L);

        BattleStageFeedbackState.Cue cue = BattleStageFeedbackState
                .cue(battleId, "p1", T0 + 100_000_000L).orElseThrow();
        assertEquals(25, cue.hpDelta());
        assertTrue(cue.hpStrength() > 0.0D);
        assertEquals(0.0D, cue.impactStrength(), 0.000001D);
    }

    @Test
    void exposedAndDefeatTransitionsArePresentationCuesOnly() {
        UUID battleId = UUID.randomUUID();
        BattleNetworkPayloads.DecodedSnapshot before = snapshot(
                battleId, 7L,
                participant("p1", 100, 30, false, true),
                participant("e1", 20, 10, false, true));
        BattleNetworkPayloads.DecodedSnapshot after = snapshot(
                battleId, 8L,
                participant("p1", 100, 0, true, true),
                participant("e1", 0, 0, false, false));
        BattleStageFeedbackState.acceptSnapshot(null, before, T0);
        BattleStageFeedbackState.acceptSnapshot(before, after, T0 + 20_000_000L);

        BattleStageFeedbackState.Cue exposed = BattleStageFeedbackState
                .cue(battleId, "p1", T0 + 120_000_000L).orElseThrow();
        BattleStageFeedbackState.Cue defeated = BattleStageFeedbackState
                .cue(battleId, "e1", T0 + 120_000_000L).orElseThrow();
        assertTrue(exposed.exposedStrength() > 0.0D);
        assertTrue(defeated.defeatStrength() > 0.0D);
    }

    @Test
    void aNewBattleCannotInheritOldParticipantFeedback() {
        UUID oldBattle = UUID.randomUUID();
        UUID newBattle = UUID.randomUUID();
        BattleNetworkPayloads.DecodedSnapshot before = snapshot(oldBattle, 1L, participant("same", 100, 40, false, true));
        BattleNetworkPayloads.DecodedSnapshot hit = snapshot(oldBattle, 2L, participant("same", 50, 20, false, true));
        BattleStageFeedbackState.acceptSnapshot(null, before, T0);
        BattleStageFeedbackState.acceptSnapshot(before, hit, T0 + 10_000_000L);
        assertFalse(BattleStageFeedbackState.cue(oldBattle, "same", T0 + 20_000_000L).isEmpty());

        BattleNetworkPayloads.DecodedSnapshot fresh = snapshot(newBattle, 1L, participant("same", 100, 40, false, true));
        BattleStageFeedbackState.acceptSnapshot(hit, fresh, T0 + 30_000_000L);

        assertTrue(BattleStageFeedbackState.cue(oldBattle, "same", T0 + 40_000_000L).isEmpty());
        assertTrue(BattleStageFeedbackState.cue(newBattle, "same", T0 + 40_000_000L).isEmpty());
    }

    private static BattleNetworkPayloads.DecodedSnapshot snapshot(
            UUID battleId,
            long revision,
            BattleNetworkPayloads.SnapshotParticipant... participants
    ) {
        return new BattleNetworkPayloads.DecodedSnapshot(
                battleId, revision, "AWAIT_COMMAND", 1, participants[0].id(), List.of(participants), List.of());
    }

    private static BattleNetworkPayloads.SnapshotParticipant participant(
            String id,
            int hp,
            int poise,
            boolean exposed,
            boolean alive
    ) {
        return new BattleNetworkPayloads.SnapshotParticipant(
                id,
                hp,
                100,
                poise,
                50,
                0,
                false,
                exposed,
                false,
                alive,
                id.startsWith("e") ? "ENEMY" : "PLAYER",
                id.startsWith("e") ? 1 : 0,
                "turnbound_re:zombie",
                List.of(),
                null,
                null);
    }
}
