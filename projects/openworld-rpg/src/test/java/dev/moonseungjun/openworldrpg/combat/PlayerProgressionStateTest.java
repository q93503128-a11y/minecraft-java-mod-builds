package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class PlayerProgressionStateTest {
    @Test
    void initialProgressionHasNoInventedClassOrWeaponBuild() {
        var state = PlayerProgressionState.initial();

        assertEquals(1, state.combatLevel());
        assertFalse(state.activeClass().isPresent());
        assertEquals(0, state.allocation(RootClass.MAGE).spentPoints());
        assertFalse(state.buildWith(
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.STAFF, 1)
        ).isPresent());
    }

    @Test
    void perClassAllocationSurvivesCodecRoundTrip() {
        var original = PlayerProgressionState.initial()
                .withCombatLevel(8)
                .withAllocation(
                        RootClass.MAGE,
                        new AttributeAllocation(0, 0, 0, 0, 7, 0)
                )
                .withActiveClass(RootClass.MAGE);

        var encoded = PlayerProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerProgressionState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertEquals(7, decoded.allocation(RootClass.MAGE).intel());
        assertEquals(0, decoded.allocation(RootClass.WARRIOR).spentPoints());
    }

    @Test
    void loweringLevelBelowExistingAllocationFailsClosed() {
        var state = PlayerProgressionState.initial()
                .withCombatLevel(8)
                .withAllocation(
                        RootClass.MAGE,
                        new AttributeAllocation(0, 0, 0, 0, 7, 0)
                );

        assertThrows(IllegalArgumentException.class, () -> state.withCombatLevel(7));
    }
}
