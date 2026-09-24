package dev.moonseungjun.openworldrpg.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.recovery.RecoveryConsumable;
import org.junit.jupiter.api.Test;

class ProjectBackpackStateTest {
    @Test
    void startingBackpackIsThirtySixSlotsAndMergesCompatibleStacks() {
        var healing15 = ProjectInventoryItem.recovery(
                "openworld_rpg:healing_potion",
                RecoveryConsumable.HEALING_POTION,
                15,
                0
        );
        var healing5 = ProjectInventoryItem.recovery(
                "openworld_rpg:healing_potion",
                RecoveryConsumable.HEALING_POTION,
                5,
                0
        );

        var first = ProjectBackpackState.startingBackpack().insert(healing15);
        var second = first.state().insert(healing5);

        assertEquals(36, second.state().capacity());
        assertEquals(1, second.state().usedSlots());
        assertEquals(20, second.state().itemAt(0).orElseThrow().quantity());
        assertTrue(second.remainder().isEmpty());
    }

    @Test
    void fullBackpackReturnsUndeliveredRemainderInsteadOfDeletingIt() {
        ProjectBackpackState state = ProjectBackpackState.startingBackpack();
        for (int i = 0; i < state.capacity(); i++) {
            var item = ProjectInventoryItem.ordinary(
                    "openworld_rpg:test_item_" + i,
                    1,
                    1,
                    0
            );
            state = state.insert(item).state();
        }

        var extra = ProjectInventoryItem.ordinary(
                "openworld_rpg:overflow",
                1,
                1,
                0
        );
        var result = state.insert(extra);

        assertEquals(36, result.state().usedSlots());
        assertEquals(extra, result.remainder().orElseThrow());
    }
}
