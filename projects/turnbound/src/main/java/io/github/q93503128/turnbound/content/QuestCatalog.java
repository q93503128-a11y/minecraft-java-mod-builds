package io.github.q93503128.turnbound.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Typed v0.4 quest definitions and canonical completion rewards. */
public final class QuestCatalog {
    public enum Kind { MAIN, CHARACTER, REGION, CHALLENGE }

    public record Quest(
            String id,
            String name,
            Kind kind,
            int chapter,
            String owner,
            String objectiveType,
            List<String> targetIds,
            int requiredCount,
            List<String> prerequisites,
            List<String> unlockFlags) {
        public Quest {
            targetIds = List.copyOf(targetIds);
            prerequisites = List.copyOf(prerequisites);
            unlockFlags = List.copyOf(unlockFlags);
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Blank quest id");
            if (requiredCount < 1) throw new IllegalArgumentException("Invalid quest requiredCount");
        }
    }

    public record Reward(int crystal, int gold, int xp, String rewardToken) {}

    private static final JsonObject RAW = V04Catalogs.questsRaw();
    private static final Map<String, Quest> QUESTS = loadQuests();

    private QuestCatalog() {}

    public static Quest quest(String id) {
        Quest quest = QUESTS.get(id);
        if (quest == null) throw new IllegalArgumentException("Unknown quest " + id);
        return quest;
    }

    public static boolean contains(String id) { return QUESTS.containsKey(id); }
    public static List<Quest> all() { return List.copyOf(QUESTS.values()); }
    public static List<Quest> kind(Kind kind) { return QUESTS.values().stream().filter(q -> q.kind() == kind).toList(); }

    public static Reward reward(Quest quest) {
        JsonObject raw = RAW.getAsJsonObject("rewards").getAsJsonObject(quest.kind().name().toLowerCase());
        int crystal = raw.has("crystal") ? raw.get("crystal").getAsInt() : 0;
        int gold = raw.has("gold") ? raw.get("gold").getAsInt() : 0;
        int xp = quest.kind() == Kind.MAIN ? 2_000 + 500 * quest.chapter() : 0;
        String token = switch (quest.kind()) {
            case REGION -> raw.has("chest") ? raw.get("chest").getAsString() : "";
            case CHALLENGE -> raw.has("cosmetic") ? raw.get("cosmetic").getAsString() : "";
            default -> "";
        };
        return new Reward(crystal, gold, xp, token);
    }

    public static boolean chapterComplete(int chapter, Set<String> completed) {
        List<Quest> quests = kind(Kind.MAIN).stream().filter(q -> q.chapter() == chapter).toList();
        return !quests.isEmpty() && quests.stream().allMatch(q -> completed.contains(q.id()));
    }

    public static String finalMainQuest(int chapter) {
        List<Quest> quests = kind(Kind.MAIN).stream().filter(q -> q.chapter() == chapter).toList();
        if (quests.isEmpty()) throw new IllegalArgumentException("Unknown chapter " + chapter);
        return quests.getLast().id();
    }

    private static Map<String, Quest> loadQuests() {
        Map<String, Quest> out = new LinkedHashMap<>();
        for (JsonElement element : RAW.getAsJsonArray("main")) {
            JsonObject o = element.getAsJsonObject();
            put(out, new Quest(
                    o.get("id").getAsString(),
                    o.get("name").getAsString(),
                    Kind.MAIN,
                    o.get("chapter").getAsInt(),
                    "",
                    string(o, "objectiveType", "MANUAL"),
                    strings(o, "targetIds"),
                    integer(o, "requiredCount", 1),
                    strings(o, "prerequisites"),
                    strings(o, "unlockFlags")));
        }
        for (JsonElement element : RAW.getAsJsonArray("character")) {
            JsonObject o = element.getAsJsonObject();
            put(out, new Quest(
                    o.get("id").getAsString(),
                    o.get("name").getAsString(),
                    Kind.CHARACTER,
                    0,
                    o.get("owner").getAsString(),
                    "CHARACTER_STORY",
                    List.of(),
                    1,
                    strings(o, "prerequisites"),
                    List.of()));
        }
        for (JsonElement element : RAW.getAsJsonArray("region")) {
            String id = element.getAsString();
            put(out, new Quest(id, id, Kind.REGION, 0, "", "MANUAL", List.of(), 1, List.of(), List.of()));
        }
        for (JsonElement element : RAW.getAsJsonArray("challenge")) {
            String id = element.getAsString();
            put(out, new Quest(id, id, Kind.CHALLENGE, 0, "", "MANUAL", List.of(), 1, List.of(), List.of()));
        }
        return Map.copyOf(out);
    }

    private static void put(Map<String, Quest> map, Quest quest) {
        if (map.put(quest.id(), quest) != null) throw new IllegalStateException("Duplicate quest " + quest.id());
    }

    private static List<String> strings(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return List.copyOf(out);
    }

    private static String string(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }
}
