package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDodgeRuntime;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import org.junit.jupiter.api.Test;

class ProjectDodgeRuntimeTest {
    private static final double EPSILON = 0.0001;

    @Test
    void canonicalServerValuesStayCoupledToDefenseAuthority() {
        assertEquals(3.20, ProjectDodgeRuntime.LEVEL_GROUND_TRAVEL_BLOCKS, EPSILON);
        assertEquals(2.00, ProjectDodgeRuntime.NO_INPUT_BACKSTEP_BLOCKS, EPSILON);
        assertEquals(
                PlayerDefenseRuntimeState.DODGE_ACTION_TICKS,
                ProjectDodgeRuntime.MOVEMENT_STEPS
        );
        assertEquals(9, ProjectDodgeRuntime.MOVEMENT_STEPS);
        assertEquals(30.0, PlayerDefenseRuntimeState.DODGE_STAMINA_COST, EPSILON);
        assertEquals(6L, PlayerDefenseRuntimeState.DODGE_INVULNERABILITY_TICKS);
        assertEquals(11L, PlayerDefenseRuntimeState.DODGE_REENTRY_TICKS);
        assertEquals(12L, PlayerDefenseRuntimeState.DODGE_REGEN_DELAY_TICKS);
    }

    @Test
    void directionalIntentNormalizesDiagonalsAndOwnsStablePresentationCodes() {
        var forward = ProjectDodgeRuntime.DirectionIntent.resolve(1.0F, 0.0F);
        var backward = ProjectDodgeRuntime.DirectionIntent.resolve(-1.0F, 0.0F);
        var left = ProjectDodgeRuntime.DirectionIntent.resolve(0.0F, -1.0F);
        var right = ProjectDodgeRuntime.DirectionIntent.resolve(0.0F, 1.0F);
        var diagonal = ProjectDodgeRuntime.DirectionIntent.resolve(1.0F, 1.0F);
        var none = ProjectDodgeRuntime.DirectionIntent.resolve(0.0F, 0.0F);

        assertEquals(0, forward.directionCode());
        assertEquals(1, backward.directionCode());
        assertEquals(2, left.directionCode());
        assertEquals(3, right.directionCode());
        assertEquals(0, diagonal.directionCode());
        assertEquals(Math.sqrt(0.5), diagonal.forward(), EPSILON);
        assertEquals(Math.sqrt(0.5), diagonal.strafe(), EPSILON);
        assertTrue(none.noDirectionalInput());
        assertEquals(1, none.directionCode());
        assertFalse(forward.noDirectionalInput());
    }
}
