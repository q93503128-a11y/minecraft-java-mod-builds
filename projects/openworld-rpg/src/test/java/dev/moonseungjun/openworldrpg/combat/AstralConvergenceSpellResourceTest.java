package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.MageAstralConvergenceRuntime;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AstralConvergenceSpellResourceTest {
    @Test
    void resourcePublishesPresentationWithoutDonorCombatAuthority()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/astral_convergence.json"
                );
        assertNotNull(
                stream,
                "Astral Convergence spell resource must be packaged."
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
                MageAstralConvergenceRuntime.RADIUS_BLOCKS,
                root.get("range").getAsDouble(),
                0.0001
        );
        assertEquals(
                1.0,
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

        JsonObject cost = root.getAsJsonObject("cost");
        assertFalse(cost.get("batching").getAsBoolean());
        assertEquals(0.0, cost.get("exhaust").getAsDouble(), 0.0001);
        assertEquals(0, cost.get("durability").getAsInt());
        assertEquals(
                0.0,
                cost.getAsJsonObject("cooldown")
                        .get("duration").getAsDouble(),
                0.0001
        );

        var spec = ProjectSpellSpec.astralConvergence();
        assertEquals(0.0, spec.manaCost(), 0.0001);
        assertEquals(0, spec.cooldownTicks());
        assertEquals(
                RootClass.MAGE,
                ProjectSpellSpec.requiredRootClass(spec.id())
                        .orElseThrow()
        );
    }
}
