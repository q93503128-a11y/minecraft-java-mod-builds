package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectBasicAttackCadenceRuntime;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectBasicAttackCadenceRuntimeTest {
    @Test
    void swordRejectsEarlyFollowupButAllowsDistinctTargetsInOneSwing() {
        UUID player = UUID.randomUUID();
        try {
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    player, ProjectWeaponFamily.SWORD, 0.0, 0, 10, 0
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    player, ProjectWeaponFamily.SWORD, 0.0, 0, 11, 0
            ));
            assertFalse(ProjectBasicAttackCadenceRuntime.authorize(
                    player, ProjectWeaponFamily.SWORD, 0.0, 0, 10, 0
            ));
            assertFalse(ProjectBasicAttackCadenceRuntime.authorize(
                    player, ProjectWeaponFamily.SWORD, 0.0, 1, 10, 15
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    player, ProjectWeaponFamily.SWORD, 0.0, 1, 10, 16
            ));
        } finally {
            ProjectBasicAttackCadenceRuntime.disconnect(player);
        }
    }

    @Test
    void classAttackSpeedBonusAlsoShortensAuthoritativeCadence() {
        UUID player = UUID.randomUUID();
        try {
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    player,
                    ProjectWeaponFamily.SWORD,
                    0.0,
                    0.06,
                    0,
                    1,
                    0
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    player,
                    ProjectWeaponFamily.SWORD,
                    0.0,
                    0.06,
                    1,
                    1,
                    16
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    player,
                    ProjectWeaponFamily.SWORD,
                    0.0,
                    0.06,
                    2,
                    1,
                    31
            ));
        } finally {
            ProjectBasicAttackCadenceRuntime.disconnect(player);
        }
    }

    @Test
    void gearAttackSpeedShortensServerCadenceAndDualBladeEventsUseTwoHitCycleRate() {
        UUID swordPlayer = UUID.randomUUID();
        UUID dualPlayer = UUID.randomUUID();
        try {
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    swordPlayer, ProjectWeaponFamily.SWORD, 0.35, 0, 1, 0
            ));
            assertFalse(ProjectBasicAttackCadenceRuntime.authorize(
                    swordPlayer, ProjectWeaponFamily.SWORD, 0.35, 1, 1, 11
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    swordPlayer, ProjectWeaponFamily.SWORD, 0.35, 1, 1, 12
            ));

            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    dualPlayer, ProjectWeaponFamily.DUAL_BLADES, 0.0, 0, 1, 0
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    dualPlayer, ProjectWeaponFamily.DUAL_BLADES, 0.0, 1, 1, 8
            ));
            assertTrue(ProjectBasicAttackCadenceRuntime.authorize(
                    dualPlayer, ProjectWeaponFamily.DUAL_BLADES, 0.0, 2, 1, 15
            ));
        } finally {
            ProjectBasicAttackCadenceRuntime.disconnect(swordPlayer);
            ProjectBasicAttackCadenceRuntime.disconnect(dualPlayer);
        }
    }
}
