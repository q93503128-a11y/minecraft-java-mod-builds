package io.github.q93503128.turnbound.content;

import com.google.gson.JsonArray;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production contract for the first Drehmal enemy visual: no vanilla or placeholder presentation. */
class CapitalValleyEnemyPresentationAssetContractTest {
    private static final String MODEL = "assets/turnbound/geckolib/models/entity/enemy/cv_a_mossback_boar.geo.json";
    private static final String ANIMATION = "assets/turnbound/geckolib/animations/entity/enemy/cv_a_mossback_boar.animation.json";
    private static final String TEXTURE = "assets/turnbound/textures/entity/enemy/cv_a_mossback_boar.png";
    private static final Set<String> REQUIRED_CLIPS = Set.of(
            "misc.idle", "misc.turn_ready", "attack.strike", "boss.charge",
            "combat.hit", "misc.death", "misc.revive", "misc.victory",
            "field.walk", "field.idle", "boss.telegraph");

    @Test
    void cvAUsesARealExternalProductionRigAndCompleteFieldBattleClipSet() {
        JsonObject geometry = loadJson(MODEL).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertEquals("geometry.turnbound.enemy_cv_a_mossback_boar",
                geometry.getAsJsonObject("description").get("identifier").getAsString());
        JsonArray bones = geometry.getAsJsonArray("bones");
        assertTrue(bones.size() >= 9, "CV-A external boar rig lost required body parts");

        Set<String> boneNames = new HashSet<>();
        for (var bone : bones) boneNames.add(bone.getAsJsonObject().get("name").getAsString());

        JsonObject animations = loadJson(ANIMATION).getAsJsonObject("animations");
        for (String clip : REQUIRED_CLIPS) {
            assertTrue(animations.has(clip), "CV-A missing " + clip);
            JsonObject raw = animations.getAsJsonObject(clip);
            if (!raw.has("bones")) continue;
            for (String referenced : raw.getAsJsonObject("bones").keySet()) {
                assertTrue(boneNames.contains(referenced), clip + " references missing bone " + referenced);
            }
        }

        ClassLoader loader = CapitalValleyEnemyPresentationAssetContractTest.class.getClassLoader();
        try (InputStream texture = loader.getResourceAsStream(TEXTURE)) {
            assertNotNull(texture, "CV-A production texture missing");
            assertTrue(texture.read() >= 0, "CV-A production texture is empty");
        } catch (Exception ex) {
            throw new AssertionError("Could not read CV-A production texture", ex);
        }
    }

    @Test
    void firstCapitalValleyEncounterUsesOnlyTheProductionCvAIdentity() {
        assertTrue(CanonicalData.contains("CV_A"));
        var encounter = CampaignEncounterCatalog.spec("CV_FIRST_COMMON");
        assertEquals(1, encounter.level());
        assertEquals(List.of("CV_A", "CV_A"), encounter.enemies());
        assertTrue(!encounter.boss());
    }

    private static JsonObject loadJson(String resource) {
        ClassLoader loader = CapitalValleyEnemyPresentationAssetContractTest.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(resource);
        assertNotNull(stream, "Missing production resource " + resource);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            throw new AssertionError("Could not parse production resource " + resource, ex);
        }
    }
}
