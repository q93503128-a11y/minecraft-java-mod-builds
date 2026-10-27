package dev.moonseungjun.augmentationsurvival.augment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validated runtime snapshot of the locked 149-augment roster. */
public final class AugmentCatalog {
    public static final String SECOND_CHOICE_ID = AugmentationSurvival.MOD_ID + ":second_choice";
    public static final String REROLL_ID = AugmentationSurvival.MOD_ID + ":candidate_reroll";
    private static final String RESOURCE_PATH = "data/augmentation_survival/augmentation/augment_catalog.json";
    private static final int EXPECTED_TOTAL = 149;
    private static final Map<AugmentTier, Integer> CANONICAL_COUNTS = Map.of(
            AugmentTier.COMMON, 20,
            AugmentTier.UNCOMMON, 27,
            AugmentTier.RARE, 31,
            AugmentTier.EPIC, 41,
            AugmentTier.LEGENDARY, 20,
            AugmentTier.MYTHIC, 10
    );
    private static volatile Snapshot current;

    private AugmentCatalog() { }
    public static void initialize() { current = loadBundled(); AugmentationSurvival.LOGGER.info("Loaded canonical augment catalog: {} definitions", current.byId().size()); }
    public static Snapshot current() { Snapshot snapshot = current; if (snapshot == null) throw new IllegalStateException("Augment catalog is not initialized"); return snapshot; }
    public static AugmentDefinition require(String id) { AugmentDefinition definition = current().byId().get(id); if (definition == null) throw new IllegalArgumentException("Unknown augment id: " + id); return definition; }
    public static List<AugmentDefinition> definitions(AugmentTier tier) { return current().byTier().getOrDefault(tier, List.of()); }
    public static List<AugmentDefinition> allDefinitions() { return List.copyOf(current().byId().values()); }
    public static int indexOf(String id) { int index = 0; for (String candidateId : current().byId().keySet()) { if (candidateId.equals(id)) return index; index++; } return -1; }
    public static AugmentDefinition definitionAt(int index) { if (index < 0 || index >= current().byId().size()) throw new IllegalArgumentException("Augment catalog index out of range: " + index); int currentIndex = 0; for (AugmentDefinition definition : current().byId().values()) { if (currentIndex == index) return definition; currentIndex++; } throw new IllegalStateException("Augment catalog index resolution failed: " + index); }
    public static double powerScore(Map<String, Integer> levels) { double score = 0.0D; for (Map.Entry<String, Integer> entry : levels.entrySet()) { AugmentDefinition definition = current().byId().get(entry.getKey()); if (definition != null) score += definition.powerScore(entry.getValue()); } return score; }

    private static Snapshot loadBundled() {
        ClassLoader loader = AugmentCatalog.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) throw new IllegalStateException("Missing bundled augment catalog: " + RESOURCE_PATH);
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) { return parse(JsonParser.parseReader(reader).getAsJsonObject()); }
        } catch (IOException exception) { throw new IllegalStateException("Failed to read augment catalog", exception); }
    }

    private static Snapshot parse(JsonObject root) {
        int schemaVersion = requireInt(root, "schema_version");
        if (schemaVersion != 1) throw new IllegalStateException("Unsupported augment catalog schema: " + schemaVersion);
        JsonObject expectedCounts = root.getAsJsonObject("expected_counts");
        for (AugmentTier tier : AugmentTier.values()) {
            int declared = requireInt(expectedCounts, tier.id());
            int canonical = CANONICAL_COUNTS.get(tier);
            if (declared != canonical) throw new IllegalStateException("Augment catalog declared count mismatch for " + tier.id() + ": expected " + canonical + ", got " + declared);
        }
        Map<String, AugmentDefinition> byId = new LinkedHashMap<>();
        EnumMap<AugmentTier, List<AugmentDefinition>> mutableByTier = new EnumMap<>(AugmentTier.class);
        Set<String> canonicalNames = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("augments")) {
            JsonObject object = element.getAsJsonObject();
            String path = requireString(object, "id"); requireValidPath(path);
            String id = AugmentationSurvival.MOD_ID + ":" + path;
            String name = requireString(object, "canonical_name_ko");
            AugmentTier tier = AugmentTier.fromId(requireString(object, "tier"));
            int maxLevel = requireInt(object, "max_level");
            AugmentDefinition definition = new AugmentDefinition(id, name, tier, maxLevel);
            if (byId.putIfAbsent(id, definition) != null) throw new IllegalStateException("Duplicate augment id: " + id);
            if (!canonicalNames.add(name)) throw new IllegalStateException("Duplicate canonical augment name: " + name);
            mutableByTier.computeIfAbsent(tier, ignored -> new ArrayList<>()).add(definition);
        }
        if (byId.size() != EXPECTED_TOTAL) throw new IllegalStateException("Canonical augment roster must contain " + EXPECTED_TOTAL + " entries, got " + byId.size());
        EnumMap<AugmentTier, List<AugmentDefinition>> byTier = new EnumMap<>(AugmentTier.class);
        for (AugmentTier tier : AugmentTier.values()) {
            List<AugmentDefinition> definitions = mutableByTier.getOrDefault(tier, List.of());
            int expected = CANONICAL_COUNTS.get(tier);
            if (definitions.size() != expected) throw new IllegalStateException("Canonical augment count mismatch for " + tier.id() + ": expected " + expected + ", got " + definitions.size());
            byTier.put(tier, List.copyOf(definitions));
        }
        if (!byId.containsKey(SECOND_CHOICE_ID)) throw new IllegalStateException("Canonical second-choice augment is missing from catalog");
        if (!byId.containsKey(REROLL_ID)) throw new IllegalStateException("Canonical reroll augment is missing from catalog");
        return new Snapshot(schemaVersion, Collections.unmodifiableMap(byId), Collections.unmodifiableMap(byTier));
    }

    private static void requireValidPath(String path) { for (int index = 0; index < path.length(); index++) { char c = path.charAt(index); boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/'; if (!valid) throw new IllegalStateException("Invalid augment id path: " + path); } }
    private static int requireInt(JsonObject object, String key) { if (object == null || !object.has(key)) throw new IllegalStateException("Missing required augment catalog integer: " + key); return object.get(key).getAsInt(); }
    private static String requireString(JsonObject object, String key) { if (!object.has(key)) throw new IllegalStateException("Missing required augment catalog string: " + key); String value = object.get(key).getAsString(); if (value.isBlank()) throw new IllegalStateException("Blank augment catalog string: " + key); return value; }
    public record Snapshot(int schemaVersion, Map<String, AugmentDefinition> byId, Map<AugmentTier, List<AugmentDefinition>> byTier) { }
}
