package io.github.q93503128.turnbound.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the authored v0.4 boss model/animation production contract. */
class BossPresentationAssetContractTest {
    private static final Map<String, String> BOSSES = Map.of(
            "B01", "graul", "B02", "verna", "B03", "oro7", "B04", "kolvak", "B05", "serak");

    private static final List<String> REQUIRED_CLIPS = List.of(
            "misc.idle", "misc.turn_ready", "attack.strike", "attack.cast",
            "boss.telegraph", "boss.phase_enter", "boss.charge", "boss.summon",
            "boss.stagger", "boss.hit_light", "boss.hit_heavy",
            "misc.revive", "misc.victory", "misc.death");

    @Test
    void everyBossShipsAuthoredThirtyFiveBoneModelAndFourteenClipRig() {
        for (var entry : BOSSES.entrySet()) {
            String bossId = entry.getKey();
            String path = entry.getValue();
            JsonObject geometryRoot = load("assets/turnbound/geckolib/models/entity/boss/" + path + ".geo.json");
            JsonArray geometries = geometryRoot.getAsJsonArray("minecraft:geometry");
            assertNotNull(geometries, bossId + " is missing minecraft:geometry");
            assertTrue(!geometries.isEmpty(), bossId + " has no geometry entry");
            JsonArray bones = geometries.get(0).getAsJsonObject().getAsJsonArray("bones");
            assertNotNull(bones, bossId + " is missing bones");
            assertTrue(bones.size() >= 35,
                    () -> bossId + " only ships " + bones.size() + " bones; v0.4 boss production floor is 35");

            Set<String> boneNames = new HashSet<>();
            int visibleBones = 0;
            for (JsonElement boneElement : bones) {
                JsonObject bone = boneElement.getAsJsonObject();
                boneNames.add(bone.get("name").getAsString());
                if (bone.has("cubes") && !bone.getAsJsonArray("cubes").isEmpty()) visibleBones++;
            }
            assertTrue(visibleBones >= 35,
                    () -> bossId + " only has " + visibleBones + " visible geometry bones; empty padding bones do not count");

            JsonObject animationRoot = load("assets/turnbound/geckolib/animations/entity/boss/" + path + ".animation.json");
            JsonObject animations = animationRoot.getAsJsonObject("animations");
            assertNotNull(animations, bossId + " is missing animations object");
            assertTrue(animations.size() >= 14,
                    () -> bossId + " only ships " + animations.size() + " boss clips; v0.4 requires at least 14");
            for (String clip : REQUIRED_CLIPS) {
                assertTrue(animations.has(clip), () -> bossId + " is missing required boss clip " + clip);
            }

            JsonObject death = animations.getAsJsonObject("misc.death");
            double deathSeconds = death.get("animation_length").getAsDouble();
            assertTrue(deathSeconds >= 3.0 && deathSeconds <= 4.5,
                    () -> bossId + " death is " + deathSeconds + "s; v0.4 requires 3.0-4.5s");

            assertTrue(hasAuthoredBones(animations.getAsJsonObject("boss.telegraph")), bossId + " telegraph has no authored bone motion");
            assertTrue(hasAuthoredBones(animations.getAsJsonObject("boss.phase_enter")), bossId + " phase_enter has no authored bone motion");

            for (var animationEntry : animations.entrySet()) {
                JsonObject animation = animationEntry.getValue().getAsJsonObject();
                if (!animation.has("bones")) continue;
                for (String referencedBone : animation.getAsJsonObject("bones").keySet()) {
                    assertTrue(boneNames.contains(referencedBone),
                            () -> bossId + " clip " + animationEntry.getKey() + " references missing bone " + referencedBone);
                }
            }
        }
    }

    private static boolean hasAuthoredBones(JsonObject animation) {
        return animation != null && animation.has("bones") && !animation.getAsJsonObject("bones").entrySet().isEmpty();
    }

    private static JsonObject load(String resource) {
        ClassLoader loader = BossPresentationAssetContractTest.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(resource);
        assertNotNull(stream, "Missing presentation resource " + resource);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            throw new AssertionError("Could not parse presentation resource " + resource, ex);
        }
    }
}
