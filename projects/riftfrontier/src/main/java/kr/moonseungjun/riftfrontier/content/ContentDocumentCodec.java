package kr.moonseungjun.riftfrontier.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strict decoder for Riftfrontier data-driven content definitions.
 * JSON is converted into typed definitions before it becomes visible to runtime gameplay code.
 */
public final class ContentDocumentCodec {
    public CoreDefinition decode(String json) {
        return decode(JsonParser.parseString(json));
    }

    public CoreDefinition decode(Reader reader) {
        return decode(JsonParser.parseReader(reader));
    }

    public CoreDefinition decode(JsonElement element) {
        JsonObject object = requireObject(element, "content definition");
        String kind = requireString(object, "kind");
        ContentId id = ContentId.parse(requireString(object, "id"));

        return switch (kind) {
            case "combat_archetype" -> new CoreDefinition.CombatArchetype(id, stringSet(object, "behaviours"));
            case "region" -> new CoreDefinition.Region(
                id,
                requireString(object, "gameplay_rule"),
                idSet(object, "archetypes"),
                optionalIdSet(object, "resources"),
                optionalIdSet(object, "contracts")
            );
            case "loot_profile" -> new CoreDefinition.LootProfile(id, stringList(object, "pools"));
            case "creature" -> new CoreDefinition.Creature(
                id,
                requireId(object, "region"),
                requireId(object, "archetype"),
                requireId(object, "loot_profile"),
                stringSet(object, "behaviours")
            );
            case "encounter" -> new CoreDefinition.Encounter(
                id,
                requireId(object, "region"),
                idList(object, "participants"),
                requireString(object, "objective"),
                requireString(object, "world_consequence")
            );
            case "expedition_resource" -> new CoreDefinition.ExpeditionResource(
                id,
                requireId(object, "region"),
                requireString(object, "category"),
                requireInt(object, "carry_weight"),
                requireInt(object, "field_value")
            );
            case "contract" -> new CoreDefinition.Contract(
                id,
                requireId(object, "region"),
                requireString(object, "objective"),
                idIntMap(object, "required_resources"),
                requireId(object, "reward_loot_profile"),
                requireId(object, "extraction_result"),
                requireString(object, "world_consequence")
            );
            case "extraction_result" -> new CoreDefinition.ExtractionResultProfile(
                id,
                requireString(object, "outcome"),
                requireInt(object, "retained_percent"),
                requireInt(object, "threat_delta"),
                requireString(object, "world_consequence")
            );
            default -> throw new IllegalArgumentException("Unknown content kind '" + kind + "' for " + id);
        };
    }

    private static ContentId requireId(JsonObject object, String key) {
        return ContentId.parse(requireString(object, key));
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
            throw new IllegalArgumentException("Missing or non-numeric field '" + key + "'");
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Field '" + key + "' must be an integer", error);
        }
    }

    private static List<String> stringList(JsonObject object, String key) {
        JsonArray array = requireArray(object, key);
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Field '" + key + "' must contain strings only");
            }
            String value = element.getAsString().trim();
            if (value.isEmpty()) throw new IllegalArgumentException("Field '" + key + "' contains a blank value");
            values.add(value);
        }
        return List.copyOf(values);
    }

    private static Set<String> stringSet(JsonObject object, String key) {
        List<String> list = stringList(object, key);
        Set<String> values = new LinkedHashSet<>(list);
        if (values.size() != list.size()) throw new IllegalArgumentException("Field '" + key + "' contains duplicate values");
        return Set.copyOf(values);
    }

    private static List<ContentId> idList(JsonObject object, String key) {
        return stringList(object, key).stream().map(ContentId::parse).toList();
    }

    private static Set<ContentId> idSet(JsonObject object, String key) {
        List<ContentId> list = idList(object, key);
        Set<ContentId> values = new LinkedHashSet<>(list);
        if (values.size() != list.size()) throw new IllegalArgumentException("Field '" + key + "' contains duplicate content ids");
        return Set.copyOf(values);
    }

    private static Set<ContentId> optionalIdSet(JsonObject object, String key) {
        return object.has(key) ? idSet(object, key) : Set.of();
    }

    private static Map<ContentId, Integer> idIntMap(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("Missing or non-object field '" + key + "'");
        Map<ContentId, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            ContentId id = ContentId.parse(entry.getKey());
            JsonElement amount = entry.getValue();
            if (!amount.isJsonPrimitive() || !amount.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("Field '" + key + "' amount for " + id + " must be numeric");
            }
            int count = amount.getAsInt();
            if (count <= 0) throw new IllegalArgumentException("Field '" + key + "' amount for " + id + " must be > 0");
            if (result.putIfAbsent(id, count) != null) throw new IllegalArgumentException("Field '" + key + "' contains duplicate id " + id);
        }
        return Map.copyOf(result);
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
