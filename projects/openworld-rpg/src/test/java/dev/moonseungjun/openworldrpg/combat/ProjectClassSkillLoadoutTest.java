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

        assertTrue(
                ProjectClassSkillLoadout
                        .spellId(RootClass.MAGE, 1)
                        .isEmpty()
        );
        assertTrue(
                ProjectClassSkillLoadout
                        .spellId(RootClass.CLERIC, 2)
                        .isEmpty()
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
