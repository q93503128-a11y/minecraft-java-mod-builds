package dev.moonseungjun.augmentationsurvival.augment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Explicit player-facing augment text. Design canon is never auto-summarized into cards. */
public final class AugmentPresentationCatalog {
    private static final String RESOURCE = "data/augmentation_survival/augmentation/augment_presentations.json";
    private static final int SCHEMA_VERSION = 1;
    private static final String[] FORBIDDEN_PLAYER_TERMS = {
            "root", "derived", "compat", "whitelist", "uuid", "namespace", "debug",
            "server authority", "game tick", "blocks/tick", "재귀", "내부 판정"
    };
    private static volatile Map<String, Entry> entries;

    private AugmentPresentationCatalog() { }

    public static void initialize() {
        Map<String, Entry> loaded = load();
        if (loaded.size() != AugmentCatalog.allDefinitions().size()) {
            throw new IllegalStateException("Player-facing augment text count mismatch: expected="
                    + AugmentCatalog.allDefinitions().size() + ", actual=" + loaded.size());
        }

        for (AugmentDefinition definition : AugmentCatalog.allDefinitions()) {
            String path = path(definition.id());
            Entry entry = loaded.get(path);
            if (entry == null) {
                throw new IllegalStateException("Missing explicit player-facing augment text for " + definition.id());
            }

            String expectedName = displayName(definition.canonicalNameKo());
            if (!entry.displayName().equals(expectedName)) {
                throw new IllegalStateException("Player-facing augment name drift for " + definition.id()
                        + ": expected=" + expectedName + ", actual=" + entry.displayName());
            }

            Kind expectedKind = expectedKind(definition, path);
            if (entry.kind() != expectedKind) {
                throw new IllegalStateException("Player-facing augment kind drift for " + definition.id()
                        + ": expected=" + expectedKind + ", actual=" + entry.kind());
            }

            validateText(definition.id(), entry.summary(), "summary");
            validateText(definition.id(), entry.detail(), "detail");
        }
        entries = Map.copyOf(loaded);
    }

    public static Entry get(AugmentDefinition definition) {
        Map<String, Entry> current = entries;
        if (current == null) {
            initialize();
            current = entries;
        }
        Entry entry = current.get(path(definition.id()));
        if (entry == null) {
            throw new IllegalStateException("Missing explicit player-facing augment text for " + definition.id());
        }
        return entry;
    }

    private static Map<String, Entry> load() {
        ClassLoader loader = AugmentPresentationCatalog.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled player-facing augment text: " + RESOURCE);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schemaVersion = requiredInt(root, "schema_version");
                if (schemaVersion != SCHEMA_VERSION) {
                    throw new IllegalStateException("Unsupported augment presentation schema: " + schemaVersion);
                }
                JsonObject rawEntries = requiredObject(root, "entries");
                LinkedHashMap<String, Entry> result = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> raw : rawEntries.entrySet()) {
                    String id = raw.getKey();
                    JsonObject object = raw.getValue().getAsJsonObject();
                    Entry entry = new Entry(
                            requiredString(object, "display_name"),
                            Kind.parse(requiredString(object, "kind")),
                            requiredString(object, "summary"),
                            requiredString(object, "detail")
                    );
                    if (result.put(id, entry) != null) {
                        throw new IllegalStateException("Duplicate player-facing augment text id: " + id);
                    }
                }
                return result;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load player-facing augment text", exception);
        }
    }

    private static void validateText(String augmentId, String text, String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Blank player-facing " + field + " for " + augmentId);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_PLAYER_TERMS) {
            if (lower.contains(forbidden)) {
                throw new IllegalStateException("Player-facing " + field + " leaked internal term '"
                        + forbidden + "' for " + augmentId + ": " + text);
            }
        }
    }

    private static Kind expectedKind(AugmentDefinition definition, String path) {
        if (path.equals("instant_mining")) return Kind.TOGGLE;
        String canonical = definition.canonicalNameKo();
        if (canonical.contains("(액티브 · 토글)")) return Kind.TOGGLE;
        if (canonical.contains("(액티브)")) return Kind.ACTIVE;
        return Kind.PASSIVE;
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static String displayName(String canonical) {
        return canonical.replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
    }

    private static JsonObject requiredObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalStateException("Missing JSON object '" + key + "' in " + RESOURCE);
        }
        return element.getAsJsonObject();
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Missing string '" + key + "' in " + RESOURCE);
        }
        String value = element.getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank string '" + key + "' in " + RESOURCE);
        }
        return value;
    }

    private static int requiredInt(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Missing integer '" + key + "' in " + RESOURCE);
        }
        return element.getAsInt();
    }

    public enum Kind {
        PASSIVE("패시브"),
        ACTIVE("액티브"),
        TOGGLE("토글");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        private static Kind parse(String raw) {
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "passive" -> PASSIVE;
                case "active" -> ACTIVE;
                case "toggle" -> TOGGLE;
                default -> throw new IllegalStateException("Unknown player-facing augment kind: " + raw);
            };
        }
    }

    public record Entry(String displayName, Kind kind, String summary, String detail) { }
}
