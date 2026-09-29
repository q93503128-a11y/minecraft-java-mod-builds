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

        assertTrue(
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 1)
                        .isEmpty()
        );
        assertEquals(
                5,
                ProjectClassSkillLoadout
                        .implementedSlots(RootClass.CLERIC)
                        .size()
        );
        assertTrue(
                ProjectClassSkillLoadout
                        .implementedSlots(RootClass.WARRIOR)
                        .isEmpty()
        );
    }

    @Test
    void fiveSlotContractKeepsUltimateInSlotFive() {
        assertEquals(4, ProjectClassSkillLoadout.ACTIVE_SLOT_COUNT);
        assertEquals(4, ProjectClassSkillLoadout.ULTIMATE_SLOT);
        assertEquals(5, ProjectClassSkillLoadout.TOTAL_SLOT_COUNT);
    }
}
