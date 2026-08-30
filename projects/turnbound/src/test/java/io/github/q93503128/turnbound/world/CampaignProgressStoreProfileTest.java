package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignProgressStoreProfileTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        CampaignProgressStore.resetForTests(playerId);
    }

    @Test
    void newCampaignUsesCanonicalStartingEconomyAndStoryParty() {
        assertEquals(5_000, CampaignProgressStore.gold(playerId));
        assertEquals(0, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(Set.of("P01", "P03", "P04", "F03"), CampaignProgressStore.ownedCharacters(playerId));
    }

    @Test
    void b01FirstClearAppliesArchiveUnlockPackageExactlyOnce() {
        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.B01_GRAUL, BattleOutcome.ALLY_VICTORY);

        assertEquals(17_000, CampaignProgressStore.gold(playerId));
        assertEquals(3_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(60, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P08"));
        assertTrue(CampaignProgressStore.snapshot(playerId).profile().starterArchiveUnlocked());
        assertFalse(CampaignProgressStore.snapshot(playerId).profile().starterArchiveUsed());

        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.B01_GRAUL, BattleOutcome.ALLY_VICTORY);
        assertEquals(17_000, CampaignProgressStore.gold(playerId));
        assertEquals(3_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(60, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
    }

    @Test
    void campaignSnapshotRoundTripsEconomyXpAndClearFlags() {
        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.ENC_M01, BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);

        CampaignProgressStore.resetForTests(playerId);
        CampaignProgressStore.restore(playerId, snapshot);

        assertEquals(snapshot, CampaignProgressStore.snapshot(playerId));
        assertFalse(CampaignProgressStore.previewVictory(playerId, SouthgateEncounterCatalog.ENC_M01).firstClear());
    }
}
