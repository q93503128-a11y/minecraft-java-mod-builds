package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectHealingRuntime;
import org.junit.jupiter.api.Test;

class ProjectHealingRuntimeApplicationTest {
    @Test
    void rejectedHealCannotCarryNatureSpiritParticipation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectHealingRuntime.Application(
                        false,
                        0.0,
                        0.0,
                        false,
                        true
                )
        );
    }

    @Test
    void encounterParticipationRequiresRealEffectiveHealing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectHealingRuntime.Application(
                        true,
                        10.0,
                        0.0,
                        false,
                        true
                )
        );
    }

    @Test
    void oneHealCannotQualifyForTwoEncounterActors() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectHealingRuntime.Application(
                        true,
                        10.0,
                        5.0,
                        true,
                        true
                )
        );
    }

    @Test
    void oneRegalhartSupportQualificationIsAValidAppliedResult() {
        assertDoesNotThrow(
                () -> new ProjectHealingRuntime.Application(
                        true,
                        10.0,
                        5.0,
                        false,
                        false,
                        true
                )
        );
    }

    @Test
    void crossEncounterHealQualificationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectHealingRuntime.Application(
                        true,
                        10.0,
                        5.0,
                        false,
                        true,
                        true
                )
        );
    }

    @Test
    void oneNatureSpiritSupportQualificationIsAValidAppliedResult() {
        assertDoesNotThrow(
                () -> new ProjectHealingRuntime.Application(
                        true,
                        10.0,
                        5.0,
                        false,
                        true
                )
        );
    }
}
