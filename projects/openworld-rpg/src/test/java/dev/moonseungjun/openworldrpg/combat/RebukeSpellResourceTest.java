package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RebukeSpellResourceTest {
    @Test
    void authorityContractMatchesClericCanon() {
        var spec = ProjectSpellSpec.rebuke();
        assertEquals(ProjectSpellSpec.REBUKE_ID, spec.id());
        assertEquals(24.0, spec.manaCost(), 0.0001);
        assertEquals(200, spec.cooldownTicks());
        assertEquals(1.80, spec.actionCoefficient(), 0.0001);
        assertEquals(1.60, spec.poiseCoefficient(), 0.0001);
        assertEquals(
                2.20,
                ProjectSpellSpec.REBUKE_EMPOWERED_POISE_COEFFICIENT,
                0.0001
        );
        assertEquals(
                RootClass.CLERIC,
                ProjectSpellSpec.requiredRootClass(
                        spec.id()
                ).orElseThrow()
        );
    }

    @Test
    void spellResourceIsServerResolvedAndCarriesShapedRelease() throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/rebuke.json"
                );
        assertNotNull(stream, "Rebuke spell resource must be packaged.");

        JsonObject root;
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals(4.5, root.get("range").getAsDouble(), 0.0001);
        assertEquals(
                "spell_engine:one_handed_area_release_air_wave",
                root.getAsJsonObject("release")
                        .getAsJsonObject("animation")
                        .get("id").getAsString()
        );

        var models = root.getAsJsonObject("release")
                .getAsJsonObject("visuals")
                .getAsJsonArray("models");
        assertEquals(1, models.size());
        assertEquals(
                "openworld_rpg:spell_effect/rebuke_burst",
                models.get(0).getAsJsonObject()
                        .get("model_id").getAsString()
        );

        assertEquals(
                "NONE",
                root.getAsJsonObject("target")
                        .get("type").getAsString()
        );
        assertEquals(
                "DIRECT",
                root.getAsJsonObject("deliver")
                        .get("type").getAsString()
        );
        assertEquals(0, root.getAsJsonArray("impacts").size());

        var cost = root.getAsJsonObject("cost");
        assertFalse(cost.get("batching").getAsBoolean());
        assertEquals(
                0.0,
                cost.getAsJsonObject("cooldown")
                        .get("duration").getAsDouble(),
                0.0001
        );
    }
}
