package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.progression.QuestProgress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignContentUnlocksTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void archiveAndForgeCannotBeUsedBeforeB01AndUnlockTogetherAfterChapterOne() {
        CampaignProgressStore.ensureNewGame(playerId);
        assertFalse(CampaignContentUnlocks.archive(playerId));
        assertFalse(CampaignContentUnlocks.forge(playerId));
        assertFalse(MetaActionGate.denial(playerId, "SUMMON1").isBlank());
        assertFalse(MetaActionGate.denial(playerId, "ENHANCE|eq_1").isBlank());

        CampaignProgressStore.commit(playerId, "BATTLE_B01", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignContentUnlocks.archive(playerId));
        assertTrue(CampaignContentUnlocks.forge(playerId));
        assertEquals("", MetaActionGate.denial(playerId, "SUMMON10"));
        assertEquals("", MetaActionGate.denial(playerId, "ENHANCE|eq_1"));
    }

    @Test
    void accessoryRequiresB02AndEndgameRequiresReconnectFlagRatherThanBareB05Clear() {
        CampaignProgressStore.ensureNewGame(playerId);
        CampaignProgressStore.commit(playerId, "BATTLE_B02", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignContentUnlocks.accessory(playerId));

        CampaignProgressStore.commit(playerId, "BATTLE_B05", BattleOutcome.ALLY_VICTORY);
        assertFalse(CampaignContentUnlocks.endgame(playerId));

        var snapshot = CampaignProgressStore.snapshot(playerId);
        var q = snapshot.quests();
        var flags = new LinkedHashSet<>(q.unlockFlags());
        flags.add("ENDGAME");
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                snapshot.profile(), snapshot.characters(), snapshot.growth(), snapshot.equipment(),
                new QuestProgress.Snapshot(q.completed(), q.tracked(), flags, q.rewardTokens(), q.counters(), q.marks()),
                snapshot.activeParty(), snapshot.clearedEncounters(), snapshot.orphanedCharacterIds(), snapshot.orphanedEquipmentIds()));
        assertTrue(CampaignContentUnlocks.endgame(playerId));
        assertTrue(CampaignContentUnlocks.signatureActual(playerId));
    }
}
