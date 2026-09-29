package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrabyelHubAutoPlacementTest {
    @Test
    void sourcePlanCoversAllSixHubServicesWithoutChangingStaticCatalog() {
        Set<String> roles = DrabyelMapPlacementCatalog.plan().placements().stream()
                .map(placement -> DrabyelHubServiceCatalog.service(placement.serviceLocator()))
                .filter(java.util.Objects::nonNull)
                .map(DrabyelHubServiceCatalog.Service::role)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("GREETER","TRAVEL","MARKET","BLACKSMITH","STORY","SUMMON"), roles);
        assertTrue(DrabyelHubServiceCatalog.productionServices().isEmpty(),
                "live-world auto placement must not rewrite static source verification flags");
    }

    @Test
    void pinnedMapPlacementPlanIsCompleteAndFailClosed() {
        assertTrue(DrabyelMapPlacementCatalog.validate().isEmpty(),
                () -> String.join("; ", DrabyelMapPlacementCatalog.validate()));
        assertEquals("zachaa/DrehmalMap", DrabyelMapPlacementCatalog.plan().source().repository());
        assertEquals("72d82180cbe3f950f068cf2d8e8668c6b09d5c58",
                DrabyelMapPlacementCatalog.plan().source().commit());
        assertTrue(DrabyelMapPlacementCatalog.plan().source().paths().contains("data/all_entity_data.json"));

        for (var placement : DrabyelMapPlacementCatalog.plan().placements()) {
            assertTrue(placement.seeds().stream()
                    .anyMatch(seed -> !DrabyelMapPlacementCatalog.excluded(placement, seed.x(), seed.z())),
                    placement.serviceLocator() + " must keep at least one non-conflicting source seed");
            assertFalse(placement.exclusions().isEmpty(),
                    placement.serviceLocator() + " should explicitly protect nearby source content");
        }
    }
}
