package dev.moonseungjun.openworldrpg.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingLoader;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingData;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class R01StructureBindingDataTest {
    @Test
    void bundledAlderfordBindingsStayGatedButBindEveryRequiredShell() {
        var data = R01StructureBindingLoader.loadBundled();

        assertEquals(1, data.schemaVersion());
        assertEquals(
                "openworld_rpg:r01/alderford_structure_bindings",
                data.id()
        );
        assertEquals("AzariNEW4252026", data.mapBuild());
        assertEquals(15, data.structures().size());
        assertEquals(9, data.services().size());
        assertEquals(5, data.properties().size());
        assertFalse(data.productionReady());

        assertTrue(data.structures().stream().allMatch(structure ->
                "candidate".equals(structure.status())
                        && "visual_review_gated".equals(structure.composition().state())
                        && "authored_fixed".equals(structure.composition().selectionMode())
                        && structure.composition().exactPrefabId() == null
        ));
        assertTrue(data.services().stream().allMatch(service ->
                data.productionService(service.id()).isEmpty()
        ));
        assertTrue(data.properties().stream().allMatch(property ->
                data.productionProperty(property.id()).isEmpty()
        ));
    }

    @Test
    void fixedArchitectureFamiliesMatchPassEightWithoutInventingPrefabModules() {
        var data = R01StructureBindingLoader.loadBundled();

        var forge = data.structure(
                "openworld_rpg:r01/alderford/holt_forge"
        ).orElseThrow();
        assertEquals(
                R01StructureBindingData.MEDIEVAL_VILLAGE_FAMILY,
                forge.architectureFamilyId()
        );
        assertEquals(
                Set.of(
                        R01StructureBindingData.FANTASY_PROPS_FAMILY,
                        R01StructureBindingData.KAYKIT_RPG_TOOLS_FAMILY
                ),
                Set.copyOf(forge.propFamilyIds())
        );
        assertNull(forge.composition().exactPrefabId());

        var marketHouse = data.structure(
                "openworld_rpg:r01/alderford/housing/market_house"
        ).orElseThrow();
        assertEquals("housing", marketHouse.kind());
        assertEquals(
                R01StructureBindingData.MEDIEVAL_VILLAGE_FAMILY,
                marketHouse.architectureFamilyId()
        );
        assertNull(marketHouse.composition().exactPrefabId());

        var board = data.structure(
                "openworld_rpg:r01/alderford/route_board"
        ).orElseThrow();
        assertEquals("board", board.kind());
        assertNull(board.architectureFamilyId());
        assertEquals(
                Set.of(R01StructureBindingData.FANTASY_PROPS_FAMILY),
                Set.copyOf(board.propFamilyIds())
        );
    }

    @Test
    void serviceInteractionSocketsAreSeparatedFromTerrainShellAnchors() {
        var data = R01StructureBindingLoader.loadBundled();

        var kettle = data.structure(
                "openworld_rpg:r01/alderford/copper_kettle"
        ).orElseThrow();
        var service = data.service(
                "openworld_rpg:service/alderford/copper_kettle"
        ).orElseThrow();

        assertEquals(
                "openworld_rpg:r01/alderford/copper_kettle_candidate",
                kettle.spatialAnchorId()
        );
        assertEquals(kettle.id(), service.structureId());
        assertEquals(
                "openworld_rpg:r01/socket/alderford/copper_kettle/service",
                service.interactionSocketId()
        );
        assertFalse(
                service.interactionSocketId().equals(kettle.spatialAnchorId()),
                "A service socket must not silently reuse the terrain shell center."
        );

        var gate = data.structure(
                "openworld_rpg:r01/alderford/gate_watch"
        ).orElseThrow();
        var shrine = data.structure(
                "openworld_rpg:r01/alderford/gate_shrine"
        ).orElseThrow();
        assertEquals(gate.spatialAnchorId(), shrine.spatialAnchorId());
        assertFalse(gate.composition().id().equals(shrine.composition().id()));
    }

    @Test
    void housingShellIdentityMatchesClosedR01PropertyCanon() {
        var data = R01StructureBindingLoader.loadBundled();

        var smallProperties = data.properties().stream()
                .filter(property -> "small_cottage".equals(property.tier()))
                .toList();
        assertEquals(4, smallProperties.size());
        assertTrue(smallProperties.stream().allMatch(property ->
                property.purchasePrice() == 2400
                        && property.storageCapacity() == 54
                        && Math.abs(property.saleCreditRate() - 0.80) < 0.000001
        ));

        Set<String> smallNames = smallProperties.stream()
                .map(R01StructureBindingData.PropertyBinding::displayName)
                .collect(Collectors.toSet());
        assertEquals(
                Set.of(
                        "Gate Cottage",
                        "Paddock Cottage",
                        "Riverside Cottage",
                        "Quarry-Road Cottage"
                ),
                smallNames
        );

        var marketHouse = data.property(
                "openworld_rpg:property/alderford/market_house"
        ).orElseThrow();
        assertEquals("Market House", marketHouse.displayName());
        assertEquals("town_house", marketHouse.tier());
        assertEquals(9000, marketHouse.purchasePrice());
        assertEquals(72, marketHouse.storageCapacity());
        assertEquals(0.80, marketHouse.saleCreditRate(), 0.000001);
    }

    @Test
    void everyStructureResolvesToTheCurrentCandidateSpatialDatasetWithoutProductionLeak() {
        var structures = R01StructureBindingLoader.loadBundled();
        var spatial = R01SpatialBindingLoader.loadBundled();

        for (var structure : structures.structures()) {
            var anchor = spatial.anchor(structure.spatialAnchorId()).orElseThrow();
            assertEquals("candidate", anchor.status());
            assertTrue(structures.productionStructure(structure.id()).isEmpty());
            assertTrue(spatial.productionAnchor(anchor.id()).isEmpty());
        }
    }
}
