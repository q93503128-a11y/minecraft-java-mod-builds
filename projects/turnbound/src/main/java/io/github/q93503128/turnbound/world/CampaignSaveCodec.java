package io.github.q93503128.turnbound.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Versioned JSON codec mirroring the canonical v0.4 player save schema. */
public final class CampaignSaveCodec {
    public static final int SCHEMA_VERSION = 4;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CampaignSaveCodec() {}

    public static String encode(CampaignProgressStore.Snapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.add("profile", encodeProfile(snapshot.profile()));
        root.add("characters", encodeCharacters(snapshot.characters()));
        root.add("growth", encodeGrowth(snapshot.growth()));
        root.add("equipment", encodeEquipment(snapshot.equipment()));
        root.add("quests", encodeQuests(snapshot.quests()));
        root.add("clearedEncounters", strings(snapshot.clearedEncounters()));
        root.add("orphanedCharacterIds", strings(snapshot.orphanedCharacterIds()));
        root.add("orphanedEquipmentIds", strings(snapshot.orphanedEquipmentIds()));
        return GSON.toJson(root);
    }

    public static CampaignProgressStore.Snapshot decode(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 1;
        if (schema != 1 && schema != SCHEMA_VERSION) throw new IllegalStateException("Unsupported TURNBOUND campaign save schema " + schema);

        Set<String> orphanedCharacters = new LinkedHashSet<>();
        Set<String> orphanedEquipment = new LinkedHashSet<>();
        ProfileDecode profile = decodeProfile(requiredObject(root, "profile"));
        orphanedCharacters.addAll(profile.orphanedCharacters());

        Map<String, CharacterProgression.State> characters = decodeCharacters(optionalObject(root, "characters"), orphanedCharacters);
        Map<String, CharacterGrowthRules.State> growth = schema >= 4
                ? decodeGrowth(optionalObject(root, "growth"), orphanedCharacters) : new LinkedHashMap<>();
        for (String characterId : profile.snapshot().ownedCharacters()) {
            characters.putIfAbsent(characterId, new CharacterProgression.State(1, 0));
            growth.putIfAbsent(characterId, CharacterGrowthRules.initial(characterId));
        }

        EquipmentInventory.Snapshot equipment = schema >= 4 && root.has("equipment")
                ? decodeEquipment(root.getAsJsonObject("equipment"), orphanedEquipment) : EquipmentInventory.Snapshot.empty();
        QuestProgress.Snapshot quests = schema >= 4 && root.has("quests")
                ? decodeQuests(root.getAsJsonObject("quests")) : QuestProgress.Snapshot.empty();

        Set<String> cleared = stringSet(optionalArray(root, "clearedEncounters"));
        orphanedCharacters.addAll(stringSet(optionalArray(root, "orphanedCharacterIds")));
        orphanedEquipment.addAll(stringSet(optionalArray(root, "orphanedEquipmentIds")));
        return new CampaignProgressStore.Snapshot(profile.snapshot(), characters, growth, equipment, quests,
                cleared, orphanedCharacters, orphanedEquipment);
    }

    private static JsonObject encodeProfile(PlayerProfile.Snapshot profile) {
        JsonObject out = new JsonObject();
        out.addProperty("gold", profile.gold());
        out.addProperty("summonCrystal", profile.summonCrystal());
        out.addProperty("starEssence", profile.starEssence());
        out.addProperty("awakeningCore", profile.awakeningCore());
        out.add("ownedCharacters", strings(profile.ownedCharacters()));
        out.addProperty("fiveStarPity", profile.fiveStarPity());
        out.addProperty("starterArchiveUnlocked", profile.starterArchiveUnlocked());
        out.addProperty("starterArchiveUsed", profile.starterArchiveUsed());
        return out;
    }

    private static ProfileDecode decodeProfile(JsonObject raw) {
        Set<String> known = new LinkedHashSet<>();
        Set<String> orphaned = new LinkedHashSet<>();
        for (JsonElement element : optionalArray(raw, "ownedCharacters")) {
            String id = element.getAsString();
            if (GachaCatalog.isSummonable(id)) known.add(id); else orphaned.add(id);
        }
        return new ProfileDecode(new PlayerProfile.Snapshot(
                optionalLong(raw, "gold", 5_000), optionalLong(raw, "summonCrystal", 0), optionalLong(raw, "starEssence", 0),
                optionalLong(raw, "awakeningCore", 0), known, optionalInt(raw, "fiveStarPity", 0),
                optionalBoolean(raw, "starterArchiveUnlocked", false), optionalBoolean(raw, "starterArchiveUsed", false)), orphaned);
    }

