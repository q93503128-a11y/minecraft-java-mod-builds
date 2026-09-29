package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import org.junit.jupiter.api.Test;

class PlayerBarrierAuthorityTest {
    private static final double EPSILON = 0.0001;

    @Test
    void barrierReferenceUsesSixtyFiveWillThirtyFiveEnd() {
        double weighted = 0.65 * 15.0 + 0.35 * 5.0;
        double expected = ProjectCombatRules.baseHp(1)
                * ProjectCombatRules.attributeDamageMultiplier(weighted);
        assertEquals(
                expected,
                PlayerBarrierAuthority.barrierReference(
                        1,
                        15.0,
                        5.0
                ),
                EPSILON
        );
    }

    @Test
    void skillBarrierAppliesCoefficientThenOutputBonus() {
        assertEquals(
                33.0,
                PlayerBarrierAuthority.skillBarrierAmount(
                        100.0,
                        0.30,
                        0.10
                ),
                EPSILON
        );
        assertEquals(
                120,
                PlayerBarrierAuthority.DEFAULT_BARRIER_DURATION_TICKS
        );
        assertEquals(
                0.40,
                PlayerBarrierAuthority
                        .DIFFERENT_SOURCE_STACK_CAP_MAX_HP_FRACTION,
                EPSILON
        );
    }
}
