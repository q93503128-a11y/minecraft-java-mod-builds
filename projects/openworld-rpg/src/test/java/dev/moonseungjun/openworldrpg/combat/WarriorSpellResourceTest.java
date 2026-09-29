package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorSkillRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorSkillShape;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class WarriorSpellResourceTest {
    @Test
    void rootSkillAuthorityMatchesLockedCanon() {
        var driving = ProjectSpellSpec.warriorDrivingSlash();
        var counter = ProjectSpellSpec.warriorIronCounter();
        var cyclone = ProjectSpellSpec.warriorCycloneCut();
        var breaker = ProjectSpellSpec.warriorBreakerSlam();
        var ultimate = ProjectSpellSpec.warriorEarthshatter();

        assertEquals(20.0, driving.manaCost(), 0.0001);
        assertEquals(120, driving.cooldownTicks());
        assertEquals(18.0, counter.staminaCost(), 0.0001);
        assertEquals(0.0, counter.manaCost(), 0.0001);
        assertEquals(200, counter.cooldownTicks());
        assertEquals(28.0, cyclone.manaCost(), 0.0001);
        assertEquals(220, cyclone.cooldownTicks());
        assertEquals(36.0, breaker.manaCost(), 0.0001);
        assertEquals(300, breaker.cooldownTicks());
        assertEquals(0.0, ultimate.manaCost(), 0.0001);
        assertEquals(0.0, ultimate.staminaCost(), 0.0001);
        assertEquals(0, ultimate.cooldownTicks());

        for (var spec : List.of(
                driving,
                counter,
                cyclone,
                breaker,
                ultimate
        )) {
            assertEquals(
                    RootClass.WARRIOR,
                    ProjectSpellSpec.requiredRootClass(
                            spec.id()
                    ).orElseThrow()
            );
        }

        assertEquals(
                3.5,
                WarriorSkillShape.DRIVING_SLASH_RANGE,
                0.0001
        );
        assertEquals(
                3.3,
                WarriorSkillShape.CYCLONE_RADIUS,
                0.0001
        );
        assertEquals(
                4.2,
                WarriorSkillShape.BREAKER_SLAM_RANGE,
                0.0001
        );
        assertEquals(
                7.0,
                WarriorSkillShape.EARTHSHATTER_RANGE,
                0.0001
        );

        assertEquals(
                1.65,
                WarriorSkillRuntime.DRIVING_SLASH_ACTION,
                0.0001
        );
        assertEquals(
                1.90,
                WarriorSkillRuntime.DRIVING_SLASH_EMPOWERED_ACTION,
                0.0001
        );
        assertEquals(
                1.55,
                WarriorSkillRuntime.IRON_COUNTER_ACTION,
                0.0001
        );
        assertEquals(
                2.10,
                WarriorSkillRuntime.CYCLONE_FIRST_ACTION
                        + WarriorSkillRuntime.CYCLONE_SECOND_ACTION,
                0.0001
        );
        assertEquals(
                2.30,
                WarriorSkillRuntime.CYCLONE_FIRST_ACTION
                        + WarriorSkillRuntime.CYCLONE_EMPOWERED_SECOND_ACTION,
                0.0001
        );
        assertEquals(
                2.90,
                WarriorSkillRuntime.BREAKER_SLAM_ACTION,
                0.0001
        );
        assertEquals(
                3.35,
                WarriorSkillRuntime.BREAKER_SLAM_EMPOWERED_ACTION,
                0.0001
        );
        assertEquals(
                6.00,
                WarriorSkillRuntime.EARTHSHATTER_ACTION,
                0.0001
        );
        assertEquals(
                1.60,
                WarriorSkillRuntime.BREAKER_HYPERARMOR_MULTIPLIER,
                0.0001
        );
        assertEquals(
                2.00,
                WarriorSkillRuntime.EARTHSHATTER_HYPERARMOR_MULTIPLIER,
                0.0001
        );
    }

    @Test
    void resourcesAreProjectNeutralAndCarryAuthoredModelEffects()
            throws Exception {
        assertResource(
                "warrior_driving_slash",
                3.5,
                0.35,
                9
        );
        assertResource(
                "warrior_iron_counter",
                2.0,
                0.0,
                10
        );
        assertResource(
                "warrior_cyclone_cut",
                3.3,
                0.20,
                12
        );
        assertResource(
                "warrior_breaker_slam",
                4.2,
                0.75,
                14
        );
        assertResource(
                "warrior_earthshatter",
                7.0,
                1.0,
                20
        );
    }

    private void assertResource(
            String id,
            double range,
            double castDuration,
            int modelDuration
    ) throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/"
                                + id
                                + ".json"
                );
        assertNotNull(stream, id + " must be packaged.");

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader)
                    .getAsJsonObject();
        }

        assertEquals(
                range,
                root.get("range").getAsDouble(),
                0.0001
        );
        assertEquals(
                castDuration,
                root.getAsJsonObject("active")
                        .getAsJsonObject("cast")
                        .get("duration")
                        .getAsDouble(),
                0.0001
        );
        assertEquals(
                "NONE",
                root.getAsJsonObject("target")
                        .get("type")
                        .getAsString()
        );
        assertEquals(
                "DIRECT",
                root.getAsJsonObject("deliver")
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
                "openworld_rpg:spell_effect/" + id,
                model.get("model_id").getAsString()
        );
        assertEquals(
                modelDuration,
                model.get("duration").getAsInt()
        );

        JsonObject cost = root.getAsJsonObject("cost");
        assertEquals(
                0.0,
                cost.get("exhaust").getAsDouble(),
                0.0001
        );
        assertEquals(0, cost.get("durability").getAsInt());
        assertEquals(
                0.0,
                cost.getAsJsonObject("cooldown")
                        .get("duration")
                        .getAsDouble(),
                0.0001
        );
    }
}
