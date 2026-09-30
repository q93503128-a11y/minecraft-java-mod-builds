package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.HunterQuickstepVolleyRules;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuickstepVolleyRuntime;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HunterQuickstepVolleySpellResourceTest {
    @Test
    void resourceUsesThreeDeterministicPhysicalArrowsAndNeutralDonorCosts()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/hunter_quickstep_volley.json"
                );
        assertNotNull(stream);

        JsonObject root;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals("spell_power:physical_ranged", root.get("school").getAsString());
        assertEquals(
                "spell_engine:archery_release",
                root.getAsJsonObject("release")
                        .getAsJsonObject("animation")
                        .get("id").getAsString()
        );

        JsonObject delivery = root.getAsJsonObject("deliver").getAsJsonObject("projectile");
        assertEquals(
                HunterQuickstepVolleyRules.PROJECTILE_COUNT - 1,
                delivery.getAsJsonObject("launch_properties")
                        .get("extra_launch_count").getAsInt()
        );
        assertEquals(
                1,
                delivery.getAsJsonObject("launch_properties")
                        .get("extra_launch_delay").getAsInt()
        );
        assertEquals(3, delivery.getAsJsonArray("direction_offsets").size());
        assertEquals(
                -4.0,
                delivery.getAsJsonArray("direction_offsets")
                        .get(0).getAsJsonObject()
                        .get("yaw").getAsDouble(),
                0.0001
        );
        assertEquals(
                4.0,
                delivery.getAsJsonArray("direction_offsets")
                        .get(2).getAsJsonObject()
                        .get("yaw").getAsDouble(),
                0.0001
        );

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
        assertEquals(
                0.0,
                cost.getAsJsonObject("cooldown").get("duration").getAsDouble(),
                0.0001
        );

        assertEquals(10, HunterQuickstepVolleyRuntime.ACTION_TICKS);
        assertEquals(6, HunterQuickstepVolleyRuntime.DASH_STEPS);
        assertEquals(
                HunterQuickstepVolleyRules.WHOLE_ACTION_POISE_COEFFICIENT
                        / HunterQuickstepVolleyRules.PROJECTILE_COUNT,
                HunterQuickstepVolleyRuntime.PER_PROJECTILE_POISE_COEFFICIENT,
                0.0001
        );
    }
}
