package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerActionRuntimeState;
import org.junit.jupiter.api.Test;

class PlayerActionRuntimeStateTest {
    @Test
    void committedActionOwnsMovementAndDodgeCancelWindow() {
        var state = new PlayerActionRuntimeState();
        var begun = state.beginAction(
                "openworld_rpg:warrior_cyclone_cut",
                100L,
                13,
                10,
                0.75
        );

        assertTrue(begun.accepted());
        assertEquals(110L, begun.dodgeCancelAtTick());
        assertEquals(113L, begun.endTick());
        assertEquals(0.75, state.movementMultiplier(100L), 0.0001);
        assertFalse(state.canDodgeCancel(109L));
        assertTrue(state.canDodgeCancel(110L));
        assertFalse(state.basicAttackAllowed(110L));
        assertTrue(state.commitDodgeCancel(110L));
        assertEquals(1.0, state.movementMultiplier(110L), 0.0001);
        assertTrue(state.basicAttackAllowed(110L));
    }

    @Test
    void reactionOverridesSkillAndCannotBeDodgeCancelled() {
        var state = new PlayerActionRuntimeState();
        assertTrue(state.beginAction(
                "openworld_rpg:skill",
                20L,
                20,
                10,
                0.75
        ).accepted());

        var reaction = state.applyReaction(
                "openworld_rpg:reaction/guard_break",
                24L,
                17
        );

        assertTrue(reaction.accepted());
        assertTrue(state.hardReactionActive(40L));
        assertFalse(state.canDodgeCancel(40L));
        assertFalse(state.basicAttackAllowed(40L));
        assertEquals(1.0, state.movementMultiplier(40L), 0.0001);
        assertFalse(state.hardReactionActive(41L));
        assertTrue(state.basicAttackAllowed(41L));
    }

    @Test
    void namedActionCancellationOnlyClearsTheMatchingCommitment() {
        var state = new PlayerActionRuntimeState();
        assertTrue(state.beginAction(
                "openworld_rpg:recovery_belt",
                50L,
                19,
                19,
                0.65
        ).accepted());

        assertFalse(state.cancelAction("openworld_rpg:other_action", 55L));
        assertEquals(0.65, state.movementMultiplier(55L), 0.0001);
        assertTrue(state.cancelAction("openworld_rpg:recovery_belt", 55L));
        assertTrue(state.canStartAction(55L));
        assertEquals(1.0, state.movementMultiplier(55L), 0.0001);
    }

    @Test
    void actionExpiresAtExactEndTick() {
        var state = new PlayerActionRuntimeState();
        assertTrue(state.beginAction(
                "openworld_rpg:test",
                5L,
                14,
                10,
                1.0
        ).accepted());

        assertFalse(state.canStartAction(18L));
        assertTrue(state.canStartAction(19L));
    }
}
