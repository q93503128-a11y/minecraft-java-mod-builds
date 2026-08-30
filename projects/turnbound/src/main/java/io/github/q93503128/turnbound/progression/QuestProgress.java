package io.github.q93503128.turnbound.progression;

import io.github.q93503128.turnbound.content.QuestCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persistent v0.4 quest journal and objective progress. */
public final class QuestProgress {
    public static final int TRACK_LIMIT = 3;

    public enum EventType { INTERACT, PARTY_CONFIRM, BATTLE_WIN, BOSS_WIN, INVENTORY_FLAG, KILL, LOOT }

    public record Event(EventType type, String primaryId, Set<String> ids, int amount) {
        public Event {
            if (type == null) throw new IllegalArgumentException("Missing quest event type");
            primaryId = primaryId == null ? "" : primaryId;
            ids = Set.copyOf(ids == null ? Set.of() : ids);
            if (amount < 1) throw new IllegalArgumentException("Quest event amount must be positive");
        }
        public static Event interact(String id) { return new Event(EventType.INTERACT, id, Set.of(id), 1); }
        public static Event partyConfirm(Set<String> ids) { return new Event(EventType.PARTY_CONFIRM, "", ids, 1); }
        public static Event battleWin(String encounterId, Set<String> enemyIds) { return new Event(EventType.BATTLE_WIN, encounterId, enemyIds, 1); }
        public static Event bossWin(String bossId) { return new Event(EventType.BOSS_WIN, bossId, Set.of(bossId), 1); }
        public static Event inventoryFlag(String id) { return new Event(EventType.INVENTORY_FLAG, id, Set.of(id), 1); }
        public static Event kill(String id, int amount) { return new Event(EventType.KILL, id, Set.of(id), amount); }
        public static Event loot(String id, int amount) { return new Event(EventType.LOOT, id, Set.of(id), amount); }
    }

    public record Snapshot(
            Set<String> completed,
            List<String> tracked,
            Set<String> unlockFlags,
            Map<String, Integer> rewardTokens,
            Map<String, Integer> counters,
            Map<String, Set<String>> marks) {
        public Snapshot {
            completed = Set.copyOf(completed);
            tracked = List.copyOf(tracked);
            unlockFlags = Set.copyOf(unlockFlags);
            rewardTokens = Map.copyOf(rewardTokens);
            counters = Map.copyOf(counters);
            Map<String, Set<String>> safeMarks = new LinkedHashMap<>();
            marks.forEach((id, values) -> safeMarks.put(id, Set.copyOf(values)));
            marks = Map.copyOf(safeMarks);
            if (tracked.size() > TRACK_LIMIT) throw new IllegalArgumentException("Too many tracked quests");
            if (new LinkedHashSet<>(tracked).size() != tracked.size()) throw new IllegalArgumentException("Duplicate tracked quest");
            rewardTokens.forEach((id, count) -> { if (count < 0) throw new IllegalArgumentException("Negative reward token"); });
            counters.forEach((id, count) -> { if (count < 0) throw new IllegalArgumentException("Negative quest counter"); });
        }
        public static Snapshot empty() { return new Snapshot(Set.of(), List.of(), Set.of(), Map.of(), Map.of(), Map.of()); }
    }

    private final Set<String> completed = new LinkedHashSet<>();
    private final List<String> tracked = new ArrayList<>();
    private final Set<String> unlockFlags = new LinkedHashSet<>();
    private final Map<String, Integer> rewardTokens = new LinkedHashMap<>();
    private final Map<String, Integer> counters = new LinkedHashMap<>();
    private final Map<String, Set<String>> marks = new LinkedHashMap<>();

    private QuestProgress() {}

    public static QuestProgress empty() { return new QuestProgress(); }

    public static QuestProgress restore(Snapshot snapshot) {
        QuestProgress state = new QuestProgress();
        state.completed.addAll(snapshot.completed());
        state.tracked.addAll(snapshot.tracked());
        state.unlockFlags.addAll(snapshot.unlockFlags());
        state.rewardTokens.putAll(snapshot.rewardTokens());
        state.counters.putAll(snapshot.counters());
        snapshot.marks().forEach((id, values) -> state.marks.put(id, new LinkedHashSet<>(values)));
        return state;
    }

    public Snapshot snapshot() { return new Snapshot(completed, tracked, unlockFlags, rewardTokens, counters, marks); }
    public boolean completed(String questId) { return completed.contains(questId); }
    public Set<String> completed() { return Set.copyOf(completed); }
    public boolean unlocked(String flag) { return unlockFlags.contains(flag); }
    public Set<String> unlockFlags() { return Set.copyOf(unlockFlags); }
    public int rewardTokens(String token) { return rewardTokens.getOrDefault(token, 0); }
    public List<String> tracked() { return List.copyOf(tracked); }
    public int counter(String key) { return counters.getOrDefault(key, 0); }
    public Set<String> marks(String questId) { return Set.copyOf(marks.getOrDefault(questId, Set.of())); }

    public void track(String questId) {
        QuestCatalog.quest(questId);
        if (completed.contains(questId) || tracked.contains(questId)) return;
        if (tracked.size() >= TRACK_LIMIT) throw new IllegalStateException("Quest track limit is " + TRACK_LIMIT);
        tracked.add(questId);
    }

