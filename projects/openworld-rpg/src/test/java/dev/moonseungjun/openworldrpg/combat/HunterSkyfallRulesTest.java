package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.HunterSkyfallRules;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import org.junit.jupiter.api.Test;

class HunterSkyfallRulesTest {
    private static final double EPSILON = 0.0001;

    @Test
    void canonicalTotalsAreGroupedIntoFiveServerPulses() {
        assertEquals(
                7.0,
                HunterSkyfallRules.RADIUS_BLOCKS,
                EPSILON
        );
        assertEquals(
                90,
                HunterSkyfallRules.DURATION_TICKS
        );
        assertEquals(
                5,
                HunterSkyfallRules.PULSE_COUNT
        );
        assertEquals(
                1.10,
                HunterSkyfallRules
                        .ACTION_COEFFICIENT_PER_PULSE,
                EPSILON
        );
        assertEquals(
                0.60,
                HunterSkyfallRules
                        .POISE_COEFFICIENT_PER_PULSE,
                EPSILON
        );
        assertEquals(
                5.50,
                HunterSkyfallRules
                        .ACTION_COEFFICIENT_PER_PULSE
                        * HunterSkyfallRules.PULSE_COUNT,
                EPSILON
        );
        assertEquals(
                3.00,
                HunterSkyfallRules
                        .POISE_COEFFICIENT_PER_PULSE
                        * HunterSkyfallRules.PULSE_COUNT,
                EPSILON
        );
    }

    @Test
    void visualArrowPatternNeverLeavesAuthoritativeRadius() {
        for (int i = 0;
                i < HunterSkyfallRules.VISUAL_PROJECTILE_COUNT;
                i++) {
            assertTrue(
                    HunterSkyfallRules.visualOffset(i).radius()
                            <= HunterSkyfallRules.RADIUS_BLOCKS
                                    + EPSILON
            );
        }
    }

    @Test
    void unspecifiedMinibossBandFailsConservativelyIntoBossLikeSlow() {
        assertEquals(
                0.75,
                HunterSkyfallRules.movementMultiplier(
                        ExternalActorCombatProfile.CombatRank
                                .NORMAL_ELITE
                ),
                EPSILON
        );
        assertEquals(
                0.90,
                HunterSkyfallRules.movementMultiplier(
                        ExternalActorCombatProfile.CombatRank
                                .MINIBOSS
                ),
                EPSILON
        );
        assertEquals(
                0.90,
                HunterSkyfallRules.movementMultiplier(
                        ExternalActorCombatProfile.CombatRank
                                .BOSS
                ),
                EPSILON
        );
    }
}
