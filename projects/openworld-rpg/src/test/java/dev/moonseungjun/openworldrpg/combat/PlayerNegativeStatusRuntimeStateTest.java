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
}
