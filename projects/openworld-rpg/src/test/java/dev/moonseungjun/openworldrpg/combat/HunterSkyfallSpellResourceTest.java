package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.HunterSkyfallRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HunterSkyfallSpellResourceTest {
    @Test
    void authorityPublishesHunterUltimateWithoutDonorResourceCost() {
        var spec = ProjectSpellSpec.hunterSkyfall();
        assertEquals(
                ProjectSpellSpec.HUNTER_SKYFALL_ID,
                spec.id()
        );
        assertEquals(0.0, spec.manaCost(), 0.0001);
        assertEquals(0, spec.cooldownTicks());
        assertEquals(
                RootClass.HUNTER,
                ProjectSpellSpec.requiredRootClass(spec.id())
                        .orElseThrow()
        );
    }

    @Test
    void resourceUsesGroundAimAndReadableMeteorRepresentation()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/"
                                + "hunter_skyfall.json"
                );
        assertNotNull(stream);
        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader)
                    .getAsJsonObject();
        }

        assertEquals(
                HunterSkyfallRules.TARGET_RANGE_BLOCKS,
                root.get("range").getAsDouble(),
                0.0001
        );
        JsonObject cast = root
                .getAsJsonObject("active")
                .getAsJsonObject("cast");
        assertEquals(0.5, cast.get("duration").getAsDouble(), 0.0001);
        assertEquals(
                "spell_engine:archery_upwards_pull",
                cast.getAsJsonObject("animation")
                        .get("id").getAsString()
        );
        assertEquals(
                "spell_engine:archery_upwards_release",
                root.getAsJsonObject("release")
                        .getAsJsonObject("animation")
                        .get("id").getAsString()
        );

        JsonObject aim = root.getAsJsonObject("target")
                .getAsJsonObject("aim");
        assertFalse(aim.get("required").getAsBoolean());

        JsonObject meteor = root
                .getAsJsonObject("deliver")
                .getAsJsonObject("meteor");
        assertEquals(
                HunterSkyfallRules.VISUAL_LAUNCH_HEIGHT_BLOCKS,
                meteor.get("launch_height").getAsDouble(),
                0.0001
        );
        assertEquals(
                0.0,
                meteor.get("launch_radius").getAsDouble(),
                0.0001
        );
        JsonObject launch = meteor
                .getAsJsonObject("launch_properties");
        assertEquals(
                HunterSkyfallRules.VISUAL_PROJECTILE_VELOCITY,
                launch.get("velocity").getAsDouble(),
                0.0001
        );
        assertEquals(
                HunterSkyfallRules.VISUAL_EXTRA_LAUNCH_COUNT,
                launch.get("extra_launch_count").getAsInt()
        );
        assertEquals(
                HunterSkyfallRules.VISUAL_LAUNCH_DELAY_TICKS,
                launch.get("extra_launch_delay").getAsInt()
        );
        assertEquals(
                "openworld_rpg:spell_projectile/"
                        + "hunter_quickstep_arrow",
                meteor.getAsJsonObject("projectile")
                        .getAsJsonObject("client_data")
                        .getAsJsonObject("composite_model")
                        .getAsJsonArray("models")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("fx")
                        .get("model_id").getAsString()
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
                        .get("duration").getAsDouble(),
                0.0001
        );
    }
}
