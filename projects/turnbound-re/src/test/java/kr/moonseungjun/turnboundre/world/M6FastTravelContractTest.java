package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.RegionDefinition;
import kr.moonseungjun.turnboundre.data.RegionDefinitionValidator;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6FastTravelContractTest {
    @Test
    void productionDefinitionsExposeTheTwoDiscoveryGatedTravelPoints() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();

        var hub = WorldFastTravelResolver.resolve(
                registry, WorldFastTravelPrototype.HUB_LOCATOR, FunctionalWorldSliceLayout.DIMENSION).orElseThrow();
        var region = WorldFastTravelResolver.resolve(
                registry, WorldFastTravelPrototype.REGION_LOCATOR, FunctionalWorldSliceLayout.DIMENSION).orElseThrow();

        assertEquals(List.of(WorldFastTravelPrototype.REGION_LOCATOR), hub.anchor().destinations());
        assertEquals(List.of(WorldFastTravelPrototype.HUB_LOCATOR), region.anchor().destinations());
        assertEquals(List.of(), WorldFastTravelPrototype.validate(registry));
        assertTrue(WorldFastTravelResolver.resolve(
                registry, WorldFastTravelPrototype.HUB_LOCATOR, "minecraft:the_nether").isEmpty());
    }

    @Test
    void travelTagsRejectAmbiguousMarkersAndUseTheSameSixBlockInteractionRange() {
        String hub = WorldFastTravelResolver.tagFor(WorldFastTravelPrototype.HUB_LOCATOR);
        String region = WorldFastTravelResolver.tagFor(WorldFastTravelPrototype.REGION_LOCATOR);

        assertEquals(WorldFastTravelPrototype.HUB_LOCATOR,
                WorldFastTravelResolver.locatorFromTags(Set.of(hub)).orElseThrow());
        assertTrue(WorldFastTravelResolver.locatorFromTags(Set.of(hub, region)).isEmpty());
        assertTrue(WorldFastTravelResolver.locatorFromTags(Set.of("turnbound_re:travel=")).isEmpty());
        assertTrue(WorldFastTravelResolver.withinUseRange(36.0D));
        assertFalse(WorldFastTravelResolver.withinUseRange(36.0001D));
    }

    @Test
    void worldAnchorPositionsAndPerPlayerDiscoveryRemainIndependent() {
        UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");
        FastTravelSavedData data = new FastTravelSavedData(Map.of(), Map.of());

        data.registerAnchor(WorldFastTravelPrototype.HUB_LOCATOR, FunctionalWorldSliceLayout.DIMENSION, new BlockPos(10, 65, 10));
        data.registerAnchor(WorldFastTravelPrototype.REGION_LOCATOR, FunctionalWorldSliceLayout.DIMENSION, new BlockPos(70, 65, 10));
        assertTrue(data.discover(alice, WorldFastTravelPrototype.HUB_LOCATOR));
        assertFalse(data.discover(alice, WorldFastTravelPrototype.HUB_LOCATOR));
        assertFalse(data.discovered(bob, WorldFastTravelPrototype.HUB_LOCATOR));
        assertEquals(Set.of(WorldFastTravelPrototype.HUB_LOCATOR), data.discovered(alice));

        data.registerAnchor(WorldFastTravelPrototype.HUB_LOCATOR, FunctionalWorldSliceLayout.DIMENSION, new BlockPos(20, 70, 20));
        assertEquals(new BlockPos(20, 70, 20), data.anchor(WorldFastTravelPrototype.HUB_LOCATOR).orElseThrow().blockPos());
        assertEquals(Set.of(WorldFastTravelPrototype.HUB_LOCATOR), data.discovered(alice));

        assertTrue(data.discover(alice, WorldFastTravelPrototype.REGION_LOCATOR));
        assertEquals(Set.of(WorldFastTravelPrototype.HUB_LOCATOR, WorldFastTravelPrototype.REGION_LOCATOR), data.discovered(alice));
    }

    @Test
    void validatorRejectsFastTravelThatDoesNotResolveToAnAuthoredDestination() {
        RegionDefinition hub = new RegionDefinition(
                "turnbound_re:hub_test",
                "HUB",
                "minecraft:overworld",
                List.of("turnbound_re:region_test"),
                List.of(),
                List.of(),
                List.of(new RegionDefinition.FastTravelAnchor(
                        "turnbound_re:hub_test/waypoint",
                        "turnbound_re:hub_test/waypoint",
                        List.of("turnbound_re:missing/waypoint"))));
        RegionDefinition region = new RegionDefinition(
                "turnbound_re:region_test",
                "REGION",
                "minecraft:overworld",
                List.of("turnbound_re:hub_test"),
                List.of(),
                List.of(),
                List.of(new RegionDefinition.FastTravelAnchor(
                        "turnbound_re:region_test/waypoint",
                        "turnbound_re:region_test/waypoint",
                        List.of("turnbound_re:hub_test/waypoint"))));

        var errors = RegionDefinitionValidator.validate(List.of(hub, region), Set.of());
        assertTrue(errors.stream().anyMatch(error -> error.contains("unresolved fast travel destination turnbound_re:missing/waypoint")));
    }
}
