package kr.moonseungjun.riftfrontier.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.Objects;

/** Loads a versioned pack of content definitions into an isolated registry and validates all references. */
public final class ContentPackLoader {
    public static final int CURRENT_CONTENT_SCHEMA = 1;

    public record LoadedPack(int schemaVersion, String packId, ContentRegistry registry, ContentValidator.Report validation) {
        public LoadedPack {
            packId = Objects.requireNonNull(packId);
            registry = Objects.requireNonNull(registry);
            validation = Objects.requireNonNull(validation);
        }

        public void requireValid() {
            if (validation.hasErrors()) {
                throw new IllegalStateException("Content pack '" + packId + "' failed validation:\n" + validation.format());
            }
        }
    }

    private final ContentDocumentCodec codec;
    private final ContentValidator validator;

    public ContentPackLoader() {
        this(new ContentDocumentCodec(), new ContentValidator());
    }

    ContentPackLoader(ContentDocumentCodec codec, ContentValidator validator) {
        this.codec = Objects.requireNonNull(codec);
        this.validator = Objects.requireNonNull(validator);
    }

    public LoadedPack load(Reader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) throw new IllegalArgumentException("Content pack root must be a JSON object");
        JsonObject object = root.getAsJsonObject();

        int schemaVersion = requireInt(object, "schema_version");
        if (schemaVersion != CURRENT_CONTENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported content schema " + schemaVersion + "; expected " + CURRENT_CONTENT_SCHEMA);
        }
        String packId = requireString(object, "pack_id");
        JsonArray definitions = requireArray(object, "definitions");

        ContentRegistry registry = new ContentRegistry();
        for (int index = 0; index < definitions.size(); index++) {
            try {
                registry.register(codec.decode(definitions.get(index)));
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("Invalid definition at index " + index + " in pack '" + packId + "': " + error.getMessage(), error);
            }
        }

        ContentValidator.Report report = validator.validate(registry);
        return new LoadedPack(schemaVersion, packId, registry, report);
    }

    private static int requireInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing or non-number field '" + key + "'");
        }
        return value.getAsInt();
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
}
