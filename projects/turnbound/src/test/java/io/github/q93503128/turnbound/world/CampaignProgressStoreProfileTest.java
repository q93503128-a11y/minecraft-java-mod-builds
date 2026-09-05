package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void newCampaignUsesCanonicalStartingEconomyAndSmallStarterParty() {
        assertEquals(5_000, CampaignProgressStore.gold(playerId));
        assertEquals(0, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(Set.of("P01", "F03"), CampaignProgressStore.ownedCharacters(playerId));
        assertEquals(List.of("P01", "F03"), CampaignProgressStore.activeParty(playerId));
    }

    @Test
    void tutorialWinsRecruitBramAndElysiaIntoTheParty() {
        CampaignProgressStore.commit(playerId, "TUTORIAL_1", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P03"));
        assertEquals(List.of("P01", "F03", "P03"), CampaignProgressStore.activeParty(playerId));

        CampaignProgressStore.commit(playerId, "TUTORIAL_2", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P04"));
        assertEquals(List.of("P01", "F03", "P03", "P04"), CampaignProgressStore.activeParty(playerId));
    }

    @Test
    void b01FirstClearPackageIsOnceButCombatRewardsRepeat() {
        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.B01_GRAUL, BattleOutcome.ALLY_VICTORY);

        assertEquals(17_000, CampaignProgressStore.gold(playerId));
        assertEquals(3_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(60, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P08"));
        assertEquals(1, CampaignProgressStore.equipment(playerId).choiceTokens().getOrDefault("T2", 0));
        assertTrue(CampaignProgressStore.snapshot(playerId).profile().starterArchiveUnlocked());
        assertFalse(CampaignProgressStore.snapshot(playerId).profile().starterArchiveUsed());

        CampaignProgressStore.commit(playerId, SouthgateEncounterCatalog.B01_GRAUL, BattleOutcome.ALLY_VICTORY);
        assertEquals(29_000, CampaignProgressStore.gold(playerId));
        assertEquals(3_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(60, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertEquals(1, CampaignProgressStore.equipment(playerId).choiceTokens().getOrDefault("T2", 0));
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
