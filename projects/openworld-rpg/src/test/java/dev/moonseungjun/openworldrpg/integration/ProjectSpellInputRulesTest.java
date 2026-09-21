package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.spellengine.ProjectSpellInputRules;
import org.junit.jupiter.api.Test;

class ProjectSpellInputRulesTest {
    @Test
    void arcBoltRequiresReleaseBeforeAnotherPhysicalPressCanCast() {
        assertFalse(ProjectSpellInputRules.suppressHeldRepeat("openworld_rpg:arc_bolt", true));
        assertTrue(ProjectSpellInputRules.suppressHeldRepeat("openworld_rpg:arc_bolt", false));
        assertFalse(ProjectSpellInputRules.suppressHeldRepeat("other_mod:spell", false));
    }
}