    public void untrack(String questId) { tracked.remove(questId); }

    public boolean apply(QuestCatalog.Quest quest, Event event) {
        if (completed(quest.id())) return false;
        return switch (quest.objectiveType()) {
            case "INTERACT" -> markIfTarget(quest, event, EventType.INTERACT, event.primaryId());
            case "PARTY_CONFIRM" -> markParty(quest, event);
            case "BATTLE_WINS" -> markIfTarget(quest, event, EventType.BATTLE_WIN, event.primaryId());
            case "BATTLE_WIN" -> markFromBattleEnemies(quest, event);
            case "BOSS_WIN" -> markIfTarget(quest, event, EventType.BOSS_WIN, event.primaryId());
            case "INTERACT_COUNT" -> incrementIfTarget(quest, event, EventType.INTERACT, quest.id(), event.primaryId(), event.amount());
            case "BATTLE_WINS_WITH" -> incrementBattleWith(quest, event);
            case "KILL_AND_LOOT" -> incrementKillOrLoot(quest, event);
            case "INVENTORY_FLAGS" -> markIfTarget(quest, event, EventType.INVENTORY_FLAG, event.primaryId());
            case "BOSS_AND_INTERACT" -> markBossAndInteract(quest, event);
            default -> false;
        };
    }

    public boolean satisfied(QuestCatalog.Quest quest) {
        return switch (quest.objectiveType()) {
            case "INTERACT", "PARTY_CONFIRM", "BATTLE_WINS", "BATTLE_WIN", "BOSS_WIN", "INVENTORY_FLAGS", "BOSS_AND_INTERACT" ->
                    marks(quest.id()).containsAll(quest.targetIds());
            case "INTERACT_COUNT", "BATTLE_WINS_WITH" -> counter(quest.id()) >= quest.requiredCount();
            case "KILL_AND_LOOT" -> quest.targetIds().stream().allMatch(id -> counter(counterKey(quest.id(), id)) >= quest.requiredCount());
            default -> false;
        };
    }

    public void complete(QuestCatalog.Quest quest) {
        if (!completed.add(quest.id())) throw new IllegalStateException("Quest already completed " + quest.id());
        tracked.remove(quest.id());
        unlockFlags.addAll(quest.unlockFlags());
        if (quest.kind() == QuestCatalog.Kind.CHARACTER && !quest.owner().isBlank()) unlockFlags.add("PROFILE_" + quest.owner());
        counters.keySet().removeIf(key -> key.equals(quest.id()) || key.startsWith(quest.id() + "|"));
        marks.remove(quest.id());
    }

    public void grantRewardToken(String token, int amount) {
        if (token == null || token.isBlank() || amount <= 0) return;
        rewardTokens.merge(token, amount, Integer::sum);
    }

    private boolean markIfTarget(QuestCatalog.Quest quest, Event event, EventType type, String candidate) {
        if (event.type() != type || !quest.targetIds().contains(candidate)) return false;
        return marks.computeIfAbsent(quest.id(), ignored -> new LinkedHashSet<>()).add(candidate);
    }

    private boolean markParty(QuestCatalog.Quest quest, Event event) {
        if (event.type() != EventType.PARTY_CONFIRM) return false;
        boolean changed = false;
        Set<String> state = marks.computeIfAbsent(quest.id(), ignored -> new LinkedHashSet<>());
        for (String target : quest.targetIds()) if (event.ids().contains(target)) changed |= state.add(target);
        return changed;
    }

    private boolean markFromBattleEnemies(QuestCatalog.Quest quest, Event event) {
        if (event.type() != EventType.BATTLE_WIN) return false;
        boolean changed = false;
        Set<String> state = marks.computeIfAbsent(quest.id(), ignored -> new LinkedHashSet<>());
        for (String target : quest.targetIds()) if (event.ids().contains(target)) changed |= state.add(target);
        return changed;
    }

    private boolean incrementIfTarget(QuestCatalog.Quest quest, Event event, EventType type, String key, String candidate, int amount) {
        if (event.type() != type || !quest.targetIds().contains(candidate)) return false;
        counters.merge(key, amount, Integer::sum);
        return true;
    }

    private boolean incrementBattleWith(QuestCatalog.Quest quest, Event event) {
        if (event.type() != EventType.BATTLE_WIN || quest.targetIds().stream().noneMatch(event.ids()::contains)) return false;
        counters.merge(quest.id(), event.amount(), Integer::sum);
        return true;
    }

    private boolean incrementKillOrLoot(QuestCatalog.Quest quest, Event event) {
        if (event.type() != EventType.KILL && event.type() != EventType.LOOT) return false;
        if (!quest.targetIds().contains(event.primaryId())) return false;
        counters.merge(counterKey(quest.id(), event.primaryId()), event.amount(), Integer::sum);
        return true;
    }

    private boolean markBossAndInteract(QuestCatalog.Quest quest, Event event) {
        EventType type = event.type();
        if (type != EventType.BOSS_WIN && type != EventType.INTERACT) return false;
        return markIfTarget(quest, event, type, event.primaryId());
    }

    private static String counterKey(String questId, String targetId) { return questId + "|" + targetId; }
}
