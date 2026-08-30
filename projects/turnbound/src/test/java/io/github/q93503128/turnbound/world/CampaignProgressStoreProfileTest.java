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
    void cleanup() { CampaignProgressStore.resetForTests(playerId); }

    @Test
    void newCampaignUsesCanonicalStartingEconomyStoryPartyAndNativeStars() {
        assertEquals(5_000, CampaignProgressStore.gold(playerId));
        assertEquals(0, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(Set.of("P01", "P03", "P04", "F03"), CampaignProgressStore.ownedCharacters(playerId));
        assertEquals(4, CampaignProgressStore.growth(playerId, "P01").currentStar());
        assertEquals(2, CampaignProgressStore.growth(playerId, "F03").currentStar());
    }

    @Test
    void b01FirstClearAppliesArchiveAndEquipmentUnlockPackageExactlyOnce() {
        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.B01_GRAUL, BattleOutcome.ALLY_VICTORY);

        assertEquals(17_000, CampaignProgressStore.gold(playerId));
        assertEquals(3_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(60, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P08"));
        assertEquals(3, CampaignProgressStore.growth(playerId, "P08").currentStar());
        assertEquals(1, CampaignProgressStore.equipment(playerId).choiceTokens().get("T2"));
        assertTrue(CampaignProgressStore.snapshot(playerId).profile().starterArchiveUnlocked());
        assertFalse(CampaignProgressStore.snapshot(playerId).profile().starterArchiveUsed());

        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.B01_GRAUL, BattleOutcome.ALLY_VICTORY);
        assertEquals(17_000, CampaignProgressStore.gold(playerId));
        assertEquals(3_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(60, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertEquals(1, CampaignProgressStore.equipment(playerId).choiceTokens().get("T2"));
    }

    @Test
    void campaignSnapshotRoundTripsEconomyXpGrowthEquipmentAndClearFlags() {
        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.ENC_M01, BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        CampaignProgressStore.resetForTests(playerId);
        CampaignProgressStore.restore(playerId, snapshot);
        assertEquals(snapshot, CampaignProgressStore.snapshot(playerId));
        assertFalse(CampaignProgressStore.previewVictory(playerId, SouthgateEncounterCatalog.ENC_M01).firstClear());
    }
}
