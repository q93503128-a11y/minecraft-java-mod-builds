package io.github.q93503128.turnbound.presentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreHeroRoleAssetTest {
    @Test
    void authoredHeroModelsContainTheirRoleBearingProps() throws IOException {
        Map<String, String[]> expected = Map.of(
                "kyren", new String[]{"weapon_r","scabbard"},
                "lumea", new String[]{"clock_core","clock_rotor_1"},
                "bram", new String[]{"shield","hammer_grip"},
                "elysia", new String[]{"staff","med_bag"},
                "lynette", new String[]{"weapon_r","sight"},
                "morwen", new String[]{"paper_1","weapon_r"},
                "marion", new String[]{"contract_totem","totem_case"},
                "raze", new String[]{"weapon_r","axe_edge"});

        for (var entry : expected.entrySet()) {
            String json = resource("assets/turnbound/geckolib/models/entity/hero/" + entry.getKey() + ".geo.json");
            for (String bone : entry.getValue()) {
                assertTrue(json.contains("\"name\":\"" + bone + "\"")
                                || json.contains("\"name\": \"" + bone + "\""),
                        entry.getKey() + " must retain role prop bone " + bone);
            }
        }

        String toto = resource("assets/turnbound/geckolib/models/entity/hero/toto.geo.json");
        assertTrue(toto.contains("light_core"));
        assertTrue(toto.contains("forehead_seal"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = CoreHeroRoleAssetTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing resource " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
