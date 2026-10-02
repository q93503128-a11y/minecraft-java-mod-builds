package dev.moonseungjun.openworldrpg.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import org.junit.jupiter.api.Test;

class RecoveryBeltReloadServiceTest {
    @Test
    void refillConsumesOnlyRealBackpackReservesAndLeavesMissingSlotsEmpty() {
        var inventory = PlayerInventoryState.initial();
        inventory = inventory.deliverBackpackOnce(
                "openworld_rpg:test/reload/healing",
                ProjectInventoryItem.recovery(
                        RecoveryConsumable.HEALING_POTION.itemId(),
                        RecoveryConsumable.HEALING_POTION,
                        2,
                        0L
                )
        ).state();
        inventory = inventory.deliverBackpackOnce(
                "openworld_rpg:test/reload/focus",
                ProjectInventoryItem.recovery(
                        RecoveryConsumable.FOCUS_DRAUGHT.itemId(),
                        RecoveryConsumable.FOCUS_DRAUGHT,
                        1,
                        0L
                )
        ).state();

        var setup = RecoveryBeltSetupState.empty()
                .withSlot(0, RecoveryBeltSlot.HEALING_POTION)
                .withSlot(1, RecoveryBeltSlot.HEALING_POTION)
                .withSlot(2, RecoveryBeltSlot.FOCUS_DRAUGHT)
                .withSlot(3, RecoveryBeltSlot.CLEANSING_TONIC);

        var plan = RecoveryBeltReloadService.planReload(
                RecoveryBeltState.empty(),
                setup,
                inventory
        );

        assertEquals(
                RecoveryBeltReloadService.ReloadStatus.PARTIAL,
                plan.result().status()
        );
        assertEquals(3, plan.result().loadedCount());
        assertEquals(java.util.List.of(3), plan.result().missingReserveSlots());
        assertEquals(3, plan.belt().loadedCount());
        assertEquals(
                RecoveryConsumable.HEALING_POTION,
                plan.belt().consumableAt(0).orElseThrow()
        );
        assertEquals(
                RecoveryConsumable.FOCUS_DRAUGHT,
                plan.belt().consumableAt(2).orElseThrow()
        );
        assertTrue(plan.belt().consumableAt(3).isEmpty());
        assertFalse(
                plan.inventory()
                        .consumeBackpackStackable(
                                RecoveryConsumable.HEALING_POTION.itemId(),
                                1
                        )
                        .consumed()
        );
        assertFalse(
                plan.inventory()
                        .consumeBackpackStackable(
                                RecoveryConsumable.FOCUS_DRAUGHT.itemId(),
                                1
                        )
                        .consumed()
        );
    }

    @Test
    void alreadyLoadedSlotNeverConsumesAnotherReserve() {
        var inventory = PlayerInventoryState.initial()
                .deliverBackpackOnce(
                        "openworld_rpg:test/reload/spare",
                        ProjectInventoryItem.recovery(
                                RecoveryConsumable.HEALING_POTION.itemId(),
                                RecoveryConsumable.HEALING_POTION,
                                1,
                                0L
                        )
                ).state();
        var belt = RecoveryBeltState.empty()
                .loadReservedDoseIntoSlot(
                        0,
                        RecoveryConsumable.HEALING_POTION
                );
        var setup = RecoveryBeltSetupState.empty()
                .withSlot(0, RecoveryBeltSlot.HEALING_POTION);

        var plan = RecoveryBeltReloadService.planReload(
                belt,
                setup,
                inventory
        );

        assertEquals(
                RecoveryBeltReloadService.ReloadStatus.NOTHING_TO_RELOAD,
                plan.result().status()
        );
        assertTrue(
                plan.inventory()
                        .consumeBackpackStackable(
                                RecoveryConsumable.HEALING_POTION.itemId(),
                                1
                        )
                        .consumed()
        );
    }

    @Test
    void unconfiguredBeltNeverConsumesReserve() {
        var inventory = PlayerInventoryState.initial()
                .deliverBackpackOnce(
                        "openworld_rpg:test/reload/unconfigured",
                        ProjectInventoryItem.recovery(
                                RecoveryConsumable.HEALING_POTION.itemId(),
                                RecoveryConsumable.HEALING_POTION,
                                1,
                                0L
                        )
                ).state();

        var plan = RecoveryBeltReloadService.planReload(
                RecoveryBeltState.empty(),
                RecoveryBeltSetupState.empty(),
                inventory
        );

        assertEquals(
                RecoveryBeltReloadService.ReloadStatus.NO_CONFIGURATION,
                plan.result().status()
        );
        assertEquals(0, plan.result().loadedCount());
        assertTrue(
                plan.inventory()
                        .consumeBackpackStackable(
                                RecoveryConsumable.HEALING_POTION.itemId(),
                                1
                        )
                        .consumed()
        );
    }
}
