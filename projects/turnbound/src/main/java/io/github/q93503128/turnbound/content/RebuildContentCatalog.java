package io.github.q93503128.turnbound.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Explicit game-content contract for the reset foundation. This is not a substitute for the
 * retired catalogs; it verifies that no unpublished characters, quests or encounters sneak into
 * the new production build before user co-design.
 */
public final class RebuildContentCatalog {
    private static final String MANIFEST = "/data/turnbound/rebuild/foundation_v1.json";
    private static final List<String> CONTENT = List.of(
            "characters", "enemies", "quests", "equipment", "encounters", "regions");

    private RebuildContentCatalog() {}

    public static void requireEmptyProductionCatalog() {
        try (var stream = RebuildContentCatalog.class.getResourceAsStream(MANIFEST)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND rebuild catalog");
            requireEmpty(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read TURNBOUND rebuild catalog", exception);
        }
    }

    /** Package-visible to assert rejection of malformed or prematurely authored content in unit tests. */
    static void requireEmpty(Reader source) {
        JsonObject root = JsonParser.parseReader(source).getAsJsonObject();
        if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != 1)
            throw new IllegalStateException("Unsupported TURNBOUND rebuild catalog schema");
        for (String key : CONTENT) {
            JsonElement entry = root.get(key);
            if (entry == null || !entry.isJsonArray())
                throw new IllegalStateException("Missing content array: " + key);
            if (!entry.getAsJsonArray().isEmpty())
                throw new IllegalStateException("Unapproved authored content: " + key);
        }
    }
}
