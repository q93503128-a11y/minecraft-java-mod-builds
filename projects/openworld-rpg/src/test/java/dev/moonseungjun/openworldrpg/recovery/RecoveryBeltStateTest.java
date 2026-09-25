package dev.moonseungjun.openworldrpg.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class RecoveryBeltStateTest {
    @Test
    void beltStartsEmptyWithFourSlotsAndLoadsCommittedDoseOnce() {
        var empty = RecoveryBeltState.empty();
        var loaded = empty.loadCommittedReserveDoseOnce(
                "openworld_rpg:r01/opening_loadout/recovery/healing_potion",
                RecoveryConsumable.HEALING_POTION
        );
        var retried = loaded.loadCommittedReserveDoseOnce(
                "openworld_rpg:r01/opening_loadout/recovery/healing_potion",
                RecoveryConsumable.HEALING_POTION
        );

        assertEquals(4, empty.slots().size());
        assertEquals(0, empty.loadedCount());
        assertEquals(1, loaded.loadedCount());
        assertEquals(
                RecoveryConsumable.HEALING_POTION,
                loaded.selectedConsumable().orElseThrow()
        );
        assertSame(loaded, retried);
    }

    @Test
    void successfulResolutionConsumesExactlyOneDoseAndStartsSixSecondLockout() {
        var loaded = RecoveryBeltState.empty().loadCommittedReserveDoseOnce(
                "openworld_rpg:test/load",
                RecoveryConsumable.HEALING_POTION
        );

        var resolved = loaded.consumeSelectedAtResolution(200L);

        assertEquals(RecoveryConsumable.HEALING_POTION, resolved.consumable());
        assertEquals(0, resolved.state().loadedCount());
        assertEquals(320L, resolved.state().sharedLockoutUntilTick());
        assertTrue(resolved.state().isLockedOut(319L));
        assertThrows(
                IllegalStateException.class,
                () -> resolved.state().consumeSelectedAtResolution(250L)
        );
    }

    @Test
    void actionConsumesTheOriginallyStartedSlotEvenIfSelectionChanges() {
        var loaded = RecoveryBeltState.empty()
                .loadCommittedReserveDoseOnce(
                        "openworld_rpg:test/heal",
                        RecoveryConsumable.HEALING_POTION
                )
                .loadCommittedReserveDoseOnce(
                        "openworld_rpg:test/focus",
                        RecoveryConsumable.FOCUS_DRAUGHT
                )
                .withSelectedSlot(1);

        var resolved = loaded.consumeSlotAtResolution(
                0,
                RecoveryConsumable.HEALING_POTION,
                200L
        );

        assertEquals(RecoveryConsumable.HEALING_POTION, resolved.consumable());
        assertTrue(resolved.state().consumableAt(0).isEmpty());
        assertEquals(
                RecoveryConsumable.FOCUS_DRAUGHT,
                resolved.state().selectedConsumable().orElseThrow()
        );
    }

    @Test
    void beltStateSurvivesCodecRoundTrip() {
        var original = RecoveryBeltState.empty()
                .loadCommittedReserveDoseOnce(
                        "openworld_rpg:test/load_a",
                        RecoveryConsumable.FOCUS_DRAUGHT
                )
                .loadCommittedReserveDoseOnce(
                        "openworld_rpg:test/load_b",
                        RecoveryConsumable.CLEANSING_TONIC
                )
                .withSelectedSlot(1);

        var encoded = RecoveryBeltState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = RecoveryBeltState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void fullBeltRejectsUncommittedFifthDose() {
        var state = RecoveryBeltState.empty()
                .loadCommittedReserveDoseOnce("openworld_rpg:test/1", RecoveryConsumable.HEALING_POTION)
                .loadCommittedReserveDoseOnce("openworld_rpg:test/2", RecoveryConsumable.HEALING_POTION)
                .loadCommittedReserveDoseOnce("openworld_rpg:test/3", RecoveryConsumable.FOCUS_DRAUGHT)
                .loadCommittedReserveDoseOnce("openworld_rpg:test/4", RecoveryConsumable.CLEANSING_TONIC);

        assertThrows(
                IllegalStateException.class,
                () -> state.loadCommittedReserveDoseOnce(
                        "openworld_rpg:test/5",
                        RecoveryConsumable.HEALING_POTION
                )
        );
    }
}
