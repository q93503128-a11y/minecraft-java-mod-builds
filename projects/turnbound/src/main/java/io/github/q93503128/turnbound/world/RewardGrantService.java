package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.progression.QuestProgress;
import io.github.q93503128.turnbound.session.BattleResultSummary;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Atomic authority for settling a completed battle into campaign progression.
 * Rewards, challenge grants and the durable transaction marker either all save together or the runtime rolls back.
 */
public final class RewardGrantService {
    private static final String TX_MARK_KEY = "__turnbound_reward_tx";
    private static final Set<UUID> ACTIVE_PLAYERS = new LinkedHashSet<>();

    public record Result(BattleResultSummary rewardSummary, List<String> challengeIds, boolean duplicate) {
        public Result {
            rewardSummary = rewardSummary == null ? BattleResultSummary.none() : rewardSummary;
            challengeIds = List.copyOf(challengeIds == null ? List.of() : challengeIds);
        }
    }

    @FunctionalInterface
    interface SaveStep {
        void save() throws IOException;
    }

    private RewardGrantService() {}

    public static Result commitAndSave(ServerPlayer player, String transactionId, String encounterId,
                                       BattleState state, BattleOutcome outcome) {
        if (player == null) throw new IllegalArgumentException("Missing player");
        return commit(player.getUUID(), transactionId, encounterId, state, outcome,
                () -> CampaignPersistence.saveOrThrow(player));
    }

    static Result commit(UUID playerId, String transactionId, String encounterId,
                         BattleState state, BattleOutcome outcome, SaveStep saveStep) {
        if (outcome != BattleOutcome.ALLY_VICTORY || encounterId == null || encounterId.isBlank()) {
            return new Result(BattleResultSummary.none(), List.of(), false);
        }
        if (playerId == null) throw new IllegalArgumentException("Missing player id");
        if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("Missing reward transaction id");
        if (state == null) throw new IllegalArgumentException("Missing battle state");
        if (saveStep == null) throw new IllegalArgumentException("Missing reward save step");
        if (!ACTIVE_PLAYERS.add(playerId)) {
            throw new IllegalStateException("Reward settlement re-entry for " + playerId);
        }

        CampaignProgressStore.Snapshot before = CampaignProgressStore.snapshot(playerId);
        boolean wasDirty = CampaignProgressStore.isDirty(playerId);
        try {
            if (committed(before, transactionId)) {
                return new Result(BattleResultSummary.none(), List.of(), true);
            }

            BattleResultSummary reward = EndgameEncounterCatalog.contains(encounterId)
                    ? EndgameProgressService.commit(playerId, encounterId, outcome)
                    : CampaignProgressStore.commit(playerId, encounterId, outcome);
            List<String> challenges = ChallengeService.evaluateAndCommit(playerId, encounterId, state, outcome);
            markCommitted(playerId, transactionId);
            saveStep.save();
            return new Result(reward, challenges, false);
        } catch (IOException ex) {
            rollback(playerId, before, wasDirty);
            throw new IllegalStateException("Failed to persist reward transaction " + transactionId, ex);
        } catch (RuntimeException ex) {
            rollback(playerId, before, wasDirty);
            throw ex;
        } finally {
            ACTIVE_PLAYERS.remove(playerId);
        }
    }

    private static boolean committed(CampaignProgressStore.Snapshot snapshot, String transactionId) {
        return snapshot.quests().marks().getOrDefault(TX_MARK_KEY, Set.of()).contains(transactionId);
    }

    private static void markCommitted(UUID playerId, String transactionId) {
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        QuestProgress.Snapshot oldQuest = snapshot.quests();
        Map<String, Set<String>> marks = new LinkedHashMap<>();
        oldQuest.marks().forEach((key, values) -> marks.put(key, new LinkedHashSet<>(values)));
        marks.put(TX_MARK_KEY, new LinkedHashSet<>(Set.of(transactionId)));
        QuestProgress.Snapshot quests = new QuestProgress.Snapshot(
                oldQuest.completed(), oldQuest.tracked(), oldQuest.unlockFlags(), oldQuest.rewardTokens(), oldQuest.counters(), marks);
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                snapshot.profile(), snapshot.characters(), snapshot.growth(), snapshot.equipment(), quests,
                snapshot.activeParty(), snapshot.clearedEncounters(), snapshot.orphanedCharacterIds(), snapshot.orphanedEquipmentIds()));
        CampaignProgressStore.markDirty(playerId);
    }

    private static void rollback(UUID playerId, CampaignProgressStore.Snapshot before, boolean wasDirty) {
        CampaignProgressStore.restore(playerId, before);
        if (wasDirty) CampaignProgressStore.markDirty(playerId);
    }

    static void resetForTests() {
        ACTIVE_PLAYERS.clear();
    }
}
