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

/** Data-driven repeatable regional contracts used as an optional progression fallback between authored quests. */
public final class RegionalContractCatalog {
    public static final int SCHEMA_VERSION = 1;
    private static final String RESOURCE = "/data/turnbound/quests/regional_contracts_v1.json";
    private static final List<Contract> ALL = load();

    public record Contract(
            String id,
            String title,
            String regionLabel,
            String objective,
            List<String> encounterIds,
            int requiredWins,
            int rewardGold,
            int rewardXp
    ) {
        public Contract {
            encounterIds = List.copyOf(encounterIds == null ? List.of() : encounterIds);
        }
    }

    private RegionalContractCatalog() {}

    public static List<Contract> all() {
        return ALL;
    }

    public static Contract contract(String id) {
        if (id == null || id.isBlank()) return null;
        for (Contract contract : ALL) if (id.equals(contract.id())) return contract;
        return null;
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Contract contract : ALL) {
            if (!ids.add(contract.id())) errors.add("duplicate regional contract " + contract.id());
            if (contract.title().isBlank()) errors.add("blank regional contract title " + contract.id());
            if (contract.regionLabel().isBlank()) errors.add("blank regional contract region " + contract.id());
            if (contract.objective().isBlank()) errors.add("blank regional contract objective " + contract.id());
            if (contract.encounterIds().isEmpty()) errors.add("regional contract has no encounters " + contract.id());
            for (String encounterId : contract.encounterIds()) {
                if (!CampaignEncounterCatalog.contains(encounterId)) {
                    errors.add("regional contract references unknown encounter " + contract.id() + " -> " + encounterId);
                }
            }
            if (contract.requiredWins() < 1) errors.add("regional contract requiredWins < 1 " + contract.id());
            if (contract.rewardGold() <= 0 && contract.rewardXp() <= 0) {
                errors.add("regional contract has no progression reward " + contract.id());
            }
        }
        if (ALL.size() < 3) errors.add("regional contract pool should contain at least 3 contracts");
        return List.copyOf(errors);
    }

    private static List<Contract> load() {
        try (InputStream stream = RegionalContractCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing regional contract catalog " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : -1;
            if (schema != SCHEMA_VERSION) throw new IllegalStateException("Unsupported regional contract schema " + schema);
            JsonArray raw = root.getAsJsonArray("contracts");
            if (raw == null) throw new IllegalStateException("Missing regional contract list");

            List<Contract> out = new ArrayList<>();
            for (JsonElement element : raw) {
                JsonObject value = element.getAsJsonObject();
                List<String> encounters = new ArrayList<>();
                JsonArray rawEncounters = value.getAsJsonArray("encounterIds");
                if (rawEncounters != null) for (JsonElement encounter : rawEncounters) encounters.add(encounter.getAsString());
                out.add(new Contract(
                        required(value, "id"),
                        required(value, "title"),
                        required(value, "regionLabel"),
                        required(value, "objective"),
                        encounters,
                        integer(value, "requiredWins", 0),
                        integer(value, "rewardGold", 0),
                        integer(value, "rewardXp", 0)));
            }
            return List.copyOf(out);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading regional contract catalog", exception);
        }
    }

    private static String required(JsonObject raw, String key) {
        if (raw == null || !raw.has(key) || !raw.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing regional contract field " + key);
        }
        String value = raw.get(key).getAsString().trim();
        if (value.isBlank()) throw new IllegalStateException("Blank regional contract field " + key);
        return value;
    }

    private static int integer(JsonObject raw, String key, int fallback) {
        return raw != null && raw.has(key) && raw.get(key).isJsonPrimitive()
                ? Math.max(0, raw.get(key).getAsInt()) : fallback;
    }
}
