package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ClericGraceRuntimeState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClericGraceRuntimeStateTest {
    @Test
    void healingNeedsSixPercentAndUsesPerRecipientTwoSecondIcd() {
        var state = new ClericGraceRuntimeState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        var tooSmall = state.recordEffectiveHeal(
                first,
                5.99,
                100.0,
                100L,
                100L
        );
        assertFalse(tooSmall.thresholdQualified());
        assertEquals(0, tooSmall.currentPips());

        var firstGain = state.recordEffectiveHeal(
                first,
                6.0,
                100.0,
                101L,
                101L
        );
        assertTrue(firstGain.pipAdded());
        assertEquals(1, firstGain.currentPips());

        var blocked = state.recordEffectiveHeal(
                first,
                20.0,
                100.0,
                120L,
                120L
        );
        assertTrue(blocked.icdBlocked());
        assertEquals(1, blocked.currentPips());

        var otherTarget = state.recordEffectiveHeal(
                second,
                6.0,
                100.0,
                120L,
                120L
        );
        assertTrue(otherTarget.pipAdded());
        assertEquals(2, otherTarget.currentPips());

        var readyAgain = state.recordEffectiveHeal(
                first,
                6.0,
                100.0,
                141L,
                141L
        );
        assertEquals(3, readyAgain.currentPips());
    }

    @Test
    void damageBarrierAndHealCanFillThenSpenderConsumesAll() {
        var state = new ClericGraceRuntimeState();
        UUID target = UUID.randomUUID();

        assertEquals(
                1,
                state.recordDamagingActiveHit(10L, 10L)
                        .currentPips()
        );
        assertTrue(
                state.recordDamagingActiveHit(20L, 20L)
                        .icdBlocked()
        );
        assertEquals(
                2,
                state.recordConsumedBarrier(
                        target,
                        12.0,
                        200.0,
                        31L,
                        31L
                ).currentPips()
        );
        assertEquals(
                3,
                state.recordEffectiveHeal(
                        target,
                        12.0,
                        200.0,
                        32L,
                        32L
                ).currentPips()
        );

        assertTrue(state.consumeForSpender(33L, 33L));
        assertEquals(0, state.pips(33L, 33L));
        assertFalse(state.consumeForSpender(34L, 34L));
    }

    @Test
    void combatActivityExtendsGraceButOutOfCombatExpiresAtTenSeconds() {
        var state = new ClericGraceRuntimeState();
        state.recordDamagingActiveHit(100L, 100L);

        assertEquals(1, state.pips(299L, 250L));
        assertEquals(1, state.pips(449L, 250L));
        assertEquals(0, state.pips(450L, 250L));
    }

    @Test
    void qualifyingEventsAtMaxRefreshGraceWithoutExceedingThree() {
        var state = new ClericGraceRuntimeState();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        state.recordEffectiveHeal(a, 6, 100, 1L, 1L);
        state.recordEffectiveHeal(b, 6, 100, 2L, 2L);
        state.recordEffectiveHeal(c, 6, 100, 3L, 3L);
        var capped = state.recordEffectiveHeal(d, 6, 100, 100L, 3L);

        assertFalse(capped.pipAdded());
        assertTrue(capped.thresholdQualified());
        assertEquals(3, capped.currentPips());
        assertEquals(3, state.pips(299L, 3L));
        assertEquals(0, state.pips(300L, 3L));
    }

    @Test
    void lingeringGraceAddsExactExpiryTime() {
        var state = new ClericGraceRuntimeState();
        state.synchronizeExpiryBonusTicks(60L);
        state.recordDamagingActiveHit(100L, 100L);

        assertEquals(1, state.pips(359L, 100L));
        assertEquals(0, state.pips(360L, 100L));
    }
}
