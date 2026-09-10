package kr.moonseungjun.turnboundre.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class M6RegionDefinitionTest {
    @Test
    void validRegionGraphAndAnchorReferences() {
        RegionDefinition hub = new RegionDefinition(
                "turnbound_re:hub_01",
                "HUB",
                "minecraft:overworld",
                List.of("turnbound_re:region_01"),
                List.of());
        RegionDefinition region = new RegionDefinition(
                "turnbound_re:region_01",
                "REGION",
                "minecraft:overworld",
                List.of("turnbound_re:hub_01"),
                List.of(new RegionDefinition.EncounterAnchor(
                        "turnbound_re:region_01/overworld_patrol",
                        "turnbound_re:debug_overworld_patrol",
                        "turnbound_re:region_01/overworld_patrol",
                        true)));

        assertTrue(RegionDefinitionValidator.validate(
                List.of(hub, region), Set.of("turnbound_re:debug_overworld_patrol")).isEmpty());
    }

    @Test
    void rejectsBrokenRegionGraphAndAnchorReferences() {
        RegionDefinition broken = new RegionDefinition(
                "turnbound_re:region_01",
                "WRONG_KIND",
                "minecraft:overworld",
                List.of("turnbound_re:region_01", "turnbound_re:missing_region"),
                List.of(new RegionDefinition.EncounterAnchor(
                        "turnbound_re:region_01/broken",
                        "turnbound_re:missing_encounter",
                        "turnbound_re:region_01/broken",
                        true)));

        List<String> errors = RegionDefinitionValidator.validate(List.of(broken), Set.of());
        assertTrue(errors.stream().anyMatch(error -> error.contains("unknown region kind")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("cannot exit to itself")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("unresolved exit region")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("unresolved encounter")));
    }

    @Test
    void bundleParserCarriesRegionDefinitions() {
        String json = """
                {
                  "regions": [
                    {
                      "id": "turnbound_re:hub_01",
                      "kind": "HUB",
                      "dimension": "minecraft:overworld",
                      "exits": [],
                      "encounterAnchors": []
                    }
                  ]
                }
                """;

        DefinitionBundleParser.Parsed parsed = DefinitionBundleParser.parse(Map.of("turnbound_re:test_region.json", json));
        assertEquals(1, parsed.registry().regions().size());
        assertEquals("HUB", parsed.registry().regions().get("turnbound_re:hub_01").kind());
    }
}
