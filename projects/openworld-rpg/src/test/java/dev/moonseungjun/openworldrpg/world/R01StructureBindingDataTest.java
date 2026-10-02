package dev.moonseungjun.openworldrpg.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        Set<String> terrainAnchorIds = data.structures().stream()
                .map(R01StructureBindingData.StructureBinding::spatialAnchorId)
                .collect(Collectors.toSet());
        assertTrue(data.services().stream().noneMatch(service ->
                terrainAnchorIds.contains(service.interactionSocketId())
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

    @Test
    void productionCompositionSourceIsARealPromotionPathButCandidateSourceCannotLeakIt() {
        var bundled = R01StructureBindingLoader.loadBundled();

        var structures = bundled.structures().stream()
                .map(value -> new R01StructureBindingData.StructureBinding(
                        value.id(),
                        "production",
                        value.kind(),
                        value.spatialAnchorId(),
                        value.architectureFamilyId(),
                        value.propFamilyIds(),
                        new R01StructureBindingData.CompositionBinding(
                                value.composition().id(),
                                "accepted",
                                value.composition().selectionMode(),
                                value.composition().id() + "/accepted_prefab"
                        ),
                        value.role()
                ))
                .toList();
        var services = bundled.services().stream()
                .map(value -> new R01StructureBindingData.ServiceBinding(
                        value.id(),
                        "production",
                        value.structureId(),
                        value.interactionSocketId(),
                        value.role()
                ))
                .toList();
        var properties = bundled.properties().stream()
                .map(value -> new R01StructureBindingData.PropertyBinding(
                        value.id(),
                        "production",
                        value.displayName(),
                        value.structureId(),
                        value.tier(),
                        value.purchasePrice(),
                        value.saleCreditRate(),
                        value.storageCapacity()
                ))
                .toList();

        var promoted = new R01StructureBindingData(
                bundled.schemaVersion(),
                bundled.id(),
                bundled.mapBuild(),
                R01StructureBindingData.PRODUCTION_SOURCE_STATUS,
                structures,
                services,
                properties
        );
        R01StructureBindingData.validate(promoted);

        assertTrue(promoted.productionReady());
        assertTrue(promoted.productionStructure(
                "openworld_rpg:r01/alderford/copper_kettle"
        ).isPresent());
        assertTrue(promoted.productionService(
                "openworld_rpg:service/alderford/copper_kettle"
        ).isPresent());
        assertTrue(promoted.productionProperty(
                "openworld_rpg:property/alderford/gate_cottage"
        ).isPresent());

        var leaked = new R01StructureBindingData(
                bundled.schemaVersion(),
                bundled.id(),
                bundled.mapBuild(),
                R01StructureBindingData.CANDIDATE_SOURCE_STATUS,
                structures,
                services,
                properties
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> R01StructureBindingData.validate(leaked)
        );
        assertTrue(leaked.productionStructure(
                "openworld_rpg:r01/alderford/copper_kettle"
        ).isEmpty());
    }

}
