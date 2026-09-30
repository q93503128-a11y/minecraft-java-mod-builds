package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProjectProgressionRulesTest {
    @Test
    void combatXpCurveMatchesCanonAnchors() {
        assertEquals(150L, ProjectProgressionRules.combatXpToNext(1));
        assertEquals(760L, ProjectProgressionRules.combatXpToNext(8));
        assertEquals(1280L, ProjectProgressionRules.combatXpToNext(12));
        assertEquals(29010L, ProjectProgressionRules.combatXpToNext(79));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectProgressionRules.combatXpToNext(80)
        );
    }

    @Test
    void classSwitchGoldCostMatchesCanonAnchorsAndCap() {
        assertEquals(200L, ProjectProgressionRules.classSwitchGoldCost(8));
        assertEquals(510L, ProjectProgressionRules.classSwitchGoldCost(20));
        assertEquals(1290L, ProjectProgressionRules.classSwitchGoldCost(40));
        assertEquals(2390L, ProjectProgressionRules.classSwitchGoldCost(60));
        assertEquals(2500L, ProjectProgressionRules.classSwitchGoldCost(80));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectProgressionRules.classSwitchGoldCost(0)
        );
    }

    @Test
    void branchSwitchGoldCostMatchesCanonFormulaAndCaps() {
        assertEquals(150L, ProjectProgressionRules.branchSwitchGoldCost(8));
        assertEquals(310L, ProjectProgressionRules.branchSwitchGoldCost(20));
        assertEquals(770L, ProjectProgressionRules.branchSwitchGoldCost(40));
        assertEquals(1430L, ProjectProgressionRules.branchSwitchGoldCost(60));
        assertEquals(1500L, ProjectProgressionRules.branchSwitchGoldCost(80));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectProgressionRules.branchSwitchGoldCost(0)
        );
    }

    @Test
    void passiveRespecGoldCostMatchesCanonFormulaAndCaps() {
        assertEquals(50L, ProjectProgressionRules.passiveRespecGoldCost(8));
        assertEquals(100L, ProjectProgressionRules.passiveRespecGoldCost(20));
        assertEquals(260L, ProjectProgressionRules.passiveRespecGoldCost(40));
        assertEquals(480L, ProjectProgressionRules.passiveRespecGoldCost(60));
        assertEquals(500L, ProjectProgressionRules.passiveRespecGoldCost(80));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectProgressionRules.passiveRespecGoldCost(0)
        );
    }

    @Test
    void classXpCurveMatchesCanonAnchors() {
        assertEquals(120L, ProjectProgressionRules.classXpToNext(1));
        assertEquals(250L, ProjectProgressionRules.classXpToNext(5));
        assertEquals(500L, ProjectProgressionRules.classXpToNext(10));
        assertEquals(5880L, ProjectProgressionRules.classXpToNext(49));
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectProgressionRules.classXpToNext(50)
        );
    }
    @Test
    void combatRewardLevelMultiplierMatchesCanonicalAntiFarmCurve() {
        assertEquals(
                1.20,
                ProjectProgressionRules.combatRewardLevelMultiplier(16, 10),
                0.0001
        );
        assertEquals(
                1.10,
                ProjectProgressionRules.combatRewardLevelMultiplier(13, 10),
                0.0001
        );
        assertEquals(
                1.00,
                ProjectProgressionRules.combatRewardLevelMultiplier(8, 10),
                0.0001
        );
        assertEquals(
                0.75,
                ProjectProgressionRules.combatRewardLevelMultiplier(7, 10),
                0.0001
        );
        assertEquals(
                0.40,
                ProjectProgressionRules.combatRewardLevelMultiplier(4, 10),
                0.0001
        );
        assertEquals(
                0.10,
                ProjectProgressionRules.combatRewardLevelMultiplier(1, 12),
                0.0001
        );
    }

}
