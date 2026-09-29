package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectRangedProjectileContext;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectRangedProjectileContextTest {
    @Test
    void launchSnapshotKeepsOriginalBuildAfterLaterEquipmentSwap() {
        var bowBuild = new PlayerCombatBuildState(
                8,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.BOW, 8)
        );
        var laterCrossbowBuild = new PlayerCombatBuildState(
                8,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.CROSSBOW, 8)
        );

        var shot = new ProjectRangedProjectileContext.RangedShot(
                UUID.randomUUID(),
                ProjectWeaponFamily.BOW,
                bowBuild,
                0.75
        );

        assertSame(bowBuild, shot.build());
        assertEquals(ProjectWeaponFamily.BOW, shot.weaponFamily());
        assertEquals(0.75, shot.drawPower(), 0.0001);
        assertNotEquals(laterCrossbowBuild, shot.build());
    }

    @Test
    void crossbowSnapshotIsAlwaysAFullShotAndMustMatchBuildFamily() {
        var crossbowBuild = new PlayerCombatBuildState(
                8,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.CROSSBOW, 8)
        );
        var bowBuild = new PlayerCombatBuildState(
                8,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.BOW, 8)
        );

        assertEquals(
                1.0,
                new ProjectRangedProjectileContext.RangedShot(
                        UUID.randomUUID(),
                        ProjectWeaponFamily.CROSSBOW,
                        crossbowBuild,
                        1.0
                ).drawPower(),
                0.0001
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRangedProjectileContext.RangedShot(
                        UUID.randomUUID(),
                        ProjectWeaponFamily.CROSSBOW,
                        crossbowBuild,
                        0.5
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRangedProjectileContext.RangedShot(
                        UUID.randomUUID(),
                        ProjectWeaponFamily.CROSSBOW,
                        bowBuild,
                        1.0
                )
        );
    }
}
