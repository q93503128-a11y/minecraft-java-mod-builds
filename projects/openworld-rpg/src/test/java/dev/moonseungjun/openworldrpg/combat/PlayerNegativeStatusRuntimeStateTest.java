package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerNegativeStatusRuntimeState;
import dev.moonseungjun.openworldrpg.recovery.RecoveryActionRules;
import dev.moonseungjun.openworldrpg.recovery.RecoveryEffectAuthority;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerNegativeStatusRuntimeStateTest {
    @Test
    void reconnectSnapshotKeepsOnlyUnexpiredStatusState() {
        var original = new PlayerNegativeStatusRuntimeState();
        original.applyStatus(
                "openworld_rpg:poison",
                Set.of(RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG),
                500L
        );
        original.applyNegativeBuildupResistance(200, 100L);
        var snapshot = original.persistentSnapshot(100L);

        var restored = new PlayerNegativeStatusRuntimeState();
        restored.restorePersistent(snapshot, 300L);
        assertTrue(restored.hasStatus("openworld_rpg:poison", 300L));
        assertEquals(
                1.0,
                restored.negativeBuildupReceivedMultiplier(300L),
                0.0001
        );

        var expired = new PlayerNegativeStatusRuntimeState();
        expired.restorePersistent(snapshot, 600L);
        assertFalse(expired.hasStatus("openworld_rpg:poison", 600L));
    }

    @Test
    void cleansingRemovesOnlyMinorDispellableAndAppliesTenSecondResistance() {
        var state = new PlayerNegativeStatusRuntimeState();
        state.applyStatus(
                "openworld_rpg:poison",
                Set.of(RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG),
                500L
        );
        state.applyStatus(
                "openworld_rpg:boss_mark",
                Set.of("major_scripted"),
                500L
        );

        assertEquals(
                1,
                state.cleanseTagged(
                        RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG,
                        100L
                )
        );
        assertFalse(state.hasStatus("openworld_rpg:poison", 100L));
        assertTrue(state.hasStatus("openworld_rpg:boss_mark", 100L));

        state.applyNegativeBuildupResistance(
                RecoveryActionRules.CLEANSING_BUILDUP_RESISTANCE_TICKS,
                100L
        );
        assertEquals(0.80, state.negativeBuildupReceivedMultiplier(299L), 0.0001);
        assertEquals(1.0, state.negativeBuildupReceivedMultiplier(300L), 0.0001);
    }
    @Test
    void singleCleanseRemovesExactlyOneEligibleStatusDeterministically() {
        var state = new PlayerNegativeStatusRuntimeState();
        state.applyStatus(
                "openworld_rpg:zeta_poison",
                Set.of(RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG),
                500L
        );
        state.applyStatus(
                "openworld_rpg:alpha_burn",
                Set.of(RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG),
                500L
        );

        assertEquals(
                1,
                state.cleanseOneTagged(
                        RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG,
                        100L
                )
        );
        assertFalse(
                state.hasStatus("openworld_rpg:alpha_burn", 100L)
        );
        assertTrue(
                state.hasStatus("openworld_rpg:zeta_poison", 100L)
        );
        assertEquals(1, state.activeStatusCount(100L));
    }

    @Test
    void sanctuaryStyleDurationModifierAffectsOnlyStatusesAppliedInsideWindow() {
        var state = new PlayerNegativeStatusRuntimeState();
        state.applyNegativeStatusDurationMultiplier(
                0.80,
                2L,
                100L
        );
        assertEquals(
                0.80,
                state.negativeStatusDurationMultiplier(100L),
                0.0001
        );

        state.applyStatusForDuration(
                "openworld_rpg:inside_mark",
                Set.of("minor_dispellable"),
                100L,
                100L
        );
        assertTrue(state.hasStatus("openworld_rpg:inside_mark", 179L));
        assertFalse(state.hasStatus("openworld_rpg:inside_mark", 180L));

        assertEquals(
                1.0,
                state.negativeStatusDurationMultiplier(102L),
                0.0001
        );
        state.applyStatusForDuration(
                "openworld_rpg:outside_mark",
                Set.of("minor_dispellable"),
                100L,
                102L
        );
        assertTrue(state.hasStatus("openworld_rpg:outside_mark", 201L));
        assertFalse(state.hasStatus("openworld_rpg:outside_mark", 202L));
    }

}
