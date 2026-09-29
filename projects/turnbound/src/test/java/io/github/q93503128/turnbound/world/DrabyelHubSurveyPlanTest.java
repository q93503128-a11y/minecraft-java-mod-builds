package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrabyelHubSurveyPlanTest {
    @Test
    void surveyPlanCoversEveryServiceRoleWithoutPromotingCoordinates() {
        var lines = DrabyelHubSurveyPlan.serviceSeedLines();
        assertEquals(6, lines.size());

        Set<String> roles = DrabyelHubServiceCatalog.hub().services().stream()
                .map(DrabyelHubServiceCatalog.Service::role)
                .collect(Collectors.toSet());
        assertEquals(Set.of("GREETER","TRAVEL","MARKET","BLACKSMITH","STORY","SUMMON"), roles);

        for (String role : roles) {
            assertFalse(DrabyelHubSurveyPlan.sourceHint(role).isBlank(), role);
        }
        assertTrue(DrabyelHubServiceCatalog.productionServices().isEmpty(),
                "survey hints must not promote runtime service coordinates");
    }
}
