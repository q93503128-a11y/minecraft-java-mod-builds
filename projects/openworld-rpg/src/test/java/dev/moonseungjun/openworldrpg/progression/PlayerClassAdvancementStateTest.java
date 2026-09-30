package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class PlayerClassAdvancementStateTest {
    @Test
    void firstSpecializationUnlockActivatesAndSiblingUnlockDoesNotReplaceIt() {
        var first = PlayerClassAdvancementState.initial()
                .unlockSpecialization(
                        ClassSpecialization.HUNTER_RANGER
                );
        var hunter = first.rootState(RootClass.HUNTER);

        assertEquals(
                ClassSpecialization.HUNTER_RANGER,
                hunter.active().orElseThrow()
        );
        assertTrue(
                hunter.isUnlocked(
                        ClassSpecialization.HUNTER_RANGER
                )
        );

        var sibling = first.unlockSpecialization(
                ClassSpecialization.HUNTER_MARKSMAN
        );
        var after = sibling.rootState(RootClass.HUNTER);
        assertEquals(
                ClassSpecialization.HUNTER_RANGER,
                after.active().orElseThrow()
        );
        assertTrue(
                after.isUnlocked(
                        ClassSpecialization.HUNTER_MARKSMAN
                )
        );
    }

    @Test
    void branchSwitchTransactionIsMonotonicAndReconnectSafeById() {
        var unlocked = PlayerClassAdvancementState.initial()
                .unlockSpecialization(
                        ClassSpecialization.MAGE_ELEMENTALIST
                )
                .unlockSpecialization(
                        ClassSpecialization.MAGE_ARCANIST
                );
        var prepared = unlocked.prepareBranchSwitch(
                ClassSpecialization.MAGE_ARCANIST,
                310L
        );
        var pending = prepared.pendingBranchSwitch()
                .orElseThrow();

        assertEquals(1L, pending.sequence());
        assertEquals(
                "openworld_rpg:branch_switch/1",
                pending.transactionId()
        );
        assertEquals(
                "openworld_rpg:branch_switch/1/gold",
                pending.debitTransactionId()
        );
        assertEquals(
                ClassSpecialization.MAGE_ELEMENTALIST,
                pending.source()
        );
        assertEquals(
                ClassSpecialization.MAGE_ARCANIST,
                pending.target()
        );

        var committed = prepared.commitBranchSwitch(
                pending.transactionId()
        );
        assertEquals(
                ClassSpecialization.MAGE_ARCANIST,
                committed.rootState(RootClass.MAGE)
                        .active()
                        .orElseThrow()
        );
        assertTrue(
                committed.pendingBranchSwitch().isEmpty()
        );

        var next = committed.prepareBranchSwitch(
                ClassSpecialization.MAGE_ELEMENTALIST,
                310L
        );
        assertEquals(2L, next.branchSwitchSequence());
    }

    @Test
    void wrongRootOrLockedBranchFailsClosed() {
        var badRoot = new RootClassSpecializationState(
                java.util.Set.of(
                        ClassSpecialization.HUNTER_RANGER
                ),
                java.util.Optional.of(
                        ClassSpecialization.HUNTER_RANGER
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerClassAdvancementState(
                        PlayerClassAdvancementState
                                .CURRENT_SCHEMA_VERSION,
                        badRoot,
                        RootClassSpecializationState.initial(),
                        RootClassSpecializationState.initial(),
                        RootClassSpecializationState.initial(),
                        RootClassSpecializationState.initial(),
                        0L,
                        java.util.Optional.empty()
                )
        );

        var first = PlayerClassAdvancementState.initial()
                .unlockSpecialization(
                        ClassSpecialization.GUARDIAN_BASTION
                );
        assertThrows(
                IllegalStateException.class,
                () -> first.prepareBranchSwitch(
                        ClassSpecialization.GUARDIAN_SENTINEL,
                        300L
                )
        );
    }

    @Test
    void stateSurvivesCodecRoundTrip() {
        var original = PlayerClassAdvancementState.initial()
                .unlockSpecialization(
                        ClassSpecialization.WARRIOR_VANGUARD
                )
                .unlockSpecialization(
                        ClassSpecialization.WARRIOR_ARMSMASTER
                )
                .prepareBranchSwitch(
                        ClassSpecialization.WARRIOR_ARMSMASTER,
                        770L
                );

        var encoded = PlayerClassAdvancementState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerClassAdvancementState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
