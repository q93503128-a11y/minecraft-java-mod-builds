package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.HunterPowerShotRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HunterPowerShotSpellResourceTest {
    @Test
    void authorityMatchesLockedHunterCanon() {
        var spec = ProjectSpellSpec.hunterPowerShot();
        assertEquals(ProjectSpellSpec.HUNTER_POWER_SHOT_ID, spec.id());
        assertEquals(34.0, spec.manaCost(), 0.0001);
        assertEquals(280, spec.cooldownTicks());
        assertEquals(3.00, spec.actionCoefficient(), 0.0001);
        assertEquals(1.60, spec.poiseCoefficient(), 0.0001);
        assertEquals(
                RootClass.HUNTER,
                ProjectSpellSpec.requiredRootClass(spec.id()).orElseThrow()
        );
    }

    @Test
    void resourceUsesCommittedArcheryWindupAndHighSpeedPhysicalArrow()
            throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(
                "data/openworld_rpg/spell/hunter_power_shot.json"
        );
        assertNotNull(stream);
        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals(
                "spell_power:physical_ranged",
                root.get("school").getAsString()
        );
        JsonObject cast = root
                .getAsJsonObject("active")
                .getAsJsonObject("cast");
        assertEquals(0.70, cast.get("duration").getAsDouble(), 0.0001);
        assertEquals("STANDARD", cast.get("type").getAsString());
        assertEquals(
                "spell_engine:archery_pull",
                cast.getAsJsonObject("animation").get("id").getAsString()
        );
        assertEquals(
                "spell_engine:archery_release",
                root.getAsJsonObject("release")
                        .getAsJsonObject("animation")
                        .get("id").getAsString()
        );

        JsonObject projectile = root
                .getAsJsonObject("deliver")
                .getAsJsonObject("projectile");
        assertEquals(
                HunterPowerShotRules.PROJECTILE_VELOCITY,
                projectile.getAsJsonObject("launch_properties")
                        .get("velocity").getAsDouble(),
                0.0001
        );
        assertEquals(
                0,
                projectile.getAsJsonObject("projectile")
                        .getAsJsonObject("perks")
                        .get("pierce").getAsInt()
        );
        assertEquals(
                "openworld_rpg:spell_projectile/hunter_power_arrow",
                projectile.getAsJsonObject("projectile")
                        .getAsJsonObject("client_data")
                        .getAsJsonObject("composite_model")
                        .getAsJsonArray("models")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("fx")
                        .get("model_id").getAsString()
        );
    }
}
