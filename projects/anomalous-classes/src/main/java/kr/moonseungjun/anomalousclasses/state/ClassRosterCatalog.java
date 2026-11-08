package kr.moonseungjun.anomalousclasses.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Keeps the packaged, player-facing roster aligned with the server's selectable class IDs. */
public final class ClassRosterCatalog {
    private static final String RESOURCE = "data/anomalousclasses/class_catalog.json";
    private static final List<String> ON_HOLD = List.of("gravity_caster", "resonance_caster");

    private ClassRosterCatalog() {}

    public static void validate() {
        try (InputStream stream = ClassRosterCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing class roster resource: " + RESOURCE);
            JsonObject root = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> available = values(root.getAsJsonArray("available"));
            List<String> expected = Arrays.stream(ClassId.values()).map(ClassId::key).toList();
            if (!available.equals(expected)) throw new IllegalStateException("Packaged class roster does not match ClassId order: " + available);
            List<String> onHold = values(root.getAsJsonArray("on_hold"));
            if (!ON_HOLD.equals(onHold)) throw new IllegalStateException("Packaged on-hold roster changed unexpectedly: " + onHold);
            if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported class roster schema");
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Invalid packaged class roster", exception);
        }
    }

    private static List<String> values(JsonArray array) {
        if (array == null) throw new IllegalStateException("Class roster list is missing");
        return array.asList().stream().map(element -> element.getAsString()).toList();
    }
}
