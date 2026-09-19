package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalSummonUnlockRewardTest {
    @Test
    void firstDrabyelRoadClearFundsAndUnlocksOneStarterTenPull() {
        UUID playerId = UUID.randomUUID();
        CampaignProgressStore.resetForTests(playerId);
        try {
            CampaignProgressStore.ensureNewGame(playerId);

            var preview = CampaignProgressStore.previewVictory(playerId, DrehmalContentUnlocks.DRABYEL_ROAD);
            assertTrue(preview.firstClear());
            assertEquals(GachaCatalog.TEN_COST, preview.crystal());

            var committed = CampaignProgressStore.commit(
                    playerId, DrehmalContentUnlocks.DRABYEL_ROAD, BattleOutcome.ALLY_VICTORY);
            assertEquals(GachaCatalog.TEN_COST, committed.crystal());
            assertEquals(GachaCatalog.TEN_COST,
                    CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
            assertTrue(CampaignProgressStore.starterArchiveAvailable(playerId));

            var repeat = CampaignProgressStore.previewVictory(playerId, DrehmalContentUnlocks.DRABYEL_ROAD);
            assertFalse(repeat.firstClear());
            assertEquals(0, repeat.crystal());
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
