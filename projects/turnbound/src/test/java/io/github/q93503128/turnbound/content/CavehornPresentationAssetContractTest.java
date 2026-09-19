package io.github.q93503128.turnbound.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CavehornPresentationAssetContractTest {
    private static final String ROOT = "elite_cv_cavehorn_ravager";
    private static final String MODEL = "assets/turnbound/geckolib/models/entity/elite/" + ROOT + ".geo.json";
    private static final String ANIMATION = "assets/turnbound/geckolib/animations/entity/elite/" + ROOT + ".animation.json";
    private static final String TEXTURE = "assets/turnbound/textures/entity/elite/" + ROOT + ".png";
    private static final Set<String> REQUIRED = Set.of(
            "misc.idle", "misc.turn_ready", "attack.strike", "attack.cast",
            "boss.telegraph", "boss.charge", "combat.hit", "misc.death",
            "misc.revive", "misc.victory", "field.walk", "field.idle");

    @Test
    void cavehornUsesTheTrackedHornedQuadrupedProductionRigWithoutMountGear() {
        JsonObject geometry = load(MODEL).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertEquals("geometry.turnbound.elite_cv_cavehorn_ravager",
                geometry.getAsJsonObject("description").get("identifier").getAsString());

        Set<String> bones = new HashSet<>();
        for (var raw : geometry.getAsJsonArray("bones")) bones.add(raw.getAsJsonObject().get("name").getAsString());
        assertTrue(bones.contains("leftHorn"));
        assertTrue(bones.contains("rightHorn"));
        assertTrue(bones.contains("body"));
        assertTrue(bones.contains("fLeftLeg"));
        assertTrue(bones.contains("rRightLeg"));
        assertFalse(bones.contains("chest"));
        assertTrue(bones.stream().noneMatch(name -> name.startsWith("Saddle")));

        JsonObject animations = load(ANIMATION).getAsJsonObject("animations");
        for (String clip : REQUIRED) {
            assertTrue(animations.has(clip), "Cavehorn missing " + clip);
            JsonObject raw = animations.getAsJsonObject(clip);
            if (!raw.has("bones")) continue;
            for (String bone : raw.getAsJsonObject("bones").keySet()) {
                assertTrue(bones.contains(bone), clip + " references missing bone " + bone);
            }
        }
        assertResource(TEXTURE);
    }

    @Test
    void warningCaveEncounterUsesOnlyTheProductionCavehornElite() {
        assertTrue(CanonicalData.contains("EL_CV01"));
        var encounter = CampaignEncounterCatalog.spec("CV_WARNING_CAVE_ELITE");
        assertEquals(3, encounter.level());
        assertEquals(List.of("EL_CV01"), encounter.enemies());
        assertEquals(900, encounter.respawnSeconds());
        assertFalse(encounter.boss());
    }

    private static JsonObject load(String resource) {
        InputStream stream = CavehornPresentationAssetContractTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, "Missing production resource " + resource);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            throw new AssertionError("Could not parse production resource " + resource, ex);
        }
    }

    private static void assertResource(String resource) {
        try (InputStream stream = CavehornPresentationAssetContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing production resource " + resource);
            assertTrue(stream.read() >= 0, "Empty production resource " + resource);
        } catch (Exception ex) {
            throw new AssertionError("Could not read production resource " + resource, ex);
        }
    }
}
