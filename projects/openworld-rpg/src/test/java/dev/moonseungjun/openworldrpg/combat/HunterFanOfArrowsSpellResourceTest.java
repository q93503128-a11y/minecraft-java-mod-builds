package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.HunterFanOfArrowsRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterFanOfArrowsRuntime;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HunterFanOfArrowsSpellResourceTest {
    @Test
    void authorityMatchesLockedHunterCanon() {
        var spec = ProjectSpellSpec.hunterFanOfArrows();
        assertEquals(ProjectSpellSpec.HUNTER_FAN_OF_ARROWS_ID, spec.id());
        assertEquals(26.0, spec.manaCost(), 0.0001);
        assertEquals(200, spec.cooldownTicks());
        assertEquals(1.90, spec.actionCoefficient(), 0.0001);
        assertEquals(1.00, spec.poiseCoefficient(), 0.0001);
        assertEquals(2.15, ProjectSpellSpec.HUNTER_FAN_EMPOWERED_ACTION_COEFFICIENT_CAP, 0.0001);
        assertEquals(RootClass.HUNTER, ProjectSpellSpec.requiredRootClass(spec.id()).orElseThrow());
        assertEquals(10, HunterFanOfArrowsRuntime.ACTION_TICKS);
    }

    @Test
    void resourceAuthorsFiveBaseShotsAndSevenOrderedConeOffsets() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(
                "data/openworld_rpg/spell/hunter_fan_of_arrows.json"
        );
        assertNotNull(stream);
        JsonObject root;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals("spell_power:physical_ranged", root.get("school").getAsString());
        assertEquals("spell_engine:archery_release",
                root.getAsJsonObject("release").getAsJsonObject("animation").get("id").getAsString());

        JsonObject delivery = root.getAsJsonObject("deliver").getAsJsonObject("projectile");
        JsonObject launch = delivery.getAsJsonObject("launch_properties");
        assertEquals(HunterFanOfArrowsRules.BASE_PROJECTILE_COUNT - 1,
                launch.get("extra_launch_count").getAsInt());
        assertEquals(1, launch.get("extra_launch_delay").getAsInt());

        JsonArray offsets = delivery.getAsJsonArray("direction_offsets");
        assertEquals(HunterFanOfArrowsRules.EMPOWERED_PROJECTILE_COUNT, offsets.size());
        double[] expected = {0, -4, 4, -8, 8, -12, 12};
        for (int i = 0; i < expected.length; i++) {
            JsonObject offset = offsets.get(i).getAsJsonObject();
            double yaw = offset.has("yaw") ? offset.get("yaw").getAsDouble() : 0.0;
            assertEquals(expected[i], yaw, 0.0001);
        }

        JsonObject projectile = delivery.getAsJsonObject("projectile");
        assertEquals(0, projectile.getAsJsonObject("perks").get("pierce").getAsInt());
        assertEquals(
                "openworld_rpg:spell_projectile/hunter_quickstep_arrow",
                projectile.getAsJsonObject("client_data")
                        .getAsJsonObject("composite_model")
                        .getAsJsonArray("models")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("fx")
                        .get("model_id").getAsString()
        );

        JsonObject cost = root.getAsJsonObject("cost");
        assertEquals(0.0, cost.get("exhaust").getAsDouble(), 0.0001);
        assertEquals(0, cost.get("durability").getAsInt());
        assertEquals(0.0, cost.getAsJsonObject("cooldown").get("duration").getAsDouble(), 0.0001);
    }
}
