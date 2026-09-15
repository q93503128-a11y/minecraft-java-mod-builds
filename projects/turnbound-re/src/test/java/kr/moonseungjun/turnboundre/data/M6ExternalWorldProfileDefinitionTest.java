package kr.moonseungjun.turnboundre.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class M6ExternalWorldProfileDefinitionTest {
    @Test
    void bundleParserCarriesDataDrivenExternalWorldProfile() {
        String json = """
                {
                  "regions": [
                    {
                      "id": "turnbound_re:hub_01",
                      "kind": "HUB",
                      "dimension": "minecraft:overworld",
                      "exits": ["turnbound_re:region_01"],
                      "fastTravelAnchors": [
                        {
                          "id": "turnbound_re:hub_01/waypoint",
                          "locator": "turnbound_re:hub_01/waypoint",
                          "destinations": ["turnbound_re:region_01/waypoint"]
                        }
                      ]
                    },
                    {
                      "id": "turnbound_re:region_01",
                      "kind": "REGION",
                      "dimension": "minecraft:overworld",
                      "exits": ["turnbound_re:hub_01"],
                      "fastTravelAnchors": [
                        {
                          "id": "turnbound_re:region_01/waypoint",
                          "locator": "turnbound_re:region_01/waypoint",
                          "destinations": ["turnbound_re:hub_01/waypoint"]
                        }
                      ],
                      "resourceAnchors": [
                        {
                          "id": "turnbound_re:region_01/ore_outcrop",
                          "activity": "MINING",
                          "locator": "turnbound_re:region_01/ore_outcrop"
                        }
                      ]
                    }
                  ],
                  "externalWorldProfiles": [
                    {
                      "id": "turnbound_re:drehmal_test",
                      "sourceVersion": "test",
                      "dimension": "minecraft:overworld",
                      "anchors": [
                        {
                          "kind": "FAST_TRAVEL",
                          "locator": "turnbound_re:hub_01/waypoint",
                          "x": 502,
                          "y": 67,
                          "z": 1801,
                          "enabled": true
                        },
                        {
                          "kind": "RESOURCE",
                          "locator": "turnbound_re:region_01/ore_outcrop",
                          "x": 855,
                          "y": 65,
                          "z": 553,
                          "enabled": false,
                          "sourceNote": "candidate"
                        }
                      ]
                    }
                  ]
                }
                """;

        DefinitionBundleParser.Parsed parsed = DefinitionBundleParser.parse(
                Map.of("turnbound_re:external_world_test.json", json));
        ExternalWorldProfileDefinition profile = parsed.registry().externalWorldProfiles()
                .get("turnbound_re:drehmal_test");

        assertEquals("test", profile.sourceVersion());
        assertEquals(2, profile.anchors().size());
        assertFalse(profile.anchors().get(1).enabled());
        assertEquals("candidate", profile.anchors().get(1).sourceNote());
    }

    @Test
    void rejectsExternalWorldAnchorThatDoesNotResolveToRegionSemantics() {
        String json = """
                {
                  "regions": [
                    {
                      "id": "turnbound_re:hub_01",
                      "kind": "HUB",
                      "dimension": "minecraft:overworld"
                    }
                  ],
                  "externalWorldProfiles": [
                    {
                      "id": "turnbound_re:broken_profile",
                      "sourceVersion": "test",
                      "dimension": "minecraft:overworld",
                      "anchors": [
                        {
                          "kind": "ENCOUNTER",
                          "locator": "turnbound_re:missing/encounter",
                          "x": 1,
                          "y": 64,
                          "z": 1
                        }
                      ]
                    }
                  ]
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> DefinitionBundleParser.parse(
                Map.of("turnbound_re:broken_external_world_test.json", json)));
    }
}
