package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class DuplicateRewardChoiceTest {
    private static UUID prepare(Set<String> owned, Map<String, Integer> pending, int crystal) {
        UUID playerId = UUID.randomUUID();
        Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        Map<String, CharacterGrowthRules.State> growth = new LinkedHashMap<>();
        for (String id : owned) {
            characters.put(id, new CharacterProgression.State(1, 0));
            growth.put(id, CharacterGrowthRules.initial(id));
        }
        PlayerProfile.Snapshot profile = new PlayerProfile.Snapshot(
                5_000, crystal, 0, 0, owned, 0, false, false,
                List.of(), List.of(List.of(), List.of(), List.of()), pending);
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                profile, characters, growth, EquipmentInventory.Snapshot.empty(),
                QuestProgress.Snapshot.empty(), Set.of(), Set.of(), Set.of()));
        return playerId;
    }

    @Test
    void levelOrEssenceIsExclusiveAndDuplicatePacketsCannotDoubleClaim() {
        UUID id = prepare(Set.of("P01"), Map.of("P01", 2), 0);
        CampaignProgressStore.chooseDuplicateReward(id, "P01", CampaignProgressStore.DuplicateRewardChoice.LEVEL);
        assertEquals(1, CampaignProgressStore.character(id, "P01").bonusLevel());
        assertEquals(0, CampaignProgressStore.currency(id, PlayerProfile.Currency.STAR_ESSENCE));
        assertEquals(1, CampaignProgressStore.snapshot(id).profile().pendingDuplicates().get("P01").intValue());

        CampaignProgressStore.chooseDuplicateReward(id, "P01", CampaignProgressStore.DuplicateRewardChoice.ESSENCE);
        assertEquals(1, CampaignProgressStore.character(id, "P01").bonusLevel());
        assertEquals(100, CampaignProgressStore.currency(id, PlayerProfile.Currency.STAR_ESSENCE));
        assertTrue(CampaignProgressStore.snapshot(id).profile().pendingDuplicates().isEmpty());
        assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.chooseDuplicateReward(id, "P01", CampaignProgressStore.DuplicateRewardChoice.ESSENCE));
    }

    @Test
    void cappedLevelRejectsLevelChoiceAndPreservesEssenceOption() {
        UUID id = prepare(Set.of("P01"), Map.of("P01", 1), 0);
        var before = CampaignProgressStore.snapshot(id);
        Map<String, CharacterProgression.State> characters = new LinkedHashMap<>(before.characters());
        characters.put("P01", new CharacterProgression.State(60, 0, 10));
        CampaignProgressStore.restore(id, new CampaignProgressStore.Snapshot(
                before.profile(), characters, before.growth(), before.equipment(), before.quests(),
                before.activeParty(), before.clearedEncounters(), before.orphanedCharacterIds(), before.orphanedEquipmentIds()));

        assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.chooseDuplicateReward(id, "P01", CampaignProgressStore.DuplicateRewardChoice.LEVEL));
        assertEquals(1, CampaignProgressStore.snapshot(id).profile().pendingDuplicates().get("P01").intValue());
        CampaignProgressStore.chooseDuplicateReward(id, "P01", CampaignProgressStore.DuplicateRewardChoice.ESSENCE);
        assertEquals(10, CampaignProgressStore.character(id, "P01").bonusLevel());
        assertEquals(100, CampaignProgressStore.currency(id, PlayerProfile.Currency.STAR_ESSENCE));
    }

    @Test
    void knownDuplicatePullQueuesChoiceInsteadOfAutomaticallyGrantingStats() {
        Set<String> owned = Set.of("P01","P02","P03","P04","P05","P06","P07","P08","F01","F02","F03","F04");
        UUID id = prepare(owned, Map.of(), 300);
        GachaService.BatchResult batch = CampaignProgressStore.summonStandard(id, 1);
        GachaService.PullResult result = batch.pulls().getFirst();
        assertFalse(result.newlyOwned());
        assertEquals(0, result.starEssenceGranted());
        assertEquals(0, result.bonusLevelGranted());
        assertEquals(1, CampaignProgressStore.snapshot(id).profile().pendingDuplicates().get(result.characterId()).intValue());
        assertEquals(0, CampaignProgressStore.currency(id, PlayerProfile.Currency.SUMMON_CRYSTAL));
    }
}
