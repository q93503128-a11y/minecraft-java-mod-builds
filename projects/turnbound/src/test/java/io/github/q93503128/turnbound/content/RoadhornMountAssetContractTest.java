package io.github.q93503128.turnbound.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadhornMountAssetContractTest {
    private static final String MODEL = "assets/turnbound/geckolib/models/entity/mount/roadhorn_mount.geo.json";
    private static final String ANIMATION = "assets/turnbound/geckolib/animations/entity/mount/roadhorn_mount.animation.json";
    private static final String TEXTURE = "assets/turnbound/textures/entity/elite/elite_cv_cavehorn_ravager.png";

    @Test
    void dedicatedMountRetainsSaddleRigAndHasMountedLocomotionStates() {
        JsonObject geometry = load(MODEL).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertEquals("geometry.roadhorn_mount",
                geometry.getAsJsonObject("description").get("identifier").getAsString());

        Set<String> bones = new HashSet<>();
        for (var raw : geometry.getAsJsonArray("bones")) bones.add(raw.getAsJsonObject().get("name").getAsString());
        assertTrue(bones.contains("chest"));
        assertTrue(bones.contains("Saddle"));
        assertTrue(bones.contains("Saddle7"));
        assertTrue(bones.contains("leftHorn"));
        assertTrue(bones.contains("rightHorn"));

        JsonObject animations = load(ANIMATION).getAsJsonObject("animations");
        for (String clip : Set.of(
                "animation.roadhorn_mount.idle",
                "animation.roadhorn_mount.walk",
                "animation.roadhorn_mount.gallop",
                "animation.roadhorn_mount.jump")) {
            assertTrue(animations.has(clip), "Roadhorn mount missing " + clip);
        }
        assertResource(TEXTURE);
    }

    private static JsonObject load(String resource) {
        InputStream stream = RoadhornMountAssetContractTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, "Missing production resource " + resource);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            throw new AssertionError("Could not parse production resource " + resource, ex);
        }
    }

    private static void assertResource(String resource) {
        try (InputStream stream = RoadhornMountAssetContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing production resource " + resource);
            assertTrue(stream.read() >= 0, "Empty production resource " + resource);
        } catch (Exception ex) {
            throw new AssertionError("Could not read production resource " + resource, ex);
        }
    }
}
