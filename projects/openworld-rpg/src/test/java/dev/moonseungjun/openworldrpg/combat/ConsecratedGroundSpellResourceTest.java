package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.ConsecratedGroundRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ConsecratedGroundZoneShape;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ConsecratedGroundSpellResourceTest {
    @Test
    void authorityContractMatchesClericCanon() {
        var spec = ProjectSpellSpec.consecratedGround();
        assertEquals(ProjectSpellSpec.CONSECRATED_GROUND_ID, spec.id());
        assertEquals(32.0, spec.manaCost(), 0.0001);
        assertEquals(280, spec.cooldownTicks());
        assertEquals(
                RootClass.CLERIC,
                ProjectSpellSpec.requiredRootClass(spec.id()).orElseThrow()
        );
        assertEquals(
                5.0,
                ConsecratedGroundZoneShape.RADIUS_BLOCKS,
                0.0001
        );
        assertEquals(100, ConsecratedGroundRuntime.DURATION_TICKS);
        assertEquals(20, ConsecratedGroundRuntime.PULSE_INTERVAL_TICKS);
        assertEquals(5, ConsecratedGroundRuntime.PULSE_COUNT);
        assertEquals(
                0.30,
                ConsecratedGroundRuntime.TOTAL_HEAL_COEFFICIENT,
                0.0001
        );
        assertEquals(
                1.35,
                ConsecratedGroundRuntime.TOTAL_ENEMY_ACTION_COEFFICIENT,
                0.0001
        );
        assertEquals(
                0.12,
                ConsecratedGroundRuntime.EMPOWERED_BARRIER_COEFFICIENT,
                0.0001
        );
    }

    @Test
    void spellResourceIsNeutralAndCarriesFiveSecondGroundPresentation()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/consecrated_ground.json"
                );
        assertNotNull(
                stream,
                "Consecrated Ground spell resource must be packaged."
        );

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals(5.0, root.get("range").getAsDouble(), 0.0001);
        assertEquals(
                "NONE",
                root.getAsJsonObject("target")
                        .get("type")
                        .getAsString()
        );
        assertEquals(0, root.getAsJsonArray("impacts").size());

        JsonObject model = root
                .getAsJsonObject("release")
                .getAsJsonObject("visuals")
                .getAsJsonArray("models")
                .get(0)
                .getAsJsonObject();
        assertEquals(
                "openworld_rpg:spell_effect/consecrated_ground",
                model.get("model_id").getAsString()
        );
        assertEquals(100, model.get("duration").getAsInt());

        JsonObject cost = root.getAsJsonObject("cost");
        assertEquals(0.0, cost.get("exhaust").getAsDouble(), 0.0001);
        assertEquals(0, cost.get("durability").getAsInt());
        JsonObject cooldown = cost.getAsJsonObject("cooldown");
        assertEquals(
                0.0,
                cooldown.get("attempt_duration").getAsDouble(),
                0.0001
        );
        assertEquals(
                0.0,
                cooldown.get("duration").getAsDouble(),
                0.0001
        );
    }
}
