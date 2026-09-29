package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.spellengine.ProjectSpellInputRules;
import org.junit.jupiter.api.Test;

class ProjectSpellInputRulesTest {
    @Test
    void projectTapSpellsRequireReleaseBeforeAnotherPhysicalPressCanCast() {
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:arc_bolt",
                        true
                )
        );
        assertTrue(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:arc_bolt",
                        false
                )
        );
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:radiant_lance",
                        true
                )
        );
        assertTrue(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:radiant_lance",
                        false
                )
        );
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:mend",
                        true
                )
        );
        assertTrue(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:mend",
                        false
                )
        );
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:rebuke",
                        true
                )
        );
        assertTrue(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:rebuke",
                        false
                )
        );
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:sanctuary",
                        true
                )
        );
        assertTrue(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "openworld_rpg:sanctuary",
                        false
                )
        );
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "other_mod:spell",
                        false
                )
        );
    }
}
