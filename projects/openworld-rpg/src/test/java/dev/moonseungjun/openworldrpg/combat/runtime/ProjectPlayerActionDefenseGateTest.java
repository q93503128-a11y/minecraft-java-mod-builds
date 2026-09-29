package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectPlayerActionDefenseGateTest {
    @Test
    void liveDefenseStateCannotDodgeOrRaiseGuardBeforeActionCancelWindow() {
        UUID playerId = UUID.randomUUID();
        try {
            var action = ProjectPlayerActionRuntime.state(playerId);
            assertTrue(action.beginAction(
                    "openworld_rpg:test_committed_action",
                    100L,
                    13,
                    10,
                    0.75
            ).accepted());

            var defense = new PlayerDefenseRuntimeState(playerId);
            var resources = new PlayerCombatState(5, 100L);

            assertFalse(
                    ProjectPlayerActionRuntime.canBufferDodge(
                            playerId,
                            107L
                    )
            );
            assertTrue(
                    ProjectPlayerActionRuntime.canBufferDodge(
                            playerId,
                            108L
                    )
            );
            assertTrue(
                    ProjectPlayerActionRuntime.canBufferDodge(
                            playerId,
                            109L
                    )
            );
            assertFalse(defense.pressGuard(109L).accepted());
            assertFalse(
                    defense.tryBeginDodge(
                            resources,
                            109L,
                            false
                    )
            );
            assertTrue(
                    defense.tryBeginDodge(
                            resources,
                            110L,
                            false
                    )
            );
            assertTrue(
                    ProjectPlayerActionRuntime.basicAttackAllowed(
                            playerId,
                            110L
                    )
            );
        } finally {
            ProjectPlayerActionRuntime.disconnect(playerId);
        }
    }

    @Test
    void hardReactionBlocksGuardAndDodgeUntilExactEndTick() {
        UUID playerId = UUID.randomUUID();
        try {
            var action = ProjectPlayerActionRuntime.state(playerId);
            action.applyReaction(
                    "openworld_rpg:reaction/test",
                    20L,
                    17
            );

            var defense = new PlayerDefenseRuntimeState(playerId);
            var resources = new PlayerCombatState(5, 20L);

            assertFalse(defense.pressGuard(36L).accepted());
            assertFalse(
                    defense.tryBeginDodge(
                            resources,
                            36L,
                            false
                    )
            );
            assertTrue(defense.pressGuard(37L).accepted());
        } finally {
            ProjectPlayerActionRuntime.disconnect(playerId);
        }
    }
}
