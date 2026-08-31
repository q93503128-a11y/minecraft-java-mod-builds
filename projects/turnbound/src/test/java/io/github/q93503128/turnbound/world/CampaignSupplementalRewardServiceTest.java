package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleResultSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignSupplementalRewardServiceTest {
    private final Set<UUID> players = new LinkedHashSet<>();

    @AfterEach
    void cleanup() { for (UUID id : players) CampaignProgressStore.removeRuntime(id); }

    @Test
    void bossFirstClearExtrasMatchCanonicalTableWithoutDoubleGrantingB01() {
        UUID b01 = player();
        BattleResultSummary r01 = CampaignProgressStore.commit(b01, "BATTLE_B01", BattleOutcome.ALLY_VICTORY);
        assertTrue(r01.firstClear());
        long b01Crystal = CampaignProgressStore.currency(b01, PlayerProfile.Currency.SUMMON_CRYSTAL);
        int b01T2 = CampaignProgressStore.equipment(b01).choiceTokens().getOrDefault("T2", 0);
        CampaignSupplementalRewardService.apply(b01, "BATTLE_B01", r01);
        assertEquals(3_000, b01Crystal);
        assertEquals(b01Crystal, CampaignProgressStore.currency(b01, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(1, b01T2);
        assertEquals(b01T2, CampaignProgressStore.equipment(b01).choiceTokens().getOrDefault("T2", 0));

        assertBossExtra("BATTLE_B02", "", 1_200);
        assertBossExtra("BATTLE_B03", "T3", 1_200);
        assertBossExtra("BATTLE_B04", "T3", 1_200);
        assertBossExtra("BATTLE_B05", "T4", 1_200);
    }

    @Test
    void repeatClearDoesNotRepeatCrystalOrChoiceToken() {
        UUID id = player();
        BattleResultSummary first = CampaignProgressStore.commit(id, "BATTLE_B03", BattleOutcome.ALLY_VICTORY);
        CampaignSupplementalRewardService.apply(id, "BATTLE_B03", first);
        long crystal = CampaignProgressStore.currency(id, PlayerProfile.Currency.SUMMON_CRYSTAL);
        int tokens = CampaignProgressStore.equipment(id).choiceTokens().getOrDefault("T3", 0);

        BattleResultSummary repeat = CampaignProgressStore.commit(id, "BATTLE_B03", BattleOutcome.ALLY_VICTORY);
        assertFalse(repeat.firstClear());
        CampaignSupplementalRewardService.apply(id, "BATTLE_B03", repeat);
        assertEquals(crystal, CampaignProgressStore.currency(id, PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertEquals(tokens, CampaignProgressStore.equipment(id).choiceTokens().getOrDefault("T3", 0));
    }

    private void assertBossExtra(String encounter, String tier, long crystalExpected) {
        UUID id = player();
        BattleResultSummary result = CampaignProgressStore.commit(id, encounter, BattleOutcome.ALLY_VICTORY);
        assertTrue(result.firstClear());
        CampaignSupplementalRewardService.apply(id, encounter, result);
        assertEquals(crystalExpected, CampaignProgressStore.currency(id, PlayerProfile.Currency.SUMMON_CRYSTAL));
        if (!tier.isBlank()) assertEquals(1, CampaignProgressStore.equipment(id).choiceTokens().getOrDefault(tier, 0));
        else assertTrue(CampaignProgressStore.equipment(id).choiceTokens().isEmpty());
    }

    private UUID player() {
        UUID id = UUID.randomUUID();
        players.add(id);
        CampaignProgressStore.ensureNewGame(id);
        return id;
    }
}
