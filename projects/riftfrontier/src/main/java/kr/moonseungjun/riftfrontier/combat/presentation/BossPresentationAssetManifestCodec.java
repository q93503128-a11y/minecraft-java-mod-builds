package kr.moonseungjun.riftfrontier.combat.presentation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Strict authored JSON boundary for selected production boss-presentation assets.
 *
 * <p>The manifest deliberately records logical keys and physical resource ids without assuming a
 * loader-specific path layout. Physical resource probing remains a separate, kind-aware runtime/client
 * boundary so an authored manifest can never prove a resource exists by itself.</p>
 */
public final class BossPresentationAssetManifestCodec {
    public static final int SCHEMA_VERSION = 1;
    private static final Set<String> ROOT_FIELDS = Set.of("kind", "schema_version", "assets");
    private static final Set<String> ASSET_FIELDS = Set.of(
        "logical_key", "asset_kind", "resource_id", "source", "license_note"
    );

    public BossPresentationAssetManifest decode(String json) {
        return decode(JsonParser.parseString(json));
    }

    public BossPresentationAssetManifest decode(Reader reader) {
        return decode(JsonParser.parseReader(reader));
    }

    public BossPresentationAssetManifest decode(JsonElement element) {
        JsonObject object = requireObject(element, "boss presentation asset manifest");
        rejectUnknownFields(object, ROOT_FIELDS, "boss presentation asset manifest");

        String kind = requireString(object, "kind");
        if (!"boss_presentation_asset_manifest".equals(kind)) {
            throw new IllegalArgumentException("Expected kind 'boss_presentation_asset_manifest', got '" + kind + "'");
        }

        int schemaVersion = requireInt(object, "schema_version");
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                "Unsupported boss presentation asset manifest schema_version " + schemaVersion + "; expected " + SCHEMA_VERSION
            );
        }

        JsonArray array = requireArray(object, "assets");
        List<BossPresentationAssetManifest.Asset> assets = new ArrayList<>(array.size());
        for (JsonElement entry : array) {
            JsonObject asset = requireObject(entry, "boss presentation asset");
            rejectUnknownFields(asset, ASSET_FIELDS, "boss presentation asset");
            assets.add(new BossPresentationAssetManifest.Asset(
                ContentId.parse(requireString(asset, "logical_key")),
                parseKind(requireString(asset, "asset_kind")),
                ContentId.parse(requireString(asset, "resource_id")),
                requireString(asset, "source"),
                requireString(asset, "license_note")
            ));
        }
        return new BossPresentationAssetManifest(assets);
    }

    private static BossPresentationAssetManifest.Kind parseKind(String raw) {
        try {
            return BossPresentationAssetManifest.Kind.valueOf(raw);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unknown boss presentation asset_kind '" + raw + "'", error);
        }
    }

    private static void rejectUnknownFields(JsonObject object, Set<String> allowed, String label) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unknown field '" + key + "' in " + label);
            }
        }
    }

    private static String requireString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing or non-string field '" + key + "'");
        }
        String text = value.getAsString().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Field '" + key + "' must not be blank");
        return text;
    }

    private static int requireInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing or non-number field '" + key + "'");
        }
        try {
            return value.getAsInt();
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Field '" + key + "' must be an integer", error);
        }
    }

    private static JsonArray requireArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalArgumentException("Missing or non-array field '" + key + "'");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject requireObject(JsonElement element, String label) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(label + " must be a JSON object");
        }
        return element.getAsJsonObject();
    }
}
