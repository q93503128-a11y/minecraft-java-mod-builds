package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectClassSkillLoadout;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class ProjectClassSkillLoadoutTest {
    @Test
    void implementedSpellsKeepTheirCanonicalRootSlots() {
        assertEquals(
                ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.WARRIOR, 0)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.WARRIOR, 1)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.WARRIOR, 2)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.WARRIOR, 3)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.WARRIOR, 4)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.ARC_BOLT_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 0)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.RADIANT_LANCE_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.CLERIC, 0)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.MEND_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.CLERIC, 1)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.CONSECRATED_GROUND_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.CLERIC, 2)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.REBUKE_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.CLERIC, 3)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.SANCTUARY_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.CLERIC, 4)
                        .orElseThrow()
        );

        assertEquals(
                ProjectSpellSpec.PHASE_STEP_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 1)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.FROST_RING_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 2)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.FLAME_BURST_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 3)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.ASTRAL_CONVERGENCE_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 4)
                        .orElseThrow()
        );
        assertEquals(
                5,
                ProjectClassSkillLoadout
                        .implementedSlots(RootClass.MAGE)
                        .size()
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.HUNTER, 0)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_PINNING_SHOT_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.HUNTER, 1)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_FAN_OF_ARROWS_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.HUNTER, 2)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_POWER_SHOT_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.HUNTER, 3)
                        .orElseThrow()
        );
        assertEquals(
                ProjectSpellSpec.HUNTER_SKYFALL_ID,
                ProjectClassSkillLoadout
                        .spellId(RootClass.HUNTER, 4)
                        .orElseThrow()
        );
        assertEquals(
                5,
                ProjectClassSkillLoadout
                        .implementedSlots(RootClass.HUNTER)
                        .size()
        );
        assertEquals(
                5,
                ProjectClassSkillLoadout
                        .implementedSlots(RootClass.CLERIC)
                        .size()
        );
        assertEquals(
                5,
                ProjectClassSkillLoadout
                        .implementedSlots(RootClass.WARRIOR)
                        .size()
        );
    }

    @Test
    void fiveSlotContractKeepsUltimateInSlotFive() {
        assertEquals(4, ProjectClassSkillLoadout.ACTIVE_SLOT_COUNT);
        assertEquals(4, ProjectClassSkillLoadout.ULTIMATE_SLOT);
        assertEquals(5, ProjectClassSkillLoadout.TOTAL_SLOT_COUNT);
    }
}
