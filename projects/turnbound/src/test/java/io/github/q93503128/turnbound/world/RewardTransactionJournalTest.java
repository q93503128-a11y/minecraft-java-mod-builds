package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardTransactionJournalTest {
    @TempDir
    Path tempDir;

    @Test
    void failedPrimarySaveLeavesDurableJournalThatRecoversAfterRestart() throws Exception {
        UUID playerId = UUID.randomUUID();
        Path primary = tempDir.resolve("player.json");
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            CampaignProgressStore.markClean(playerId);
            CampaignProgressStore.Snapshot before = CampaignProgressStore.snapshot(playerId);
            CampaignSaveFiles.save(primary, before);
            long goldBefore = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);

            RewardGrantService.SettlementException failure = assertThrows(RewardGrantService.SettlementException.class,
                    () -> RewardGrantService.commit(playerId, "tx-recover", "ENC_M01", P0Scenario.create(), BattleOutcome.ALLY_VICTORY,
                            snapshot -> RewardTransactionJournal.prepare(primary, "tx-recover", snapshot),
                            () -> { throw new IOException("forced primary failure"); },
                            () -> RewardTransactionJournal.clear(primary)));
            assertTrue(failure.recoverableFromJournal());
            assertEquals(before, CampaignProgressStore.snapshot(playerId));
            assertTrue(Files.exists(RewardTransactionJournal.journalPath(primary)));

            CampaignProgressStore.removeRuntime(playerId);
            CampaignProgressStore.restore(playerId, CampaignSaveFiles.load(primary).orElseThrow().snapshot());
            assertEquals(RewardTransactionJournal.Recovery.APPLIED, RewardTransactionJournal.recover(primary, playerId));
            assertTrue(CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD) > goldBefore);
            assertTrue(RewardGrantService.transactionCommitted(CampaignProgressStore.snapshot(playerId), "tx-recover"));
            assertFalse(Files.exists(RewardTransactionJournal.journalPath(primary)));
            assertEquals(CampaignProgressStore.snapshot(playerId), CampaignSaveFiles.load(primary).orElseThrow().snapshot());
        } finally {
            RewardGrantService.resetForTests();
            CampaignProgressStore.resetForTests(playerId);
        }
    }

    @Test
    void staleJournalIsDroppedWhenCanonicalSaveAlreadyContainsTransaction() throws Exception {
        UUID playerId = UUID.randomUUID();
        Path primary = tempDir.resolve("stale.json");
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            CampaignProgressStore.markClean(playerId);
            RewardGrantService.commit(playerId, "tx-stale", "ENC_M01", P0Scenario.create(), BattleOutcome.ALLY_VICTORY, () -> { });
            CampaignProgressStore.Snapshot committed = CampaignProgressStore.snapshot(playerId);
            RewardTransactionJournal.prepare(primary, "tx-stale", committed);
            CampaignSaveFiles.save(primary, committed);

            CampaignProgressStore.removeRuntime(playerId);
            CampaignProgressStore.restore(playerId, CampaignSaveFiles.load(primary).orElseThrow().snapshot());
            assertEquals(RewardTransactionJournal.Recovery.STALE, RewardTransactionJournal.recover(primary, playerId));
            assertEquals(committed, CampaignProgressStore.snapshot(playerId));
            assertFalse(Files.exists(RewardTransactionJournal.journalPath(primary)));
        } finally {
            RewardGrantService.resetForTests();
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
