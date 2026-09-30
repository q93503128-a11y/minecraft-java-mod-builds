package io.github.q93503128.turnbound.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Player-facing quest content bound to the surveyed Drehmal route.
 *
 * <p>Place-based side objectives may be authored before a coordinate is promoted, but they are not visible in
 * normal gameplay until the referenced encounter has passed the 26.2 production gate.</p>
 */
public final class DrehmalQuestCatalog {
    public static final int SCHEMA_VERSION = 1;
    private static final String RESOURCE = "/data/turnbound/quests/drehmal_first_route_v1.json";
    private static final List<Quest> ALL = load();

    public enum Kind {
        MAIN("메인"),
        SIDE("서브 목표"),
        HIDDEN("숨은 의뢰");

        private final String label;
        Kind(String label) { this.label = label; }
        public String label() { return label; }
    }

    public record Quest(
            String id,
            Kind kind,
            String title,
            String regionLabel,
            String objective,
            String activationFlag,
            String completionFlag,
            String encounterId,
            boolean requiresProductionEncounter,
            int rewardCrystal,
            int rewardGold,
            int rewardXp
    ) {}

    private DrehmalQuestCatalog() {}

    public static List<Quest> all() {
        return ALL;
    }

    static boolean visible(Quest quest, Set<String> flags, Set<String> productionEncounterIds) {
        if (quest == null) return false;
        Set<String> safeFlags = flags == null ? Set.of() : flags;
        Set<String> production = productionEncounterIds == null ? Set.of() : productionEncounterIds;
        if (!quest.activationFlag().isBlank() && !safeFlags.contains(quest.activationFlag())) return false;
        return !quest.requiresProductionEncounter()
                || (!quest.encounterId().isBlank() && production.contains(quest.encounterId()));
    }

    static boolean completed(Quest quest, Set<String> flags, Set<String> clearedEncounters) {
        if (quest == null) return false;
        Set<String> safeFlags = flags == null ? Set.of() : flags;
        Set<String> clears = clearedEncounters == null ? Set.of() : clearedEncounters;
        if (!quest.completionFlag().isBlank()) return safeFlags.contains(quest.completionFlag());
        return !quest.encounterId().isBlank() && clears.contains(quest.encounterId());
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        int mainCount = 0;
        for (Quest quest : ALL) {
            if (!ids.add(quest.id())) errors.add("duplicate Drehmal quest " + quest.id());
            if (quest.kind() == Kind.MAIN) mainCount++;
            if (quest.title().isBlank()) errors.add("blank Drehmal quest title " + quest.id());
            if (quest.regionLabel().isBlank()) errors.add("blank Drehmal quest region " + quest.id());
            if (quest.objective().isBlank()) errors.add("blank Drehmal quest objective " + quest.id());
            if (!quest.encounterId().isBlank() && !CampaignEncounterCatalog.contains(quest.encounterId())) {
                errors.add("unknown Drehmal quest encounter " + quest.id() + " -> " + quest.encounterId());
            }
            if (quest.requiresProductionEncounter() && quest.encounterId().isBlank()) {
                errors.add("production-gated Drehmal quest has no encounter " + quest.id());
            }
            if (quest.completionFlag().isBlank() && quest.encounterId().isBlank()) {
                errors.add("Drehmal quest has no completion condition " + quest.id());
            }
            if (quest.rewardCrystal() <= 0 && quest.rewardGold() <= 0 && quest.rewardXp() <= 0) {
                errors.add("Drehmal quest has no completion reward " + quest.id());
            }
        }
        if (mainCount < 1) errors.add("Drehmal route must expose at least one main quest");
        return List.copyOf(errors);
    }

    private static List<Quest> load() {
        try (InputStream stream = DrehmalQuestCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND Drehmal quest catalog " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : -1;
            if (schema != SCHEMA_VERSION) throw new IllegalStateException("Unsupported Drehmal quest schema " + schema);
            JsonArray rawQuests = root.getAsJsonArray("quests");
            if (rawQuests == null) throw new IllegalStateException("Missing Drehmal quest list");

            List<Quest> out = new ArrayList<>();
            for (JsonElement element : rawQuests) {
                JsonObject raw = element.getAsJsonObject();
                out.add(new Quest(
                        required(raw, "id"),
                        Kind.valueOf(required(raw, "kind")),
                        required(raw, "title"),
                        required(raw, "regionLabel"),
                        required(raw, "objective"),
                        optional(raw, "activationFlag"),
                        optional(raw, "completionFlag"),
                        optional(raw, "encounterId"),
                        bool(raw, "requiresProductionEncounter", false),
                        integer(raw, "rewardCrystal", 0),
                        integer(raw, "rewardGold", 0),
                        integer(raw, "rewardXp", 0)));
            }
            return List.copyOf(out);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND Drehmal quest catalog", exception);
        }
    }

    private static String required(JsonObject raw, String key) {
        String value = optional(raw, key);
        if (value.isBlank()) throw new IllegalStateException("Missing Drehmal quest field " + key);
        return value;
    }

    private static String optional(JsonObject raw, String key) {
        if (raw == null || !raw.has(key) || !raw.get(key).isJsonPrimitive()) return "";
        return raw.get(key).getAsString().trim();
    }

    private static int integer(JsonObject raw, String key, int fallback) {
        return raw != null && raw.has(key) && raw.get(key).isJsonPrimitive()
                ? Math.max(0, raw.get(key).getAsInt())
                : fallback;
    }

    private static boolean bool(JsonObject raw, String key, boolean fallback) {
        return raw != null && raw.has(key) && raw.get(key).isJsonPrimitive()
                ? raw.get(key).getAsBoolean()
                : fallback;
    }
}
