package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterPinningShotRuntime;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HunterPinningShotSpellResourceTest {
    @Test
    void authorityMatchesLockedHunterCanon() {
        var spec = ProjectSpellSpec.hunterPinningShot();

        assertEquals(
                ProjectSpellSpec.HUNTER_PINNING_SHOT_ID,
                spec.id()
        );
        assertEquals(18.0, spec.manaCost(), 0.0001);
        assertEquals(160, spec.cooldownTicks());
        assertEquals(1.55, spec.actionCoefficient(), 0.0001);
        assertEquals(1.00, spec.poiseCoefficient(), 0.0001);
        assertEquals(
                1.50,
                ProjectSpellSpec
                        .HUNTER_PINNING_EMPOWERED_POISE_COEFFICIENT,
                0.0001
        );
        assertEquals(
                RootClass.HUNTER,
                ProjectSpellSpec.requiredRootClass(spec.id())
                        .orElseThrow()
        );
        assertEquals(10, HunterPinningShotRuntime.ACTION_TICKS);
    }

    @Test
    void resourceUsesSinglePhysicalArrowAndNeutralDonorCosts()
            throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "data/openworld_rpg/spell/"
                                + "hunter_pinning_shot.json"
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
                "spell_power:physical_ranged",
                root.get("school").getAsString()
        );
        assertEquals(
                24.0,
                root.get("range").getAsDouble(),
                0.0001
        );
        assertEquals(
                "spell_engine:archery_release",
                root.getAsJsonObject("release")
                        .getAsJsonObject("animation")
                        .get("id").getAsString()
        );

        JsonObject delivery = root
                .getAsJsonObject("deliver")
                .getAsJsonObject("projectile");
        assertEquals(
                2.0,
                delivery.getAsJsonObject("launch_properties")
                        .get("velocity").getAsDouble(),
                0.0001
        );
        JsonObject projectile =
                delivery.getAsJsonObject("projectile");
        assertEquals(
                0,
                projectile.getAsJsonObject("perks")
                        .get("pierce").getAsInt()
        );
        assertEquals(
                "openworld_rpg:spell_projectile/"
                        + "hunter_pinning_arrow",
                projectile.getAsJsonObject("client_data")
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
