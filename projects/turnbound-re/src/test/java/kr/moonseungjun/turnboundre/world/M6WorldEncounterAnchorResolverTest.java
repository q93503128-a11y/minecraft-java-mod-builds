package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.EncounterDefinition;
import kr.moonseungjun.turnboundre.data.RegionDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class M6WorldEncounterAnchorResolverTest {
    @Test
    void anchorTagRoundTripsOneLocatorAndRejectsConflicts() {
        String locator = "turnbound_re:region_01/overworld_patrol";
        assertEquals(locator, WorldEncounterAnchorResolver.locatorFromTags(
                Set.of("unrelated", WorldEncounterAnchorResolver.tagFor(locator))).orElseThrow());
        assertTrue(WorldEncounterAnchorResolver.locatorFromTags(Set.of(
                WorldEncounterAnchorResolver.tagFor(locator),
                WorldEncounterAnchorResolver.tagFor("turnbound_re:region_01/rift_elite"))).isEmpty());
    }

    @Test
    void resolverRequiresLocatorAndAuthoredDimensionToMatch() {
        EncounterDefinition encounter = new EncounterDefinition(
                "turnbound_re:test", 2,
                List.of(new EncounterDefinition.EnemySlot("turnbound_re:zombie", 5, 2)),
                "turnbound_re:test", "turnbound_re:scene", true);
        RegionDefinition region = new RegionDefinition(
                "turnbound_re:region_01", "REGION", "minecraft:overworld", List.of(),
                List.of(new RegionDefinition.EncounterAnchor(
                        "turnbound_re:region_01/test", encounter.id(),
                        "turnbound_re:region_01/test", true)));

        var resolved = WorldEncounterAnchorResolver.resolve(
                List.of(region), Map.of(encounter.id(), encounter),
                "turnbound_re:region_01/test", "minecraft:overworld").orElseThrow();
        assertEquals(region.id(), resolved.region().id());
        assertEquals(encounter.id(), resolved.encounter().id());
        assertTrue(WorldEncounterAnchorResolver.resolve(
                List.of(region), Map.of(encounter.id(), encounter),
                "turnbound_re:region_01/test", "minecraft:the_nether").isEmpty());
        assertTrue(WorldEncounterAnchorResolver.resolve(
                List.of(region), Map.of(encounter.id(), encounter),
                "turnbound_re:region_01/missing", "minecraft:overworld").isEmpty());
    }

    @Test
    void confirmDistanceHasExplicitSixBlockBoundary() {
        assertTrue(WorldEncounterAnchorResolver.withinConfirmRange(0.0D));
        assertTrue(WorldEncounterAnchorResolver.withinConfirmRange(36.0D));
        assertFalse(WorldEncounterAnchorResolver.withinConfirmRange(36.0001D));
        assertFalse(WorldEncounterAnchorResolver.withinConfirmRange(Double.NaN));
        assertFalse(WorldEncounterAnchorResolver.withinConfirmRange(-1.0D));
    }
}
