package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectPoiseRuntimeState;
import org.junit.jupiter.api.Test;

class ProjectPoiseRuntimeStateTest {
    @Test
    void bossPoiseRecoversAfterSixSecondDelayAtCanonicalRate() {
        var state = ProjectPoiseRuntimeState.boss(190.0, 0);

        var hit = state.apply(5.0, 0);
        assertEquals(185.0, hit.remainingPoise(), 0.0001);
        assertFalse(hit.breakTriggered());

        assertEquals(185.0, state.snapshot(120).currentPoise(), 0.0001);
        assertEquals(190.0, state.snapshot(140).currentPoise(), 0.0001);
    }

    @Test
    void bossBreakAppliesDamageWindowThenPostBreakPoiseProtection() {
        var state = ProjectPoiseRuntimeState.boss(190.0, 0);

        var broken = state.apply(190.0, 0);
        assertTrue(broken.breakTriggered());
        assertTrue(state.snapshot(47).broken());
        assertEquals(1.15, state.snapshot(47).damageTakenMultiplier(), 0.0001);

        var recovered = state.snapshot(48);
        assertFalse(recovered.broken());
        assertEquals(190.0, recovered.currentPoise(), 0.0001);
        assertEquals(0.50, recovered.poiseTakenMultiplier(), 0.0001);

        var protectedHit = state.apply(10.0, 50);
        assertEquals(5.0, protectedHit.effectivePoiseDamage(), 0.0001);
        assertEquals(185.0, protectedHit.remainingPoise(), 0.0001);

        assertEquals(1.0, state.snapshot(78).poiseTakenMultiplier(), 0.0001);
    }

    @Test
    void poiseDamageDuringBreakDoesNotExtendOrStackBreak() {
        var state = ProjectPoiseRuntimeState.boss(190.0, 0);
        state.apply(190.0, 0);

        var duringBreak = state.apply(500.0, 20);
        assertEquals(0.0, duringBreak.effectivePoiseDamage(), 0.0001);
        assertFalse(duringBreak.breakTriggered());
        assertTrue(duringBreak.broken());

        assertFalse(state.snapshot(48).broken());
        assertEquals(190.0, state.snapshot(48).currentPoise(), 0.0001);
    }
}
