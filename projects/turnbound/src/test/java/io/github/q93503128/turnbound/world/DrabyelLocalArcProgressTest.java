package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DrabyelLocalArcProgressTest {
    @Test void twoOfThreeCompletesAndAvsalNeedsAnotherRegionalMilestone(){
        Set<String> flags=Set.of(DrabyelLocalArcProgress.ACCEPTED,DrabyelLocalArcProgress.CRATE,DrabyelLocalArcProgress.SCOUT);
        assertEquals(2,DrabyelLocalArcProgress.count(flags));
        assertTrue(DrabyelLocalArcProgress.complete(flags));
        assertFalse(DrabyelLocalArcProgress.regionalGateReady(Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID),flags));
        assertTrue(DrabyelLocalArcProgress.regionalGateReady(Set.of("CV_DRABYEL_NORTH"),flags));
    }
}
