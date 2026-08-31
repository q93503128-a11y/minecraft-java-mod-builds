package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.progression.QuestProgress;
import io.github.q93503128.turnbound.session.BattleResultSummary;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Path;
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

    public static final class SettlementException extends IllegalStateException {
        private final boolean recoverableFromJournal;

        private SettlementException(String message, boolean recoverableFromJournal, Throwable cause) {
            super(message, cause);
            this.recoverableFromJournal = recoverableFromJournal;
        }

        public boolean recoverableFromJournal() { return recoverableFromJournal; }
    }

    @FunctionalInterface
    interface JournalStep {
        void prepare(CampaignProgressStore.Snapshot snapshot) throws IOException;
    }

    @FunctionalInterface
    interface SaveStep {
        void save() throws IOException;
    }

    @FunctionalInterface
    interface CleanupStep {
        void cleanup() throws IOException;
    }

    private RewardGrantService() {}

    public static Result commitAndSave(ServerPlayer player, String transactionId, String encounterId,
                                       BattleState state, BattleOutcome outcome) {
        if (player == null) throw new IllegalArgumentException("Missing player");
        Path primary = CampaignPersistence.playerFile(player);
        return commit(player.getUUID(), transactionId, encounterId, state, outcome,
                snapshot -> RewardTransactionJournal.prepare(primary, transactionId, snapshot),
                () -> CampaignPersistence.saveOrThrow(player),
                () -> RewardTransactionJournal.clear(primary));
    }

    static Result commit(UUID playerId, String transactionId, String encounterId,
                         BattleState state, BattleOutcome outcome, SaveStep saveStep) {
        return commit(playerId, transactionId, encounterId, state, outcome, ignored -> { }, saveStep, () -> { });
    }

    static Result commit(UUID playerId, String transactionId, String encounterId,
                         BattleState state, BattleOutcome outcome, JournalStep journalStep,
                         SaveStep saveStep, CleanupStep cleanupStep) {
        if (outcome != BattleOutcome.ALLY_VICTORY || encounterId == null || encounterId.isBlank()) {
            return new Result(BattleResultSummary.none(), List.of(), false);
        }
        if (playerId == null) throw new IllegalArgumentException("Missing player id");
        if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("Missing reward transaction id");
        if (state == null) throw new IllegalArgumentException("Missing battle state");
        if (journalStep == null || saveStep == null || cleanupStep == null) throw new IllegalArgumentException("Missing reward persistence step");
        if (!ACTIVE_PLAYERS.add(playerId)) {
            throw new IllegalStateException("Reward settlement re-entry for " + playerId);
        }

        CampaignProgressStore.Snapshot before = CampaignProgressStore.snapshot(playerId);
        boolean wasDirty = CampaignProgressStore.isDirty(playerId);
        boolean journalPrepared = false;
        Result result;
        try {
            if (transactionCommitted(before, transactionId)) {
                return new Result(BattleResultSummary.none(), List.of(), true);
            }

            BattleResultSummary reward = EndgameEncounterCatalog.contains(encounterId)
                    ? EndgameProgressService.commit(playerId, encounterId, outcome)
                    : CampaignProgressStore.commit(playerId, encounterId, outcome);
            if (!EndgameEncounterCatalog.contains(encounterId)) {
                CampaignSupplementalRewardService.apply(playerId, encounterId, reward);
            }
            List<String> challenges = ChallengeService.evaluateAndCommit(playerId, encounterId, state, outcome);
            markCommitted(playerId, transactionId);
            CampaignProgressStore.Snapshot after = CampaignProgressStore.snapshot(playerId);
            journalStep.prepare(after);
            journalPrepared = true;
            saveStep.save();
            result = new Result(reward, challenges, false);
        } catch (IOException ex) {
            rollback(playerId, before, wasDirty);
            throw new SettlementException("Failed to persist reward transaction " + transactionId, journalPrepared, ex);
        } catch (RuntimeException ex) {
            rollback(playerId, before, wasDirty);
            throw ex;
        } finally {
            ACTIVE_PLAYERS.remove(playerId);
        }

        try {
            cleanupStep.cleanup();
        } catch (IOException cleanupFailure) {
            Turnbound.LOGGER.warn("TURNBOUND left a stale reward journal after committed transaction {}", transactionId, cleanupFailure);
        }
        return result;
    }

    static boolean transactionCommitted(CampaignProgressStore.Snapshot snapshot, String transactionId) {
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
