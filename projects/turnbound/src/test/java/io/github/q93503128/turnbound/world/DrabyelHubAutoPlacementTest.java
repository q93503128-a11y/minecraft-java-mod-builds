package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrabyelHubAutoPlacementTest {
    @Test
    void sourcePlanCoversAllSixHubServicesWithoutChangingStaticCatalog() {
        assertEquals(Set.of("GREETER","TRAVEL","MARKET","BLACKSMITH","STORY","SUMMON"),
                DrabyelHubAutoPlacement.sourceRoles());
        assertTrue(DrabyelHubServiceCatalog.productionServices().isEmpty(),
                "live-world auto placement must not rewrite static source verification flags");
    }
}
