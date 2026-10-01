package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianSkillRuntime;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuardianSpellResourceTest {
    @Test
    void rootSkillAuthorityMatchesLockedCanon() {
        var rush = ProjectSpellSpec.guardianBulwarkRush();
        var warding = ProjectSpellSpec.guardianWardingStrike();
        var aegis = ProjectSpellSpec.guardianAegisField();
        var counterwall = ProjectSpellSpec.guardianCounterwall();
        var ultimate = ProjectSpellSpec.guardianUnbrokenLine();

        assertEquals(20.0, rush.staminaCost(), 0.0001);
        assertEquals(160, rush.cooldownTicks());
        assertEquals(20.0, warding.manaCost(), 0.0001);
        assertEquals(140, warding.cooldownTicks());
        assertEquals(30.0, aegis.manaCost(), 0.0001);
        assertEquals(300, aegis.cooldownTicks());
        assertEquals(22.0, counterwall.staminaCost(), 0.0001);
        assertEquals(240, counterwall.cooldownTicks());
        assertEquals(0.0, ultimate.manaCost(), 0.0001);
        assertEquals(0.0, ultimate.staminaCost(), 0.0001);
        assertEquals(0, ultimate.cooldownTicks());

        for (var spec : List.of(
                rush,
                warding,
                aegis,
                counterwall,
                ultimate
        )) {
            assertEquals(
                    RootClass.GUARDIAN,
                    ProjectSpellSpec.requiredRootClass(
                            spec.id()
                    ).orElseThrow()
            );
        }

        assertEquals(
                1.45,
                GuardianSkillRuntime.BULWARK_RUSH_ACTION,
                0.0001
        );
        assertEquals(
                1.80,
                GuardianSkillRuntime.BULWARK_RUSH_POISE,
                0.0001
        );
        assertEquals(
                1.60,
                GuardianSkillRuntime.WARDING_STRIKE_ACTION,
                0.0001
        );
        assertEquals(
                1.40,
                GuardianSkillRuntime.WARDING_STRIKE_POISE,
                0.0001
        );
        assertEquals(
                0.18,
                GuardianSkillRuntime.AEGIS_FIELD_BASE_BARRIER,
                0.0001
        );
        assertEquals(
                0.04,
                GuardianSkillRuntime.AEGIS_FIELD_BARRIER_PER_RESOLVE,
                0.0001
        );
        assertEquals(
                15L,
                GuardianSkillRuntime.COUNTERWALL_STANCE_TICKS
        );
        assertEquals(
                0.22,
                GuardianSkillRuntime.UNBROKEN_LINE_INITIAL_BARRIER,
                0.0001
        );
        assertEquals(
                160L,
                GuardianSkillRuntime.UNBROKEN_LINE_DURATION_TICKS
        );
        assertEquals(
                1.25,
                GuardianSkillRuntime.STAND_FIRM_POISE_MULTIPLIER,
                0.0001
        );
        assertEquals(
                0.70,
                GuardianSkillRuntime
                        .STAND_FIRM_ORDINARY_KNOCKBACK_MULTIPLIER,
                0.0001
        );
    }

    @Test
    void resourcesAreProjectNeutralAndCarryAuthoredModelEffects()
            throws Exception {
        assertResource("guardian_bulwark_rush", 3.8, 0.40, 10);
        assertResource("guardian_warding_strike", 3.5, 0.35, 12);
        assertResource("guardian_aegis_field", 5.0, 0.45, 24);
        assertResource("guardian_counterwall", 4.0, 0.0, 15);
        assertResource("guardian_unbroken_line", 7.0, 0.60, 160);
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

        assertEquals(range, root.get("range").getAsDouble(), 0.0001);
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
        assertEquals(0.0, cost.get("exhaust").getAsDouble(), 0.0001);
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
