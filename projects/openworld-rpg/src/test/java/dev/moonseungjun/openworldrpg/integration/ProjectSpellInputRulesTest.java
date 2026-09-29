package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
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
                        ProjectSpellSpec.CONSECRATED_GROUND_ID,
                        true
                )
        );
        assertTrue(
                ProjectSpellInputRules.suppressHeldRepeat(
                        ProjectSpellSpec.CONSECRATED_GROUND_ID,
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
        for (String spellId : java.util.List.of(
                ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID,
                ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID,
                ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID,
                ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID,
                ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID
        )) {
            assertFalse(
                    ProjectSpellInputRules.suppressHeldRepeat(
                            spellId,
                            true
                    )
            );
            assertTrue(
                    ProjectSpellInputRules.suppressHeldRepeat(
                            spellId,
                            false
                    )
            );
        }
        assertFalse(
                ProjectSpellInputRules.suppressHeldRepeat(
                        "other_mod:spell",
                        false
                )
        );
    }
}
