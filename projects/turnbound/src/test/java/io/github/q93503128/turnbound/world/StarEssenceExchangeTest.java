package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StarEssenceExchangeTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        CampaignProgressStore.resetForTests(playerId);
    }

    @Test
    void crystalExchangeUsesCanonicalPermanentRate() {
        restore(150, Map.of("P01", new CharacterProgression.State(1, 0)));

        var result = CampaignProgressStore.exchangeEssenceForCrystal(playerId);

        assertEquals(150, result.essenceSpent());
        assertEquals(300, result.crystalGranted());
        assertEquals(0, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertEquals(300, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.SUMMON_CRYSTAL));
    }

    @Test
    void ownedFourStarSelectorCreatesDuplicateRewardAndPlusLevel() {
        restore(1_000, Map.of("P01", new CharacterProgression.State(60, 0, 9)));

        var first = CampaignProgressStore.exchangeEssenceCharacter(playerId, 4, "P01");
        assertEquals(450, first.essenceSpent());
        assertEquals(0, first.essenceRefunded());
        assertEquals(1, first.bonusLevelGranted());
        assertEquals(10, first.bonusLevelAfter());
        assertEquals(550, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
        assertEquals(10, CampaignProgressStore.character(playerId, "P01").bonusLevel());

        assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.exchangeEssenceCharacter(playerId, 4, "P01"));
        assertEquals(550, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
    }

    @Test
    void selectorCannotRevealAnUnownedCharacter() {
        restore(2_000, Map.of("P01", new CharacterProgression.State(1, 0)));

        assertThrows(IllegalStateException.class,
                () -> CampaignProgressStore.exchangeEssenceCharacter(playerId, 4, "P07"));
        assertEquals(2_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.STAR_ESSENCE));
    }

    private void restore(long essence, Map<String, CharacterProgression.State> characters) {
        Set<String> owned = characters.keySet();
        Map<String, CharacterGrowthRules.State> growth = owned.stream()
                .collect(java.util.stream.Collectors.toMap(id -> id, CharacterGrowthRules::initial));
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(5_000, 0, essence, 0, owned, 0, false, false),
                characters, growth, EquipmentInventory.Snapshot.empty(), QuestProgress.Snapshot.empty(),
                Set.of(), Set.of(), Set.of()));
    }
}
