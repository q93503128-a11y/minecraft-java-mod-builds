package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RadiantLanceSpellResourceTest {
    @Test
    void authorityContractMatchesClericCanon() {
        var spec = ProjectSpellSpec.radiantLance();
        assertEquals(
                ProjectSpellSpec.RADIANT_LANCE_ID,
                spec.id()
        );
        assertEquals(14.0, spec.manaCost(), 0.0001);
        assertEquals(80, spec.cooldownTicks());
        assertEquals(1.35, spec.actionCoefficient(), 0.0001);
        assertEquals(0.60, spec.poiseCoefficient(), 0.0001);
        assertEquals(
                RootClass.CLERIC,
                ProjectSpellSpec.requiredRootClass(
                        spec.id()
                ).orElseThrow()
        );
        assertEquals(
                0.55,
                ProjectSpellSpec.RADIANT_LANCE_CHAIN_ACTION_COEFFICIENT,
                0.0001
        );
        assertEquals(
                0.08,
                ProjectSpellSpec.RADIANT_LANCE_FALLBACK_HEAL_COEFFICIENT,
                0.0001
        );
    }

    @Test
    void spellResourceUsesRealProjectMeshAndMatchedObb() throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/radiant_lance.json"
                );
        assertNotNull(
                stream,
                "Radiant Lance Spell Engine resource must be packaged."
        );

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals(
                20.0,
                root.get("range").getAsDouble(),
                0.0001
        );
        var projectile = root.getAsJsonObject("deliver")
                .getAsJsonObject("projectile")
                .getAsJsonObject("projectile");
        var hitbox = projectile.getAsJsonObject("hitbox");
        assertEquals(0.22, hitbox.get("width").getAsDouble(), 0.0001);
        assertEquals(0.22, hitbox.get("height").getAsDouble(), 0.0001);
        assertEquals(1.15, hitbox.get("length").getAsDouble(), 0.0001);

        var model = projectile
                .getAsJsonObject("client_data")
                .getAsJsonObject("composite_model")
                .getAsJsonArray("models")
                .get(0).getAsJsonObject();
        assertEquals(
                "ALONG_MOTION",
                model.get("orientation").getAsString()
        );
        assertEquals(
                "openworld_rpg:spell_projectile/radiant_lance",
                model.getAsJsonObject("fx")
                        .get("model_id").getAsString()
        );
        assertEquals(
                0.0,
                model.get("rotate_degrees_per_tick").getAsDouble(),
                0.0001
        );

        var action = root.getAsJsonArray("impacts")
                .get(0).getAsJsonObject()
                .getAsJsonObject("action");
        assertEquals("CUSTOM", action.get("type").getAsString());
        assertEquals(
                "HARMFUL",
                action.getAsJsonObject("custom")
                        .get("intent").getAsString()
        );

        var cost = root.getAsJsonObject("cost");
        assertFalse(cost.get("batching").getAsBoolean());
        assertEquals(0, cost.get("durability").getAsInt());
        assertTrue(
                !cost.has("item") && !cost.has("effect_id")
        );
        assertEquals(
                0.0,
                cost.getAsJsonObject("cooldown")
                        .get("duration").getAsDouble(),
                0.0001
        );
    }
}
