package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardGrantServiceTest {
    @Test
    void sameTransactionDoesNotDoubleGrantAfterSaveRoundTrip() {
        UUID playerId = UUID.randomUUID();
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            CampaignProgressStore.markClean(playerId);
            BattleState state = P0Scenario.create();
            long goldBefore = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);

            RewardGrantService.Result first = RewardGrantService.commit(
                    playerId, "tx-roundtrip", "ENC_M01", state, BattleOutcome.ALLY_VICTORY, () -> { });
            assertFalse(first.duplicate());
            long goldAfter = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);
            assertTrue(goldAfter > goldBefore);

            String encoded = CampaignSaveCodec.encode(CampaignProgressStore.snapshot(playerId));
            CampaignProgressStore.restore(playerId, CampaignSaveCodec.decode(encoded));
            RewardGrantService.Result duplicate = RewardGrantService.commit(
                    playerId, "tx-roundtrip", "ENC_M01", state, BattleOutcome.ALLY_VICTORY, () -> { });

            assertTrue(duplicate.duplicate());
            assertEquals(goldAfter, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD));
        } finally {
            RewardGrantService.resetForTests();
            CampaignProgressStore.resetForTests(playerId);
        }
    }

    @Test
    void failedPersistenceRollsBackEntireRewardTransaction() {
        UUID playerId = UUID.randomUUID();
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            CampaignProgressStore.markClean(playerId);
            CampaignProgressStore.Snapshot before = CampaignProgressStore.snapshot(playerId);

            assertThrows(IllegalStateException.class, () -> RewardGrantService.commit(
                    playerId, "tx-save-fail", "ENC_M01", P0Scenario.create(), BattleOutcome.ALLY_VICTORY,
                    () -> { throw new IOException("forced"); }));

            assertEquals(before, CampaignProgressStore.snapshot(playerId));
            assertFalse(CampaignProgressStore.isDirty(playerId));
        } finally {
            RewardGrantService.resetForTests();
            CampaignProgressStore.resetForTests(playerId);
        }
    }

    @Test
    void nestedRewardCommitIsRejectedAndOuterTransactionRollsBack() {
        UUID playerId = UUID.randomUUID();
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            CampaignProgressStore.markClean(playerId);
            CampaignProgressStore.Snapshot before = CampaignProgressStore.snapshot(playerId);
            BattleState state = P0Scenario.create();

            assertThrows(IllegalStateException.class, () -> RewardGrantService.commit(
                    playerId, "tx-outer", "ENC_M01", state, BattleOutcome.ALLY_VICTORY,
                    () -> RewardGrantService.commit(
                            playerId, "tx-inner", "ENC_M01", state, BattleOutcome.ALLY_VICTORY, () -> { })));

            assertEquals(before, CampaignProgressStore.snapshot(playerId));
            assertFalse(CampaignProgressStore.isDirty(playerId));
        } finally {
            RewardGrantService.resetForTests();
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
