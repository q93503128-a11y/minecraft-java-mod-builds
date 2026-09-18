package io.github.q93503128.turnbound.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-only description of the production authored world.
 *
 * <p>This file owns TURNBOUND semantic locators and integration seeds only. It deliberately contains no copied
 * Drehmal blocks, structures, datapack commands, textures or resource-pack assets.</p>
 */
public final class DrehmalWorldProfile {
    public static final int SCHEMA_VERSION = 1;
    public static final String PROFILE_ID = "turnbound:drehmal_apotheosis_2_2_2f";
    public static final String HUB_LOCATOR = "turnbound:hub/new_drabyel";
    public static final String FIRST_REGION_LOCATOR = "turnbound:region/stasis_facility";
    private static final String RESOURCE = "/data/turnbound/world/drehmal_v2_2_2f.json";
    private static final Profile PROFILE = load();

    public record Anchor(
            String kind,
            String locator,
            int x,
            int y,
            int z,
            boolean enabled,
            String sourceNote
    ) {}

    public record Profile(
            String id,
            String sourceVersion,
            String dimension,
            List<Anchor> anchors
    ) {
        public Profile {
            anchors = List.copyOf(anchors);
        }
    }

    private DrehmalWorldProfile() {}

    public static Profile profile() { return PROFILE; }

    public static Anchor enabled(String locator) {
        if (locator == null) return null;
        for (Anchor anchor : PROFILE.anchors()) {
            if (anchor.enabled() && locator.equals(anchor.locator())) return anchor;
        }
        return null;
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!PROFILE_ID.equals(PROFILE.id())) errors.add("unexpected profile id " + PROFILE.id());
        if (!"minecraft:overworld".equals(PROFILE.dimension())) {
            errors.add("unsupported external dimension " + PROFILE.dimension());
        }
        Anchor hub = enabled(HUB_LOCATOR);
        Anchor region = enabled(FIRST_REGION_LOCATOR);
        if (hub == null || !"HUB".equals(hub.kind())) errors.add("missing enabled New Drabyel hub anchor");
        if (region == null || !"REGION".equals(region.kind())) errors.add("missing enabled first-region anchor");
        if (hub != null && region != null && hub.x() == region.x() && hub.y() == region.y() && hub.z() == region.z()) {
            errors.add("hub and first region anchors must differ");
        }

        Map<String, Anchor> locators = new LinkedHashMap<>();
        for (Anchor anchor : PROFILE.anchors()) {
            if (anchor.locator() == null || anchor.locator().isBlank()) {
                errors.add("blank external-world locator");
                continue;
            }
            if (locators.put(anchor.locator(), anchor) != null) errors.add("duplicate locator " + anchor.locator());
        }
        return List.copyOf(errors);
    }

    private static Profile load() {
        try (InputStream stream = DrehmalWorldProfile.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND external-world profile " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : -1;
            if (schema != SCHEMA_VERSION) {
                throw new IllegalStateException("Unsupported external-world profile schema " + schema);
            }

            List<Anchor> anchors = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("anchors")) {
                JsonObject raw = element.getAsJsonObject();
                anchors.add(new Anchor(
                        string(raw, "kind"),
                        string(raw, "locator"),
                        raw.get("x").getAsInt(),
                        raw.get("y").getAsInt(),
                        raw.get("z").getAsInt(),
                        !raw.has("enabled") || raw.get("enabled").getAsBoolean(),
                        raw.has("sourceNote") ? raw.get("sourceNote").getAsString() : ""));
            }
            Profile profile = new Profile(
                    string(root, "profileId"),
                    string(root, "sourceVersion"),
                    string(root, "dimension"),
                    anchors);
            if (!PROFILE_ID.equals(profile.id())) {
                throw new IllegalStateException("External-world profile id mismatch: " + profile.id());
            }
            return profile;
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND external-world profile", exception);
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing external-world profile field " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) throw new IllegalStateException("Blank external-world profile field " + key);
        return value;
    }
}
