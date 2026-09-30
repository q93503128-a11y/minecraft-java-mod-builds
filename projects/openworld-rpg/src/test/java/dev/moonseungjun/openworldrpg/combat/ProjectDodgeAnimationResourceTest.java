package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectDodgeAnimationResourceTest {
    @Test
    void admittedRollVariantsMatchNineTickActionDuration() throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "assets/openworld_rpg/player_animations/dodge_roll.json"
                );
        assertNotNull(stream, "Dodge roll animation resource must be packaged.");

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals("1.8.0", root.get("format_version").getAsString());
        JsonObject animations = root.getAsJsonObject("animations");
        for (String id : List.of(
                "dodge_forward",
                "dodge_backward",
                "dodge_left",
                "dodge_right"
        )) {
            assertTrue(animations.has(id), "Missing dodge animation: " + id);
            assertEquals(
                    0.45,
                    animations.getAsJsonObject(id)
                            .get("animation_length")
                            .getAsDouble(),
                    0.0001
            );
        }
    }
}
