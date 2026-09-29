package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectClassSkillLoadout;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectSpellLocalizationTest {
    @Test
    void everyPublishedProjectSkillHasEnglishAndKoreanPlayerText()
            throws Exception {
        Set<String> published = new LinkedHashSet<>();
        for (RootClass rootClass : RootClass.values()) {
            ProjectClassSkillLoadout
                    .implementedSlots(rootClass)
                    .forEach(slot -> published.add(slot.spellId()));
        }

        JsonObject english = language("en_us");
        JsonObject korean = language("ko_kr");
        for (String spellId : published) {
            String path = spellId.substring(
                    spellId.indexOf(':') + 1
            );
            String base = "spell.openworld_rpg." + path;
            assertText(english, base + ".name");
            assertText(english, base + ".description");
            assertText(korean, base + ".name");
            assertText(korean, base + ".description");
        }
    }

    private JsonObject language(String id) throws Exception {
        var stream = getClass().getClassLoader()
                .getResourceAsStream(
                        "assets/openworld_rpg/lang/"
                                + id
                                + ".json"
                );
        assertTrue(stream != null, id + " must be packaged.");
        try (var reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
        )) {
            return JsonParser.parseReader(reader)
                    .getAsJsonObject();
        }
    }

    private static void assertText(
            JsonObject language,
            String key
    ) {
        assertTrue(
                language.has(key)
                        && !language.get(key)
                                .getAsString()
                                .isBlank(),
                "Missing player-facing text: " + key
        );
    }
}
