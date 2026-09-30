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

/** Journal data for the Av'Sal expansion. */
final class AvsalQuestCatalog {
    private static final String RESOURCE = "/data/turnbound/quests/avsal_v1.json";

    enum Kind {
        MAIN("메인"), SIDE("지역");
        private final String label;
        Kind(String label) { this.label = label; }
        String label() { return label; }
    }

    record Quest(String id, Kind kind, String title, String regionLabel, String objective,
                 String activationFlag, String completionFlag, String encounterId) {}

    private static final List<Quest> ALL = load();
    private AvsalQuestCatalog() {}

    static List<Quest> all() { return ALL; }

    static boolean visible(Quest quest, Set<String> flags, Set<String> productionEncounterIds) {
        if (quest == null) return false;
        Set<String> safeFlags = flags == null ? Set.of() : flags;
        Set<String> production = productionEncounterIds == null ? Set.of() : productionEncounterIds;
        if (!quest.activationFlag().isBlank() && !safeFlags.contains(quest.activationFlag())) return false;
        return quest.encounterId().isBlank() || production.contains(quest.encounterId());
    }

    static boolean completed(Quest quest, Set<String> flags, Set<String> clearedEncounters) {
        if (quest == null) return false;
        Set<String> safeFlags = flags == null ? Set.of() : flags;
        Set<String> clears = clearedEncounters == null ? Set.of() : clearedEncounters;
        if (!quest.completionFlag().isBlank()) return safeFlags.contains(quest.completionFlag());
        return !quest.encounterId().isBlank() && clears.contains(quest.encounterId());
    }

    static List<String> validate() {
        List<String> errors = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Quest quest : ALL) {
            if (quest.id().isBlank() || !ids.add(quest.id())) errors.add("duplicate/blank Av'Sal quest " + quest.id());
            if (quest.title().isBlank() || quest.regionLabel().isBlank() || quest.objective().isBlank()) {
                errors.add("incomplete Av'Sal quest " + quest.id());
            }
            if (!quest.encounterId().isBlank() && !CampaignEncounterCatalog.contains(quest.encounterId())) {
                errors.add("unknown Av'Sal quest encounter " + quest.id() + " -> " + quest.encounterId());
            }
        }
        if (ALL.stream().noneMatch(q -> "MQ_AV01".equals(q.id()))) errors.add("missing MQ_AV01");
        if (ALL.stream().noneMatch(q -> "MQ_AV02".equals(q.id()))) errors.add("missing MQ_AV02");
        return List.copyOf(errors);
    }

    private static List<Quest> load() {
        try (InputStream stream = AvsalQuestCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND Av'Sal quest catalog " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1) throw new IllegalStateException("Unsupported Av'Sal quest schema");
            JsonArray quests = root.getAsJsonArray("quests");
            List<Quest> out = new ArrayList<>();
            for (JsonElement element : quests) {
                JsonObject raw = element.getAsJsonObject();
                out.add(new Quest(
                        required(raw, "id"), Kind.valueOf(required(raw, "kind")), required(raw, "title"),
                        required(raw, "regionLabel"), required(raw, "objective"), optional(raw, "activationFlag"),
                        optional(raw, "completionFlag"), optional(raw, "encounterId")));
            }
            return List.copyOf(out);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND Av'Sal quest catalog", ex);
        }
    }

    private static String required(JsonObject raw, String key) {
        String value = optional(raw, key);
        if (value.isBlank()) throw new IllegalStateException("Missing Av'Sal quest field " + key);
        return value;
    }

    private static String optional(JsonObject raw, String key) {
        return raw.has(key) && !raw.get(key).isJsonNull() ? raw.get(key).getAsString().trim() : "";
    }
}
