package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.QuestCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.QuestProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Non-mutating mirror of the automatic MAIN quest events emitted by CampaignProgressStore.commit().
 * This exists only so the Result page can show the progression that will be settled on Continue.
 */
public final class QuestResultPreview {
    public record Completion(String id, String name, int crystal, int gold, int xp) {}
    public record Preview(List<Completion> completions, int crystal, int gold, int xp) {
        public Preview { completions = List.copyOf(completions == null ? List.of() : completions); }
        public static Preview none() { return new Preview(List.of(), 0, 0, 0); }
    }

    private QuestResultPreview() {}

    public static Preview automaticMainRewards(UUID playerId, String encounterId) {
        if (playerId == null || encounterId == null || encounterId.isBlank()) return Preview.none();
        String canonical = CampaignProgressStore.canonicalEncounterId(encounterId);
        if (!V04Catalogs.hasEncounter(canonical) || V04Catalogs.tutorialBridge(canonical)) return Preview.none();

        V04Catalogs.Encounter encounter = V04Catalogs.encounter(canonical);
        QuestProgress simulated = QuestProgress.restore(CampaignProgressStore.quests(playerId));
        ArrayList<Completion> completed = new ArrayList<>();

        applyEvent(simulated, QuestProgress.Event.battleWin(canonical, Set.copyOf(encounter.enemies())), completed);
        if (encounter.boss()) {
            applyEvent(simulated, QuestProgress.Event.bossWin(encounter.enemies().getFirst()), completed);
        }

        int crystal = completed.stream().mapToInt(Completion::crystal).sum();
        int gold = completed.stream().mapToInt(Completion::gold).sum();
        int xp = completed.stream().mapToInt(Completion::xp).sum();
        return new Preview(completed, crystal, gold, xp);
    }

    private static void applyEvent(QuestProgress simulated, QuestProgress.Event event, List<Completion> completed) {
        for (QuestCatalog.Quest quest : QuestCatalog.kind(QuestCatalog.Kind.MAIN)) {
            if (simulated.completed(quest.id()) || !prerequisitesMet(simulated, quest)) continue;
            simulated.apply(quest, event);
            if (!simulated.satisfied(quest)) continue;
            QuestCatalog.Reward reward = QuestCatalog.reward(quest);
            completed.add(new Completion(quest.id(), quest.name(), reward.crystal(), reward.gold(), reward.xp()));
            simulated.complete(quest);
        }
    }

    /** MAIN v0.4 prerequisites are quest-chain IDs; keep the preview intentionally scoped to that authored grammar. */
    private static boolean prerequisitesMet(QuestProgress simulated, QuestCatalog.Quest quest) {
        for (String prerequisite : quest.prerequisites()) {
            if (!prerequisite.startsWith("MQ_") || !simulated.completed(prerequisite)) return false;
        }
        return true;
    }
}
