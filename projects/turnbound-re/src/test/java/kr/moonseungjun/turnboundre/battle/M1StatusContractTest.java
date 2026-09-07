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
                  "id":"turnbound_re:burn",
                  "polarity":"NEGATIVE",
                  "durationUnit":"TURN",
                  "baseDuration":2,
                  "maxStacks":5,
                  "refreshRule":"REFRESH_DURATION",
                  "dispelTags":["DEBUFF","FIRE"],
                  "hooks":[
                    {"when":"TURN_END","effect":{"type":"DAMAGE_MAX_HP_PERCENT","value":0.03}}
                  ]
                }
                """;
        var decoded = StatusDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(error -> new AssertionError(error));
        assertEquals("turnbound_re:burn", decoded.id());
        assertEquals("NEGATIVE", decoded.polarity());
        assertEquals("TURN", decoded.durationUnit());
        assertEquals(2, decoded.baseDuration());
        assertEquals(5, decoded.maxStacks());
        assertEquals("REFRESH_DURATION", decoded.refreshRule());
        assertEquals(List.of("DEBUFF", "FIRE"), decoded.dispelTags());
        assertEquals(1, decoded.hooks().size());
        assertEquals("TURN_END", decoded.hooks().getFirst().when());
        assertEquals("DAMAGE_MAX_HP_PERCENT", decoded.hooks().getFirst().effect().type());
        assertEquals(0.03D, decoded.hooks().getFirst().effect().value());
    }

    @Test void invalidStatusDefinitionsFailValidationAndRegistryConstruction() {
        var invalid = new StatusDefinition(
                "bad", "UNKNOWN", "SECOND", 0, 0, "", List.of(""),
                List.of(new StatusDefinition.Hook("UNKNOWN", new StatusDefinition.Effect("UNKNOWN", Double.NaN))));
        var duplicateA = new StatusDefinition(
                "turnbound_re:duplicate", "NEGATIVE", "TURN", 1, 1, "REFRESH_DURATION", List.of(), List.of());
        var duplicateB = new StatusDefinition(
                "turnbound_re:duplicate", "NEGATIVE", "TURN", 1, 1, "REFRESH_DURATION", List.of(), List.of());
        var errors = DefinitionValidator.validateStatuses(List.of(invalid, duplicateA, duplicateB));
        assertTrue(errors.stream().anyMatch(it -> it.contains("duplicate status id")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("invalid status id")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("unknown polarity")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("unknown durationUnit")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("baseDuration")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("maxStacks")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("refreshRule")), errors.toString());
        assertThrows(IllegalArgumentException.class,
                () -> DefinitionRegistry.create(List.of(), List.of(), List.of(invalid)));
    }

    @Test void registryKeepsCanonicalSharedStatusesImmutable() {
        var statuses = List.of(
                status(StatusService.GUARD, "POSITIVE"),
                status(StatusService.EXPOSED, "NEGATIVE"),
                status(StatusService.POISE_GUARD, "POSITIVE"),
                status(StatusService.BURN, "NEGATIVE"),
                status(StatusService.SLOW, "NEGATIVE"),
                status(StatusService.ATK_UP, "POSITIVE"),
                status(StatusService.DEF_DOWN, "NEGATIVE")
        );
        var registry = DefinitionRegistry.create(List.of(), List.of(), statuses);
        assertEquals(7, registry.statuses().size());
        assertTrue(registry.statuses().keySet().containsAll(List.of(
                StatusService.GUARD, StatusService.EXPOSED, StatusService.POISE_GUARD,
                StatusService.BURN, StatusService.SLOW, StatusService.ATK_UP, StatusService.DEF_DOWN)));
        assertThrows(UnsupportedOperationException.class, () -> registry.statuses().clear());
    }

    private static StatusDefinition status(String id, String polarity) {
        return new StatusDefinition(id, polarity, "TURN", 1, 1, "REFRESH_DURATION", List.of(), List.of());
    }
}
