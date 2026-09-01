package io.github.q93503128.turnbound.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Static v0.4 character menu/profile metadata sourced from bundled data. */
public final class CharacterMenuCatalog {
    public record Profile(
            String id, String role, String primaryRole, String difficulty, String weapon,
            String reason, String personality, boolean profileQuest, String awakening) {}

    private static final Map<String, Profile> PROFILES = load();

    private CharacterMenuCatalog() {}

    public static Profile profile(String id) {
        Profile profile = PROFILES.get(id);
        if (profile == null) throw new IllegalArgumentException("Unknown character menu profile " + id);
        return profile;
    }

    public static List<Profile> all() { return List.copyOf(PROFILES.values()); }

    private static Map<String, Profile> load() {
        String resource = "/data/turnbound/characters/menu_v04.json";
        try (InputStream stream = CharacterMenuCatalog.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND resource " + resource);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != 4) {
                throw new IllegalStateException("Invalid TURNBOUND character menu schema");
            }
            Map<String, Profile> out = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("characters")) {
                JsonObject row = element.getAsJsonObject();
                Profile profile = new Profile(
                        text(row,"id"), text(row,"role"), text(row,"primaryRole"), text(row,"difficulty"),
                        text(row,"weapon"), text(row,"reason"), text(row,"personality"),
                        row.has("profileQuest") && row.get("profileQuest").getAsBoolean(),
                        PlayerFacingTerminology.mechanics(text(row,"awakening")));
                if (!CanonicalData.contains(profile.id()) || out.put(profile.id(), profile) != null) {
                    throw new IllegalStateException("Invalid/duplicate character menu profile " + profile.id());
                }
            }
            if (out.size() != 12) throw new IllegalStateException("TURNBOUND v0.4 requires 12 character menu profiles");
            return Map.copyOf(out);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading " + resource, ex);
        }
    }

    private static String text(JsonObject row, String key) {
        return row.has(key) ? row.get(key).getAsString() : "";
    }
}
