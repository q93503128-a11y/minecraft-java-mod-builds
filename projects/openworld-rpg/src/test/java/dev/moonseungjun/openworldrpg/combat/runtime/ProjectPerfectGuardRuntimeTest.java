package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import org.junit.jupiter.api.Test;

class ProjectPerfectGuardRuntimeTest {
    @Test
    void bossPerfectGuardUsesCanonicalFifteenPercentPoiseReward() {
        assertEquals(
                36.5,
                ProjectPerfectGuardRuntime.poiseDamageForRank(
                        ExternalActorCombatProfile.CombatRank.BOSS,
                        190.0,
                        1.0
                ),
                0.0001
        );
        assertEquals(
                42.34,
                ProjectPerfectGuardRuntime.poiseDamageForRank(
                        ExternalActorCombatProfile.CombatRank.BOSS,
                        190.0,
                        1.16
                ),
                0.0001
        );
    }

    @Test
    void eliteAndMinibossUseCanonicalTwentyPercentPoiseReward() {
        assertEquals(
                28.0,
                ProjectPerfectGuardRuntime.poiseDamageForRank(
                        ExternalActorCombatProfile.CombatRank.NORMAL_ELITE,
                        100.0,
                        1.0
                ),
                0.0001
        );
        assertEquals(
                28.0,
                ProjectPerfectGuardRuntime.poiseDamageForRank(
                        ExternalActorCombatProfile.CombatRank.MINIBOSS,
                        100.0,
                        1.0
                ),
                0.0001
        );
    }

    @Test
    void invalidPoiseInputsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectPerfectGuardRuntime.poiseDamageForRank(
                        ExternalActorCombatProfile.CombatRank.BOSS,
                        0.0,
                        1.0
                )
        );
    }
}
