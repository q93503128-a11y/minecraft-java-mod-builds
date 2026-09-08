package kr.moonseungjun.riftfrontier.combat.presentation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Strict JSON decoder for authored boss presentation profiles. */
public final class BossPresentationProfileCodec {
    public BossPresentationProfile decode(String json) {
        return decode(JsonParser.parseString(json));
    }

    public BossPresentationProfile decode(Reader reader) {
        return decode(JsonParser.parseReader(reader));
    }

    public BossPresentationProfile decode(JsonElement element) {
        JsonObject object = requireObject(element, "boss presentation profile");
        String kind = requireString(object, "kind");
        if (!"boss_presentation_profile".equals(kind)) {
            throw new IllegalArgumentException("Expected kind 'boss_presentation_profile', got '" + kind + "'");
        }

        ContentId id = ContentId.parse(requireString(object, "id"));
        ContentId bossProfile = ContentId.parse(requireString(object, "boss_profile"));
        String variant = requireString(object, "variant");
        ContentId modelKey = ContentId.parse(requireString(object, "model_key"));
        JsonArray array = requireArray(object, "bindings");
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();

        for (JsonElement entry : array) {
            JsonObject bindingObject = requireObject(entry, "presentation binding");
            var key = new BossPresentationProfile.BindingKey(
                requireString(bindingObject, "presentation_cue"),
                requireString(bindingObject, "delivery"),
                parsePhase(requireString(bindingObject, "phase"))
            );
            var value = new BossPresentationProfile.AssetBinding(
                ContentId.parse(requireString(bindingObject, "animation_key")),
                ContentId.parse(requireString(bindingObject, "vfx_key")),
                ContentId.parse(requireString(bindingObject, "sound_key"))
            );
            if (bindings.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate boss presentation selector: " + key.selector());
            }
        }

        return new BossPresentationProfile(id, bossProfile, variant, modelKey, bindings);
    }

    private static AttackTimeline.Phase parsePhase(String raw) {
        try {
            return AttackTimeline.Phase.valueOf(raw);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unknown presentation attack phase '" + raw + "'", error);
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

    private static JsonArray requireArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) throw new IllegalArgumentException("Missing or non-array field '" + key + "'");
        return value.getAsJsonArray();
    }

    private static JsonObject requireObject(JsonElement element, String label) {
        if (element == null || !element.isJsonObject()) throw new IllegalArgumentException(label + " must be a JSON object");
        return element.getAsJsonObject();
    }
}
