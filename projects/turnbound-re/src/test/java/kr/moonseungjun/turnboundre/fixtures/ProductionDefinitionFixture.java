package kr.moonseungjun.turnboundre.fixtures;

import kr.moonseungjun.turnboundre.data.DefinitionBundleParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads the exact bundled production definitions used by the game, not test-only copies. */
public final class ProductionDefinitionFixture {
    private static final String ROOT = "data/turnbound_re/turnbound_definitions/";
    private static final String[] FILES = {
            "core_statuses.json", "vertical_actions.json", "vertical_characters.json", "vertical_encounters.json"
    };

    private ProductionDefinitionFixture() {}

    public static DefinitionBundleParser.Parsed load() throws IOException {
        Map<String, String> resources = new LinkedHashMap<>();
        ClassLoader loader = ProductionDefinitionFixture.class.getClassLoader();
        for (String file : FILES) {
            String classpath = ROOT + file;
            try (InputStream stream = loader.getResourceAsStream(classpath)) {
                if (stream == null) throw new IOException("missing production resource " + classpath);
                resources.put("turnbound_re:turnbound_definitions/" + file,
                        new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return DefinitionBundleParser.parse(resources);
    }
}
