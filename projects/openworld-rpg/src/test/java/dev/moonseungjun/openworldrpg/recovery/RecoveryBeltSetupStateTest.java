package dev.moonseungjun.openworldrpg.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class RecoveryBeltSetupStateTest {
    @Test
    void emptySetupDoesNotInventAClassOrPotionRatio() {
        var setup = RecoveryBeltSetupState.empty();

        assertTrue(setup.isEmpty());
        assertEquals(0, setup.configuredCount());
        for (int slot = 0; slot < RecoveryActionRules.BELT_CAPACITY; slot++) {
            assertTrue(setup.desiredConsumableAt(slot).isEmpty());
        }
    }

    @Test
    void legacyMigrationMirrorsOnlyActuallyLoadedSlots() {
        var belt = RecoveryBeltState.empty()
                .loadReservedDoseIntoSlot(
                        0,
                        RecoveryConsumable.HEALING_POTION
                )
                .loadReservedDoseIntoSlot(
                        2,
                        RecoveryConsumable.FOCUS_DRAUGHT
                );

        var setup = RecoveryBeltSetupState.fromLoadedBelt(belt);

        assertEquals(2, setup.configuredCount());
        assertEquals(
                RecoveryConsumable.HEALING_POTION,
                setup.desiredConsumableAt(0).orElseThrow()
        );
        assertTrue(setup.desiredConsumableAt(1).isEmpty());
        assertEquals(
                RecoveryConsumable.FOCUS_DRAUGHT,
                setup.desiredConsumableAt(2).orElseThrow()
        );
        assertTrue(setup.desiredConsumableAt(3).isEmpty());
    }

    @Test
    void setupCodecRoundTrips() {
        var setup = RecoveryBeltSetupState.empty()
                .withSlot(0, RecoveryBeltSlot.HEALING_POTION)
                .withSlot(1, RecoveryBeltSlot.FOCUS_DRAUGHT)
                .withSlot(3, RecoveryBeltSlot.CLEANSING_TONIC);

        var encoded = RecoveryBeltSetupState.CODEC
                .encodeStart(JsonOps.INSTANCE, setup)
                .getOrThrow();
        var decoded = RecoveryBeltSetupState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(setup, decoded);
    }
}
