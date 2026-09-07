package kr.moonseungjun.turnboundre.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5LanguageContractsTest {
    private static final String EN = "assets/turnbound_re/lang/en_us.json";
    private static final String KO = "assets/turnbound_re/lang/ko_kr.json";

    @Test
    void englishAndKoreanUiKeysStayInLockstep() {
        JsonObject en = read(EN);
        JsonObject ko = read(KO);
        assertEquals(en.keySet(), ko.keySet(), "en_us/ko_kr translation key drift");
    }

    @Test
    void commandInteractionCopyCoversServerDisabledReasonsAndTooltipFacts() {
        JsonObject en = read(EN);
        Set<String> required = Set.of(
                "screen.turnbound_re.disabled.energy",
                "screen.turnbound_re.disabled.targets",
                "screen.turnbound_re.disabled.locked",
                "screen.turnbound_re.tooltip.target",
                "screen.turnbound_re.tooltip.energy",
                "screen.turnbound_re.tooltip.hp_power",
                "screen.turnbound_re.tooltip.poise_power",
                "screen.turnbound_re.tooltip.damage_tag",
                "screen.turnbound_re.tooltip.no_direct_power",
                "screen.turnbound_re.selected_summary",
                "hud.turnbound_re.command_header",
                "hud.turnbound_re.action.need_energy",
                "hud.turnbound_re.action.no_target",
                "hud.turnbound_re.action.locked"
        );
        for (String key : required) {
            assertTrue(en.has(key), "missing battle UI translation: " + key);
            assertTrue(!en.get(key).getAsString().isBlank(), "blank battle UI translation: " + key);
        }

        assertEquals("screen.turnbound_re.disabled.energy", BattleActionPresentation.disabledReasonKey("ENERGY"));
        assertEquals("screen.turnbound_re.disabled.targets", BattleActionPresentation.disabledReasonKey("TARGETS"));
        assertEquals("screen.turnbound_re.disabled.locked", BattleActionPresentation.disabledReasonKey("UNKNOWN"));
    }

    private static JsonObject read(String path) {
        InputStream input = M5LanguageContractsTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(input, "missing test resource " + path);
        try (InputStream stream = input;
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception error) {
            throw new AssertionError("failed to read " + path, error);
        }
    }
}
