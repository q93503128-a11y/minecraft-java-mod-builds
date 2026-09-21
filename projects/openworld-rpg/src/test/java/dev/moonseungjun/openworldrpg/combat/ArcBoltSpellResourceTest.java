package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ArcBoltSpellResourceTest {
    @Test
    void spellEngineResourceMatchesProjectAuthorityContract() throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream("data/openworld_rpg/spell/arc_bolt.json");
        assertNotNull(stream, "Arc Bolt Spell Engine resource must be packaged.");

        JsonObject root;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals("spell_power:arcane", root.get("school").getAsString());
        assertEquals(22.0, root.get("range").getAsDouble(), 0.0001);

        var cast = root.getAsJsonObject("active").getAsJsonObject("cast");
        assertEquals(1.0, cast.get("duration").getAsDouble(), 0.0001);

        var projectile = root.getAsJsonObject("deliver").getAsJsonObject("projectile");
        assertEquals(
                1.0,
                projectile.getAsJsonObject("launch_properties").get("velocity").getAsDouble(),
                0.0001
        );

        var action = root.getAsJsonArray("impacts")
                .get(0).getAsJsonObject()
                .getAsJsonObject("action");
        assertEquals("CUSTOM", action.get("type").getAsString());
        assertEquals(
                "openworld_rpg:project_impact",
                action.getAsJsonObject("custom").get("handler").getAsString()
        );
        assertEquals(
                "HARMFUL",
                action.getAsJsonObject("custom").get("intent").getAsString()
        );

        var cost = root.getAsJsonObject("cost");
        assertFalse(cost.get("batching").getAsBoolean());
        assertEquals(0.0, cost.get("exhaust").getAsDouble(), 0.0001);
        assertEquals(0, cost.get("durability").getAsInt());
        assertFalse(cost.has("item"));
        assertFalse(cost.has("effect_id"));

        var cooldown = cost.getAsJsonObject("cooldown");
        assertEquals(0.0, cooldown.get("attempt_duration").getAsDouble(), 0.0001);
        assertEquals(0.0, cooldown.get("duration").getAsDouble(), 0.0001);
        assertFalse(cooldown.has("group"));
        assertFalse(cooldown.get("hosting_item").getAsBoolean());

        assertTrue(root.getAsJsonArray("impacts").size() == 1);
    }
}
