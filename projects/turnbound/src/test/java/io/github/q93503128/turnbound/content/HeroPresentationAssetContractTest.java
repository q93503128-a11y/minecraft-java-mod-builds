package io.github.q93503128.turnbound.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the v0.4 §38.8 minimum animation contract for all eight core heroes. */
class HeroPresentationAssetContractTest {
    private static final Map<String, String> HERO_ANIMATIONS = Map.of(
            "P01", "p01_kyren",
            "P02", "p02_lumea",
            "P03", "p03_bram",
            "P04", "p04_elysia",
            "P05", "p05_lynette",
            "P06", "p06_morwen",
            "P07", "p07_marion",
            "P08", "p08_raze");

    private static final List<String> REQUIRED_CLIPS = List.of(
            "idle", "turn_ready", "move_attack", "basic", "active_1", "active_2",
            "hit_light", "hit_heavy", "buff", "debuff", "death", "revive", "victory", "field_idle");

    @Test
    void everyCoreHeroShipsTheCanonicalFourteenClipMinimum() {
        for (var entry : HERO_ANIMATIONS.entrySet()) {
            String resource = "assets/turnbound/geckolib/animations/entity/hero/" + entry.getValue() + ".animation.json";
            JsonObject root = load(resource);
            JsonObject animations = root.getAsJsonObject("animations");
            assertNotNull(animations, entry.getKey() + " is missing animations object");
            assertTrue(animations.size() >= 14,
                    () -> entry.getKey() + " only ships " + animations.size() + " clips; v0.4 requires at least 14");

            Set<String> names = animations.keySet();
            for (String clip : REQUIRED_CLIPS) {
                String fullName = "animation." + entry.getValue() + "." + clip;
                assertTrue(names.contains(fullName), () -> entry.getKey() + " is missing required clip " + fullName);
            }
        }
    }

    @Test
    void fieldLocomotionClipIsAuthoredForFutureWorldHeroActors() {
        for (var entry : HERO_ANIMATIONS.entrySet()) {
            JsonObject animations = load("assets/turnbound/geckolib/animations/entity/hero/" + entry.getValue() + ".animation.json")
                    .getAsJsonObject("animations");
            assertTrue(animations.has("animation." + entry.getValue() + ".field_walk"),
                    () -> entry.getKey() + " has no authored field_walk clip");
        }
    }

    private static JsonObject load(String resource) {
        ClassLoader loader = HeroPresentationAssetContractTest.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(resource);
        assertNotNull(stream, "Missing presentation resource " + resource);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            throw new AssertionError("Could not parse presentation resource " + resource, ex);
        }
    }
}
