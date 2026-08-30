package io.github.q93503128.turnbound.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.q93503128.turnbound.progression.PlayerProfile;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Versioned JSON codec for one player's authoritative TURNBOUND campaign snapshot. */
public final class CampaignSaveCodec {
    public static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CampaignSaveCodec() {}

    public static String encode(CampaignProgressStore.Snapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.add("profile", encodeProfile(snapshot.profile()));

        JsonObject characters = new JsonObject();
        snapshot.characters().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject state = new JsonObject();
            state.addProperty("level", entry.getValue().level());
            state.addProperty("xp", entry.getValue().xp());
            characters.add(entry.getKey(), state);
        });
        root.add("characters", characters);

        JsonArray cleared = new JsonArray();
        snapshot.clearedEncounters().stream().sorted().forEach(cleared::add);
        root.add("clearedEncounters", cleared);
        return GSON.toJson(root);
    }

    public static CampaignProgressStore.Snapshot decode(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int schema = requiredInt(root, "schemaVersion");
        if (schema != SCHEMA_VERSION) throw new IllegalStateException("Unsupported TURNBOUND campaign save schema " + schema);

        PlayerProfile.Snapshot profile = decodeProfile(requiredObject(root, "profile"));
        Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        for (var entry : requiredObject(root, "characters").entrySet()) {
            JsonObject state = entry.getValue().getAsJsonObject();
            if (characters.put(entry.getKey(), new CharacterProgression.State(
                    requiredInt(state, "level"), requiredInt(state, "xp"))) != null) {
                throw new IllegalStateException("Duplicate character progression " + entry.getKey());
            }
        }

        Set<String> cleared = new LinkedHashSet<>();
        for (JsonElement element : requiredArray(root, "clearedEncounters")) cleared.add(element.getAsString());
        return new CampaignProgressStore.Snapshot(profile, characters, cleared);
    }

    private static JsonObject encodeProfile(PlayerProfile.Snapshot profile) {
        JsonObject out = new JsonObject();
        out.addProperty("gold", profile.gold());
        out.addProperty("summonCrystal", profile.summonCrystal());
        out.addProperty("starEssence", profile.starEssence());
        out.addProperty("awakeningCore", profile.awakeningCore());
        JsonArray owned = new JsonArray();
        profile.ownedCharacters().stream().sorted().forEach(owned::add);
        out.add("ownedCharacters", owned);
        out.addProperty("fiveStarPity", profile.fiveStarPity());
        out.addProperty("starterArchiveUnlocked", profile.starterArchiveUnlocked());
        out.addProperty("starterArchiveUsed", profile.starterArchiveUsed());
        return out;
    }

    private static PlayerProfile.Snapshot decodeProfile(JsonObject raw) {
        Set<String> owned = new LinkedHashSet<>();
        for (JsonElement element : requiredArray(raw, "ownedCharacters")) owned.add(element.getAsString());
        return new PlayerProfile.Snapshot(
                requiredLong(raw, "gold"),
                requiredLong(raw, "summonCrystal"),
                requiredLong(raw, "starEssence"),
                requiredLong(raw, "awakeningCore"),
                owned,
                requiredInt(raw, "fiveStarPity"),
                requiredBoolean(raw, "starterArchiveUnlocked"),
                requiredBoolean(raw, "starterArchiveUsed"));
    }

    private static JsonObject requiredObject(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonObject()) throw new IllegalStateException("Missing object " + key);
        return object.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) throw new IllegalStateException("Missing array " + key);
        return object.getAsJsonArray(key);
    }

    private static int requiredInt(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalStateException("Missing int " + key);
        return object.get(key).getAsInt();
    }

    private static long requiredLong(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalStateException("Missing long " + key);
        return object.get(key).getAsLong();
    }

    private static boolean requiredBoolean(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalStateException("Missing boolean " + key);
        return object.get(key).getAsBoolean();
    }
}
