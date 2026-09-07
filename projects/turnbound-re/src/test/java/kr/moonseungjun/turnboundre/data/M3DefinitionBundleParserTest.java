package kr.moonseungjun.turnboundre.data;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class M3DefinitionBundleParserTest {
    private static final String BURN_BUNDLE = """
            {
              "statuses": [
                {
                  "id": "turnbound_re:burn",
                  "polarity": "NEGATIVE",
                  "durationUnit": "TURN",
                  "baseDuration": 2,
                  "maxStacks": 3,
                  "refreshRule": "REFRESH_DURATION",
                  "dispelTags": ["DEBUFF", "FIRE"],
                  "hooks": [
                    {"when":"TURN_END","effect":{"type":"DAMAGE_MAX_HP_PERCENT","value":0.03}}
                  ]
                }
              ]
            }
            """;

    @Test
    void mergeOrderAndDefinitionHashAreDeterministicAcrossMapIterationOrder() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("turnbound_re:turnbound_definitions/z.json", "{}");
        first.put("turnbound_re:turnbound_definitions/a.json", BURN_BUNDLE);
        Map<String, String> second = new LinkedHashMap<>();
        second.put("turnbound_re:turnbound_definitions/a.json", BURN_BUNDLE);
        second.put("turnbound_re:turnbound_definitions/z.json", "{}");

        var a = DefinitionBundleParser.parse(first);
        var b = DefinitionBundleParser.parse(second);

        assertEquals(a.hash(), b.hash());
        assertEquals(64, a.hash().length());
        assertEquals(a.resourceIds(), b.resourceIds());
        assertEquals("turnbound_re:turnbound_definitions/a.json", a.resourceIds().getFirst());
        assertTrue(a.registry().statuses().containsKey("turnbound_re:burn"));
    }

    @Test
    void invalidBundleCannotReplacePreviouslyInstalledRepositorySnapshot() {
        DefinitionRepository repository = new DefinitionRepository();
        var valid = DefinitionBundleParser.parse(Map.of("turnbound_re:turnbound_definitions/burn.json", BURN_BUNDLE));
        var installed = repository.install(valid.registry(), valid.hash());

        String duplicate = """
                {"statuses":[
                  {"id":"turnbound_re:dup","polarity":"NEGATIVE","durationUnit":"TURN","baseDuration":1,"maxStacks":1,"refreshRule":"REFRESH_DURATION","dispelTags":[],"hooks":[]},
                  {"id":"turnbound_re:dup","polarity":"NEGATIVE","durationUnit":"TURN","baseDuration":1,"maxStacks":1,"refreshRule":"REFRESH_DURATION","dispelTags":[],"hooks":[]}
                ]}
                """;
        assertThrows(IllegalArgumentException.class,
                () -> DefinitionBundleParser.parse(Map.of("turnbound_re:turnbound_definitions/bad.json", duplicate)));

        assertSame(installed, repository.snapshot());
        assertEquals(valid.hash(), repository.snapshot().hash());
        assertEquals(1L, repository.snapshot().generation());
    }
}
