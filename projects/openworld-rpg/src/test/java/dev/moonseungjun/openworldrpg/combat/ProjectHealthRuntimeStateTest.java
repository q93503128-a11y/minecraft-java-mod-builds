package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectHealthRuntimeState;
import org.junit.jupiter.api.Test;

class ProjectHealthRuntimeStateTest {
    @Test
    void canonicalBossHealthIsIndependentFromVanillaProxyRange() {
        var state = ProjectHealthRuntimeState.atFraction(4900.0, 1.0);

        var hit = state.applyDamage(32.0);
        assertEquals(32.0, hit.appliedDamage(), 0.0001);
        assertEquals(4868.0, hit.currentHealth(), 0.0001);
        assertEquals(4868.0 / 4900.0, hit.fraction(), 0.0000001);
        assertFalse(hit.killed());
    }

    @Test
    void reloadFractionRestoresCanonicalHealthWithoutInventingExtraHp() {
        var restored = ProjectHealthRuntimeState.atFraction(4900.0, 0.50);
        assertEquals(2450.0, restored.snapshot().currentHealth(), 0.0001);
    }

    @Test
    void lethalDamageClampsAtRemainingCanonicalHealth() {
        var state = ProjectHealthRuntimeState.atFraction(4900.0, 0.10);

        var lethal = state.applyDamage(9999.0);
        assertEquals(490.0, lethal.appliedDamage(), 0.0001);
        assertEquals(0.0, lethal.currentHealth(), 0.0001);
        assertTrue(lethal.killed());
    }
}
