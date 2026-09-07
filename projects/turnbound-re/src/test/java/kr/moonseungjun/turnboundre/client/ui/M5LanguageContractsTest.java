package kr.moonseungjun.turnboundre.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        requireNonBlank(en, required);

        assertEquals("screen.turnbound_re.disabled.energy", BattleActionPresentation.disabledReasonKey("ENERGY"));
        assertEquals("screen.turnbound_re.disabled.targets", BattleActionPresentation.disabledReasonKey("TARGETS"));
        assertEquals("screen.turnbound_re.disabled.locked", BattleActionPresentation.disabledReasonKey("UNKNOWN"));
    }

    @Test
    void battleHudCopyCoversEveryAuthoredIntentEnumAndCoreStatus() {
        JsonObject en = read(EN);
        Set<String> required = Set.of(
                "hud.turnbound_re.state.now",
                "hud.turnbound_re.state.defeated",
                "hud.turnbound_re.intent.none",
                "hud.turnbound_re.intent.line",
                "hud.turnbound_re.intent.line_break",
                "hud.turnbound_re.intent.unknown",
                "hud.turnbound_re.status.detail.stacks_turns",
                "hud.turnbound_re.status.detail.stacks",
                "hud.turnbound_re.status.detail.turns"
        );
        requireNonBlank(en, required);

        for (String risk : List.of("NORMAL", "DANGEROUS", "ULTIMATE")) {
            String key = BattleHudPresentation.intentRiskKey(risk);
            assertTrue(en.has(key), "missing intent risk translation: " + key);
        }
        for (String type : List.of("ATTACK", "DEFEND", "BUFF", "DEBUFF", "SPECIAL")) {
            String key = BattleHudPresentation.intentTypeKey(type);
            assertTrue(en.has(key), "missing intent type translation: " + key);
        }
        for (String targeting : List.of("SINGLE", "ALL", "SELF", "RANDOM")) {
            String key = BattleHudPresentation.intentTargetingKey(targeting);
            assertTrue(en.has(key), "missing intent targeting translation: " + key);
        }

        for (String status : List.of(
                "guard", "exposed", "poise_guard", "burn", "slow", "atk_up",
                "def_down", "venom", "webbed", "evasion", "ward", "volatile")) {
            String key = BattleHudPresentation.statusNameKey("turnbound_re:" + status);
            assertTrue(!key.isBlank(), "core status is not mapped: " + status);
            assertTrue(en.has(key), "missing core status translation: " + key);
        }

        assertEquals("hud.turnbound_re.intent.unknown", BattleHudPresentation.intentRiskKey("UNKNOWN"));
        assertEquals("", BattleHudPresentation.statusNameKey("turnbound_re:future_status"));
    }

    private static void requireNonBlank(JsonObject language, Set<String> keys) {
        for (String key : keys) {
            assertTrue(language.has(key), "missing battle UI translation: " + key);
            assertTrue(!language.get(key).getAsString().isBlank(), "blank battle UI translation: " + key);
        }
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
