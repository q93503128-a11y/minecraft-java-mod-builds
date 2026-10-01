package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.MageFlameBurstRuntime;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FlameBurstSpellResourceTest {
    @Test
    void resourceKeepsGroundCloudPresentationNeutralToProjectAuthority()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/flame_burst.json"
                );
        assertNotNull(
                stream,
                "Flame Burst spell resource must be packaged."
        );

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader)
                    .getAsJsonObject();
        }

        assertEquals(
                MageFlameBurstRuntime.GROUND_TARGET_RANGE_BLOCKS,
                root.get("range").getAsDouble(),
                0.0001
        );
        assertEquals(
                "AIM",
                root.getAsJsonObject("target")
                        .get("type")
                        .getAsString()
        );
        assertEquals(
                "CLOUD",
                root.getAsJsonObject("deliver")
                        .get("type")
                        .getAsString()
        );

        JsonObject cloud = root.getAsJsonObject("deliver")
                .getAsJsonArray("clouds")
                .get(0)
                .getAsJsonObject();
        assertEquals(
                MageFlameBurstRuntime.RADIUS_BLOCKS,
                cloud.getAsJsonObject("volume")
                        .get("radius")
                        .getAsDouble(),
                0.0001
        );
        assertEquals(
                2.5,
                cloud.get("time_to_live_seconds")
                        .getAsDouble(),
                0.0001
        );
        assertEquals(
                MageFlameBurstRuntime.CLOUD_PULSE_INTERVAL_TICKS,
                cloud.get("impact_tick_interval").getAsLong()
        );

        JsonObject action = root.getAsJsonArray("impacts")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("action");
        assertEquals("CUSTOM", action.get("type").getAsString());
        assertEquals(
                "openworld_rpg:project_impact",
                action.getAsJsonObject("custom")
                        .get("handler")
                        .getAsString()
        );

        JsonObject cost = root.getAsJsonObject("cost");
        assertFalse(cost.get("batching").getAsBoolean());
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

        var spec = ProjectSpellSpec.flameBurst();
        assertEquals(30.0, spec.manaCost(), 0.0001);
        assertEquals(240, spec.cooldownTicks());
    }
}
