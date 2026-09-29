package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DrabyelOpeningTutorialTest {
    @Test
    void greeterIsTheOnlyPositionalTargetBeforeFirstConversation() {
        assertTrue(DrabyelOpeningTutorial.shouldTargetGreeter(Set.of()));
        assertFalse(DrabyelOpeningTutorial.shouldTargetGreeter(
                Set.of(DrabyelOpeningTutorial.GREETER_FLAG)));
    }

    @Test
    void patrolStartsOnlyAfterGreeterAndPartyCheck() {
        assertFalse(DrabyelOpeningTutorial.shouldSendOut(Set.of(), Set.of()));
        assertFalse(DrabyelOpeningTutorial.shouldSendOut(
                Set.of(DrabyelOpeningTutorial.GREETER_FLAG), Set.of()));
        assertTrue(DrabyelOpeningTutorial.shouldSendOut(
                Set.of(DrabyelOpeningTutorial.GREETER_FLAG, DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                Set.of()));
        assertFalse(DrabyelOpeningTutorial.shouldSendOut(
                Set.of(DrabyelOpeningTutorial.GREETER_FLAG, DrehmalContextualOnboarding.HUB_MENU_VIEWED),
                Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID)));
    }

    @Test
    void clearedPatrolPointsBackToTownOnlyWhileOutside() {
        assertTrue(DrabyelOpeningTutorial.shouldReturnToHub(
                false, Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID)));
        assertFalse(DrabyelOpeningTutorial.shouldReturnToHub(
                true, Set.of(DrabyelOpeningTutorial.ENCOUNTER_ID)));
    }
}
