package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.RegionDefinition;
import kr.moonseungjun.turnboundre.data.RegionDefinitionValidator;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6WorldResourceAnchorResolverTest {
    @Test
    void productionRegionAuthorsOneHotspotForEachCoreGatheringActivity() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        RegionDefinition region = registry.regions().get("turnbound_re:region_01");
        assertEquals(3, region.resourceAnchors().size());
        assertEquals(Set.of("MINING", "FARMING", "FISHING"), region.resourceAnchors().stream()
                .map(RegionDefinition.ResourceAnchor::activity)
                .collect(Collectors.toSet()));
        for (RegionDefinition.ResourceAnchor anchor : region.resourceAnchors()) {
            var resolved = WorldResourceAnchorResolver.resolve(
                    registry, anchor.locator(), "minecraft:overworld").orElseThrow();
            assertEquals(anchor, resolved.anchor());
            assertEquals(region.id(), resolved.region().id());
        }
    }

    @Test
    void resourceTagRoundTripsExactlyOneLocatorAndRejectsConflicts() {
        String locator = "turnbound_re:region_01/ore_outcrop";
        assertEquals(locator, WorldResourceAnchorResolver.locatorFromTags(
                Set.of("unrelated", WorldResourceAnchorResolver.tagFor(locator))).orElseThrow());
        assertTrue(WorldResourceAnchorResolver.locatorFromTags(Set.of(
                WorldResourceAnchorResolver.tagFor(locator),
                WorldResourceAnchorResolver.tagFor("turnbound_re:region_01/river_pool"))).isEmpty());
    }

    @Test
    void resolverRequiresAuthoredLocatorAndDimensionButNeverInventsLoot() {
        RegionDefinition.ResourceAnchor resource = new RegionDefinition.ResourceAnchor(
                "turnbound_re:region_01/ore_outcrop", "MINING", "turnbound_re:region_01/ore_outcrop");
        RegionDefinition region = new RegionDefinition(
                "turnbound_re:region_01", "REGION", "minecraft:overworld", List.of(), List.of(), List.of(resource));

        var resolved = WorldResourceAnchorResolver.resolve(
                List.of(region), resource.locator(), "minecraft:overworld").orElseThrow();
        assertEquals(region.id(), resolved.region().id());
        assertEquals("MINING", resolved.anchor().activity());
        assertTrue(WorldResourceAnchorResolver.resolve(
                List.of(region), resource.locator(), "minecraft:the_nether").isEmpty());
        assertTrue(WorldResourceAnchorResolver.resolve(
                List.of(region), "turnbound_re:region_01/missing", "minecraft:overworld").isEmpty());

        assertEquals(Set.of("id", "activity", "locator"), java.util.Arrays.stream(RegionDefinition.ResourceAnchor.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).collect(Collectors.toSet()));
    }

    @Test
    void validatorRejectsUnknownActivityAndLocatorCollisionsAcrossAnchorFamilies() {
        RegionDefinition region = new RegionDefinition(
                "turnbound_re:region_01",
                "REGION",
                "minecraft:overworld",
                List.of(),
                List.of(new RegionDefinition.EncounterAnchor(
                        "turnbound_re:region_01/fight",
                        "turnbound_re:test_encounter",
                        "turnbound_re:region_01/shared",
                        true)),
                List.of(new RegionDefinition.ResourceAnchor(
                        "turnbound_re:region_01/bad_resource",
                        "CRAFTING",
                        "turnbound_re:region_01/shared")));

        List<String> errors = RegionDefinitionValidator.validate(
                List.of(region), Set.of("turnbound_re:test_encounter"));

        assertTrue(errors.stream().anyMatch(error -> error.contains("unknown resource activity CRAFTING")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("duplicate authored locator")));
    }

    @Test
    void confirmDistanceUsesSameExplicitSixBlockServerBoundary() {
        assertTrue(WorldResourceAnchorResolver.withinConfirmRange(0.0D));
        assertTrue(WorldResourceAnchorResolver.withinConfirmRange(36.0D));
        assertFalse(WorldResourceAnchorResolver.withinConfirmRange(36.0001D));
        assertFalse(WorldResourceAnchorResolver.withinConfirmRange(Double.NaN));
        assertFalse(WorldResourceAnchorResolver.withinConfirmRange(-1.0D));
    }
}
