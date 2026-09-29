package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.SanctuaryRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.SanctuaryZoneShape;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SanctuarySpellResourceTest {
    @Test
    void authorityContractMatchesClericCanon() {
        var spec = ProjectSpellSpec.sanctuary();
        assertEquals(ProjectSpellSpec.SANCTUARY_ID, spec.id());
        assertEquals(0.0, spec.manaCost(), 0.0001);
        assertEquals(0, spec.cooldownTicks());
        assertEquals(
                RootClass.CLERIC,
                ProjectSpellSpec.requiredRootClass(spec.id()).orElseThrow()
        );
        assertEquals(7.0, SanctuaryZoneShape.RADIUS_BLOCKS, 0.0001);
        assertEquals(160, SanctuaryRuntime.DURATION_TICKS);
        assertEquals(8, SanctuaryRuntime.PULSE_COUNT);
        assertEquals(20, SanctuaryRuntime.PULSE_INTERVAL_TICKS);
        assertEquals(
                0.25,
                SanctuaryRuntime.INITIAL_BARRIER_COEFFICIENT,
                0.0001
        );
        assertEquals(
                0.55,
                SanctuaryRuntime.TOTAL_HEAL_COEFFICIENT,
                0.0001
        );
        assertEquals(
                2.00,
                SanctuaryRuntime.TOTAL_ENEMY_ACTION_COEFFICIENT,
                0.0001
        );
        assertEquals(
                0.80,
                SanctuaryRuntime.NEGATIVE_STATUS_DURATION_MULTIPLIER,
                0.0001
        );
    }

    @Test
    void spellResourceIsNeutralAndCarriesEightSecondWardPresentation()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/sanctuary.json"
                );
        assertNotNull(stream, "Sanctuary spell resource must be packaged.");

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals(7.0, root.get("range").getAsDouble(), 0.0001);
        assertEquals(
                "NONE",
                root.getAsJsonObject("target")
                        .get("type")
                        .getAsString()
        );
        assertTrue(root.getAsJsonArray("impacts").isEmpty());

        JsonObject model = root
                .getAsJsonObject("release")
                .getAsJsonObject("visuals")
                .getAsJsonArray("models")
                .get(0)
                .getAsJsonObject();
        assertEquals(
                "openworld_rpg:spell_effect/sanctuary_ward",
                model.get("model_id").getAsString()
        );
        assertEquals(160, model.get("duration").getAsInt());

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
