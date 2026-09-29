package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ClericSkillCastRuntimeState;
import org.junit.jupiter.api.Test;

class ClericSkillCastRuntimeStateTest {
    @Test
    void acceptedCastSnapshotsGraceAndDoctrineUntilImpact() {
        var state = new ClericSkillCastRuntimeState();
        state.begin(
                "openworld_rpg:radiant_lance",
                true,
                1.08,
                100L
        );

        var pending = state.consume(
                "openworld_rpg:radiant_lance",
                250L
        );
        assertTrue(pending.graceEmpowered());
        assertEquals(1.08, pending.outputMultiplier(), 0.0001);
        assertNull(
                state.consume(
                        "openworld_rpg:radiant_lance",
                        251L
                )
        );
    }

    @Test
    void pendingCastExpiresAtTenSeconds() {
        var state = new ClericSkillCastRuntimeState();
        state.begin(
                "openworld_rpg:mend",
                false,
                1.10,
                5L
        );
        assertNull(
                state.consume(
                        "openworld_rpg:mend",
                        205L
                )
        );
    }
}
