package io.github.q93503128.turnbound.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the four authored 3D relation sigils used instead of text-only target-state reads. */
class HeroRelationMarkerAssetContractTest {
    private static final Map<String, String> MARKERS = Map.of(
            "duel", "relation_duel",
            "sightline", "relation_sightline",
            "sanctuary", "relation_sanctuary",
            "partner_guard", "relation_partner_guard");

    @Test
    void everyRelationShipsModelTextureAndReadableIdleEntrance() {
        for (var entry : MARKERS.entrySet()) {
            String id = entry.getKey();
            JsonObject geometry = loadJson("assets/turnbound/geckolib/models/entity/relation/" + id + ".geo.json");
            assertTrue(geometry.has("minecraft:geometry"), id + " relation geometry missing");

            JsonObject animations = loadJson("assets/turnbound/geckolib/animations/entity/relation/" + id + ".animation.json")
                    .getAsJsonObject("animations");
            assertNotNull(animations, id + " relation animations missing");
            assertTrue(animations.has("animation." + entry.getValue() + ".idle"), id + " idle clip missing");
            assertTrue(animations.has("animation." + entry.getValue() + ".turn_ready"), id + " entrance clip missing");

            String texture = "assets/turnbound/textures/entity/relation/" + id + ".png";
            try (InputStream stream = HeroRelationMarkerAssetContractTest.class.getClassLoader().getResourceAsStream(texture)) {
                assertNotNull(stream, "Missing relation texture " + texture);
            } catch (Exception ex) {
                throw new AssertionError("Could not read relation texture " + texture, ex);
            }
        }
    }

    private static JsonObject loadJson(String path) {
        InputStream stream = HeroRelationMarkerAssetContractTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing relation resource " + path);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            throw new AssertionError("Could not parse relation resource " + path, ex);
        }
    }
}
