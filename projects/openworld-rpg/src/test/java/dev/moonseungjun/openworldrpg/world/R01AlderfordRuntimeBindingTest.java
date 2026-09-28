package dev.moonseungjun.openworldrpg.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingLoader;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingLoader;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingLoader;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class R01AlderfordRuntimeBindingTest {
    @Test
    void bundledRuntimeContractCoversEveryServiceAndProperty() {
        var structures = R01StructureBindingLoader.loadBundled();
        var runtime = R01AlderfordRuntimeBindingLoader.loadBundled();

        assertEquals(structures.services().size(), runtime.services().size());
        assertEquals(
                structures.properties().size(),
                runtime.properties().size()
        );
        assertEquals(9, runtime.services().size());
        assertEquals(5, runtime.properties().size());

        var expectedServices = structures.services().stream()
                .map(value -> value.id())
                .collect(java.util.stream.Collectors.toSet());
        var actualServices = runtime.services().stream()
                .map(value -> value.serviceId())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedServices, actualServices);

        var expectedProperties = structures.properties().stream()
                .map(value -> value.id())
                .collect(java.util.stream.Collectors.toSet());
        var actualProperties = runtime.properties().stream()
                .map(value -> value.propertyId())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedProperties, actualProperties);
    }

    @Test
    void serviceSocketIdsRemainCompositionOwnedAndCannotUseTerrainCenters() {
        var structures = R01StructureBindingLoader.loadBundled();
        var runtime = R01AlderfordRuntimeBindingLoader.loadBundled();

        for (var serviceRuntime : runtime.services()) {
            var service = structures.service(
                    serviceRuntime.serviceId()
            ).orElseThrow();
            assertEquals(
                    service.interactionSocketId(),
                    serviceRuntime.socketAnchorId()
            );
            var structure = structures.structure(
                    service.structureId()
            ).orElseThrow();
            assertFalse(
                    structure.spatialAnchorId().equals(
                            serviceRuntime.socketAnchorId()
                    )
            );
        }
    }

    @Test
    void candidateShellCannotExposeLiveServicesOrPropertyPurchase() {
        var structures = R01StructureBindingLoader.loadBundled();
        var spatial = R01SpatialBindingLoader.loadBundled();
        var runtime = R01AlderfordRuntimeBindingLoader.loadBundled();

        assertFalse(runtime.productionReady(structures, spatial));

        for (var service : runtime.services()) {
            assertEquals("candidate", service.status());
            assertTrue(
                    runtime.productionService(
                            service.serviceId(),
                            structures,
                            spatial
                    ).isEmpty()
            );
        }
        for (var property : runtime.properties()) {
            assertEquals("candidate", property.status());
            assertTrue(
                    runtime.productionProperty(
                            property.propertyId(),
                            structures,
                            spatial
                    ).isEmpty()
            );
        }
    }

    @Test
    void propertyGeometryTargetsRemainDistinctAndUnauthoredUntilAcceptedComposition() {
        var runtime = R01AlderfordRuntimeBindingLoader.loadBundled();
        var spatial = R01SpatialBindingLoader.loadBundled();

        for (var property : runtime.properties()) {
            var ids = new HashSet<String>();
            ids.add(property.protectedVolumeId());
            ids.add(property.interiorVolumeId());
            ids.add(property.furnishingVolumeId());
            assertEquals(3, ids.size());

            assertTrue(spatial.volume(property.protectedVolumeId()).isEmpty());
            assertTrue(spatial.volume(property.interiorVolumeId()).isEmpty());
            assertTrue(spatial.volume(property.furnishingVolumeId()).isEmpty());

            /*
             * Door count/clearance shape belongs to the accepted exact house composition.
             * Candidate shells therefore do not invent one here.
             */
            assertTrue(property.clearanceVolumeIds().isEmpty());
        }
    }
}