    private static JsonObject encodeCharacters(Map<String, CharacterProgression.State> characters) {
        JsonObject out = new JsonObject();
        characters.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject state = new JsonObject();
            state.addProperty("level", entry.getValue().level());
            state.addProperty("xp", entry.getValue().xp());
            out.add(entry.getKey(), state);
        });
        return out;
    }

    private static Map<String, CharacterProgression.State> decodeCharacters(JsonObject raw, Set<String> orphaned) {
        Map<String, CharacterProgression.State> out = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) {
            if (!GachaCatalog.isSummonable(entry.getKey())) { orphaned.add(entry.getKey()); continue; }
            JsonObject state = entry.getValue().getAsJsonObject();
            out.put(entry.getKey(), new CharacterProgression.State(optionalInt(state, "level", 1), optionalInt(state, "xp", 0)));
        }
        return out;
    }

    private static JsonObject encodeGrowth(Map<String, CharacterGrowthRules.State> growth) {
        JsonObject out = new JsonObject();
        growth.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject state = new JsonObject();
            state.addProperty("currentStar", entry.getValue().currentStar());
            state.addProperty("awakened", entry.getValue().awakened());
            state.addProperty("characterQuestComplete", entry.getValue().characterQuestComplete());
            state.addProperty("signatureTrialCleared", entry.getValue().signatureTrialCleared());
            out.add(entry.getKey(), state);
        });
        return out;
    }

    private static Map<String, CharacterGrowthRules.State> decodeGrowth(JsonObject raw, Set<String> orphaned) {
        Map<String, CharacterGrowthRules.State> out = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) {
            String id = entry.getKey();
            if (!GachaCatalog.isSummonable(id)) { orphaned.add(id); continue; }
            JsonObject state = entry.getValue().getAsJsonObject();
            out.put(id, new CharacterGrowthRules.State(optionalInt(state, "currentStar", GachaCatalog.nativeStars(id)),
                    optionalBoolean(state, "awakened", false), optionalBoolean(state, "characterQuestComplete", false),
                    optionalBoolean(state, "signatureTrialCleared", false)));
        }
        return out;
    }

    private static JsonObject encodeEquipment(EquipmentInventory.Snapshot snapshot) {
        JsonObject out = new JsonObject();
        out.addProperty("nextSerial", snapshot.nextSerial());
        JsonArray items = new JsonArray();
        snapshot.items().values().stream().sorted(java.util.Comparator.comparing(EquipmentInventory.Item::instanceId)).forEach(item -> {
            JsonObject row = new JsonObject();
            row.addProperty("instanceId", item.instanceId());
            row.addProperty("itemId", item.itemId());
            row.addProperty("enhancementLevel", item.enhancementLevel());
            items.add(row);
        });
        out.add("items", items);
        JsonObject loadouts = new JsonObject();
        snapshot.loadouts().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            EquipmentInventory.Loadout value = entry.getValue();
            JsonObject row = new JsonObject();
            row.addProperty("weapon", value.weapon());
            row.addProperty("armor", value.armor());
            row.addProperty("accessory", value.accessory());
            row.addProperty("signature", value.signature());
            loadouts.add(entry.getKey(), row);
        });
        out.add("loadouts", loadouts);
        JsonObject choices = new JsonObject();
        snapshot.choiceTokens().forEach(choices::addProperty);
        out.add("choiceTokens", choices);
        return out;
    }

    private static EquipmentInventory.Snapshot decodeEquipment(JsonObject raw, Set<String> orphaned) {
        long nextSerial = optionalLong(raw, "nextSerial", 1);
        Map<String, EquipmentInventory.Item> items = new LinkedHashMap<>();
        for (JsonElement element : optionalArray(raw, "items")) {
            JsonObject row = element.getAsJsonObject();
            String itemId = requiredString(row, "itemId");
            if (!knownEquipment(itemId)) { orphaned.add(itemId); continue; }
            EquipmentInventory.Item item = new EquipmentInventory.Item(requiredString(row, "instanceId"), itemId, optionalInt(row, "enhancementLevel", 0));
            items.put(item.instanceId(), item);
        }
        Map<String, EquipmentInventory.Loadout> loadouts = new LinkedHashMap<>();
        for (var entry : optionalObject(raw, "loadouts").entrySet()) {
            JsonObject row = entry.getValue().getAsJsonObject();
            loadouts.put(entry.getKey(), new EquipmentInventory.Loadout(
                    validInstance(optionalString(row, "weapon", ""), items),
                    validInstance(optionalString(row, "armor", ""), items),
                    validInstance(optionalString(row, "accessory", ""), items),
                    validInstance(optionalString(row, "signature", ""), items)));
        }
        Map<String, Integer> choices = new LinkedHashMap<>();
        for (var entry : optionalObject(raw, "choiceTokens").entrySet()) choices.put(entry.getKey(), entry.getValue().getAsInt());
        return new EquipmentInventory.Snapshot(nextSerial, items, loadouts, choices);
    }

    private static JsonObject encodeQuests(QuestProgress.Snapshot snapshot) {
        JsonObject out = new JsonObject();
        out.add("completed", strings(snapshot.completed()));
        out.add("tracked", strings(snapshot.tracked()));
        out.add("unlockFlags", strings(snapshot.unlockFlags()));
        JsonObject tokens = new JsonObject();
        snapshot.rewardTokens().forEach(tokens::addProperty);
        out.add("rewardTokens", tokens);
        JsonObject counters = new JsonObject();
        snapshot.counters().forEach(counters::addProperty);
        out.add("counters", counters);
        JsonObject marks = new JsonObject();
        snapshot.marks().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> marks.add(entry.getKey(), strings(entry.getValue())));
        out.add("marks", marks);
        return out;
    }

    private static QuestProgress.Snapshot decodeQuests(JsonObject raw) {
        Set<String> completed = stringSet(optionalArray(raw, "completed"));
        List<String> tracked = new ArrayList<>();
        for (JsonElement e : optionalArray(raw, "tracked")) tracked.add(e.getAsString());
        Set<String> unlockFlags = stringSet(optionalArray(raw, "unlockFlags"));
        Map<String, Integer> tokens = intMap(optionalObject(raw, "rewardTokens"));
        Map<String, Integer> counters = intMap(optionalObject(raw, "counters"));
        Map<String, Set<String>> marks = new LinkedHashMap<>();
        for (var entry : optionalObject(raw, "marks").entrySet()) {
            if (entry.getValue().isJsonArray()) marks.put(entry.getKey(), stringSet(entry.getValue().getAsJsonArray()));
        }
        return new QuestProgress.Snapshot(completed, tracked, unlockFlags, tokens, counters, marks);
    }

    private static Map<String, Integer> intMap(JsonObject raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) out.put(entry.getKey(), entry.getValue().getAsInt());
        return out;
    }

    private static boolean knownEquipment(String itemId) {
        try { V04Catalogs.equipment(itemId); return true; } catch (RuntimeException ignored) { }
        try { V04Catalogs.signature(itemId); return true; } catch (RuntimeException ignored) { return false; }
    }

    private static String validInstance(String value, Map<String, EquipmentInventory.Item> items) {
        return value.isBlank() || !items.containsKey(value) ? "" : value;
    }

    private static JsonArray strings(Iterable<String> values) { JsonArray out = new JsonArray(); for (String value : values) out.add(value); return out; }
    private static Set<String> stringSet(JsonArray values) { Set<String> out = new LinkedHashSet<>(); for (JsonElement e : values) out.add(e.getAsString()); return out; }
    private static JsonObject requiredObject(JsonObject object, String key) { if (!object.has(key) || !object.get(key).isJsonObject()) throw new IllegalStateException("Missing object " + key); return object.getAsJsonObject(key); }
    private static JsonObject optionalObject(JsonObject object, String key) { return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : new JsonObject(); }
    private static JsonArray optionalArray(JsonObject object, String key) { return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray(); }
    private static int optionalInt(JsonObject object, String key, int fallback) { return object.has(key) ? object.get(key).getAsInt() : fallback; }
    private static long optionalLong(JsonObject object, String key, long fallback) { return object.has(key) ? object.get(key).getAsLong() : fallback; }
    private static boolean optionalBoolean(JsonObject object, String key, boolean fallback) { return object.has(key) ? object.get(key).getAsBoolean() : fallback; }
    private static String optionalString(JsonObject object, String key, String fallback) { return object.has(key) ? object.get(key).getAsString() : fallback; }
    private static String requiredString(JsonObject object, String key) { if (!object.has(key)) throw new IllegalStateException("Missing string " + key); return object.get(key).getAsString(); }
    private record ProfileDecode(PlayerProfile.Snapshot snapshot, Set<String> orphanedCharacters) {}
}
