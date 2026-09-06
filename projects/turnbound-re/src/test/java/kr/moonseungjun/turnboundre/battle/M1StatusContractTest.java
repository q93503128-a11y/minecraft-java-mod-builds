package kr.moonseungjun.turnboundre.battle;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionValidator;
import kr.moonseungjun.turnboundre.data.StatusDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M1StatusContractTest {
    @Test void canonicalSharedStatusDefinitionShapeRoundTripsThroughCodec() {
        String json = """
                {
                  "id":"BURN",
                  "polarity":"NEGATIVE",
                  "durationUnit":"TURN",
                  "maxStacks":5,
                  "refreshRule":"REFRESH",
                  "dispelTags":["DEBUFF"],
                  "hooks":["TURN_END"]
                }
                """;
        var decoded = StatusDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(error -> new AssertionError(error));
        assertEquals("BURN", decoded.id());
        assertEquals("NEGATIVE", decoded.polarity());
        assertEquals("TURN", decoded.durationUnit());
        assertEquals(5, decoded.maxStacks());
        assertEquals(List.of("DEBUFF"), decoded.dispelTags());
        assertEquals(List.of("TURN_END"), decoded.hooks());
    }

    @Test void invalidStatusDefinitionsFailValidationAndRegistryConstruction() {
        var invalid = new StatusDefinition("bad", "UNKNOWN", "SECOND", 0, "", List.of(""), List.of(""));
        var duplicate = new StatusDefinition("bad", "NEGATIVE", "TURN", 1, "REFRESH", List.of(), List.of());
        var errors = DefinitionValidator.validateStatuses(List.of(invalid, duplicate));
        assertTrue(errors.stream().anyMatch(it -> it.contains("duplicate status id")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("unknown polarity")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("unknown durationUnit")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("maxStacks")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("refreshRule")), errors.toString());
        assertThrows(IllegalArgumentException.class,
                () -> DefinitionRegistry.create(List.of(), List.of(), List.of(invalid)));
    }

    @Test void registryKeepsCanonicalSharedStatusesImmutable() {
        var statuses = List.of(
                status("GUARD", "POSITIVE"),
                status("EXPOSED", "NEGATIVE"),
                status("POISE_GUARD", "POSITIVE"),
                status("BURN", "NEGATIVE"),
                status("SLOW", "NEGATIVE"),
                status("ATK_UP", "POSITIVE"),
                status("DEF_DOWN", "NEGATIVE")
        );
        var registry = DefinitionRegistry.create(List.of(), List.of(), statuses);
        assertEquals(7, registry.statuses().size());
        assertTrue(registry.statuses().keySet().containsAll(
                List.of("GUARD", "EXPOSED", "POISE_GUARD", "BURN", "SLOW", "ATK_UP", "DEF_DOWN")));
        assertThrows(UnsupportedOperationException.class, () -> registry.statuses().clear());
    }

    private static StatusDefinition status(String id, String polarity) {
        return new StatusDefinition(id, polarity, "TURN", 1, "REFRESH", List.of(), List.of());
    }
}
