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

/** Guards the production hero animation minimum and v1 signature-state readability contract. */
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
    private static final Map<String, String> SIGNATURE_STATE_HEROES = Map.of(
            "P01", "p01_kyren",
            "P03", "p03_bram",
            "P05", "p05_lynette",
            "P06", "p06_morwen",
            "P07", "p07_marion",
            "P08", "p08_raze");
    private static final Map<String, String> SIGNATURE_PAYOFF_HEROES = Map.ofEntries(
            Map.entry("P01", "p01_kyren"),
            Map.entry("P03", "p03_bram"),
            Map.entry("P04", "p04_elysia"),
            Map.entry("P05", "p05_lynette"),
            Map.entry("P06", "p06_morwen"),
            Map.entry("P07", "p07_marion"),
            Map.entry("P08", "p08_raze"),
            Map.entry("P07_SUMMON", "toto"));

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
    void signatureResourceHeroesShipPersistentBodyLanguageStates() {
        for (var entry : SIGNATURE_STATE_HEROES.entrySet()) {
            JsonObject animations = load("assets/turnbound/geckolib/animations/entity/hero/" + entry.getValue() + ".animation.json")
                    .getAsJsonObject("animations");
            for (int stage = 0; stage <= 3; stage++) {
                String clip = "animation." + entry.getValue() + ".state_" + stage;
                assertTrue(animations.has(clip), () -> entry.getKey() + " is missing signature-state clip " + clip);
            }
        }

        JsonObject raze = load("assets/turnbound/geckolib/animations/entity/hero/p08_raze.animation.json")
                .getAsJsonObject("animations");
        assertTrue(raze.has("animation.p08_raze.overheat"));

        JsonObject toto = load("assets/turnbound/geckolib/animations/entity/hero/toto.animation.json")
                .getAsJsonObject("animations");
        for (int stage = 0; stage <= 3; stage++) {
            assertTrue(toto.has("animation.toto.state_" + stage), "Toto must mirror Marion Bond body language");
        }
    }

    @Test
    void signaturePayoffActorsShipDistinctAdditiveBeatClips() {
        for (var entry : SIGNATURE_PAYOFF_HEROES.entrySet()) {
            JsonObject animations = load("assets/turnbound/geckolib/animations/entity/hero/" + entry.getValue() + ".animation.json")
                    .getAsJsonObject("animations");
            String clip = "animation." + entry.getValue() + ".signature_payoff";
            assertTrue(animations.has(clip), () -> entry.getKey() + " is missing signature payoff clip " + clip);
        }
        JsonObject morwen = load("assets/turnbound/geckolib/animations/entity/hero/p06_morwen.animation.json")
                .getAsJsonObject("animations");
        assertTrue(morwen.has("animation.p06_morwen.signature_return"));
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
