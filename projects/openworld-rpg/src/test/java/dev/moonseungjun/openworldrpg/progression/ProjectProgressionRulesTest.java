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
}
