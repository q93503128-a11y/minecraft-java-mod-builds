package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import dev.moonseungjun.openworldrpg.progression.r01.R01RoadsideTroubleRules;
import dev.moonseungjun.openworldrpg.progression.r01.R01WorldActionAuthority;
import org.junit.jupiter.api.Test;

class R01WorldActionAuthorityTest {
    @Test
    void onlyThreeClosedR01ResourceFamiliesCreditDustGathering() {
        assertTrue(R01WorldActionAuthority.isValidDustGatherResource(
                R01WorldActionAuthority.IRON_ORE
        ));
        assertTrue(R01WorldActionAuthority.isValidDustGatherResource(
                R01WorldActionAuthority.HARDWOOD
        ));
        assertTrue(R01WorldActionAuthority.isValidDustGatherResource(
                R01WorldActionAuthority.HEALING_HERB
        ));
        assertFalse(R01WorldActionAuthority.isValidDustGatherResource(
                "openworld_rpg:verdant_crystal"
        ));

        assertEquals(
                R01PlayerState.QuarryRoadAction.R01_GATHERING_NODE,
                R01WorldActionAuthority.validGather(
                        R01WorldActionAuthority.HEALING_HERB
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> R01WorldActionAuthority.validGather("minecraft:oak_log")
        );
    }

    @Test
    void zeroActionViperProximityNeverCreatesCombatCredit() {
        assertEquals(
                R01PlayerState.QuarryRoadAction.MEADOW_VIPER,
                R01WorldActionAuthority.meadowViperContribution(
                        R01WorldActionAuthority.CombatContribution.DAMAGE
                )
        );
        assertEquals(
                R01PlayerState.QuarryRoadAction.MEADOW_VIPER,
                R01WorldActionAuthority.meadowViperContribution(
                        R01WorldActionAuthority.CombatContribution.VALID_SUPPORT
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> R01WorldActionAuthority.meadowViperContribution(
                        R01WorldActionAuthority.CombatContribution.PROXIMITY_ONLY
                )
        );
    }

    @Test
    void roadsideTroubleTimingAndThreatScalingMatchClosedR01Canon() {
        assertFalse(R01RoadsideTroubleRules.firstCycleEligible(
                true,
                R01RoadsideTroubleRules.FIRST_CYCLE_DELAY_TICKS - 1,
                false,
                true
        ));
        assertTrue(R01RoadsideTroubleRules.firstCycleEligible(
                true,
                R01RoadsideTroubleRules.FIRST_CYCLE_DELAY_TICKS,
                false,
                true
        ));
        assertFalse(R01RoadsideTroubleRules.repeatCycleEligible(
                R01RoadsideTroubleRules.REPEAT_DELAY_TICKS - 1,
                false,
                true
        ));
        assertTrue(R01RoadsideTroubleRules.repeatCycleEligible(
                R01RoadsideTroubleRules.REPEAT_DELAY_TICKS,
                false,
                true
        ));

        assertEquals(2, R01RoadsideTroubleRules.threatCount(1));
        assertEquals(3, R01RoadsideTroubleRules.threatCount(2));
        assertEquals(4, R01RoadsideTroubleRules.threatCount(3));
        assertEquals(4, R01RoadsideTroubleRules.threatCount(10));

        assertTrue(R01RoadsideTroubleRules.complete(true, true, true));
        assertFalse(R01RoadsideTroubleRules.complete(true, true, false));
        assertEquals(2_400L, R01RoadsideTroubleRules.ABANDON_EMPTY_TICKS);
    }
}
