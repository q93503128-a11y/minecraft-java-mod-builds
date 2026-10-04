package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectThreatRules;
import org.junit.jupiter.api.Test;

class ProjectThreatRulesTest {
    @Test
    void damageHealingAndBarrierUseCanonicalMaxHpRatios() {
        assertEquals(
                10.0,
                ProjectThreatRules.damageThreat(100.0, 1000.0),
                0.000001
        );
        assertEquals(
                7.0,
                ProjectThreatRules.effectiveHealingThreat(200.0, 1000.0),
                0.000001
        );
        assertEquals(
                5.0,
                ProjectThreatRules.effectiveBarrierThreat(200.0, 1000.0),
                0.000001
        );
    }

    @Test
    void perfectGuardAddsFlatFourAfterPreventedDamageThreat() {
        assertEquals(
                5.0,
                ProjectThreatRules.guardThreat(250.0, 1000.0, false),
                0.000001
        );
        assertEquals(
                9.0,
                ProjectThreatRules.guardThreat(250.0, 1000.0, true),
                0.000001
        );
        assertEquals(
                4.0,
                ProjectThreatRules.guardThreat(0.0, 1000.0, true),
                0.000001
        );
    }

    @Test
    void invalidAmountsAndMaxHpFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectThreatRules.damageThreat(-1.0, 100.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectThreatRules.effectiveHealingThreat(1.0, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectThreatRules.effectiveBarrierThreat(
                        Double.NaN,
                        100.0
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectThreatRules.guardThreat(
                        1.0,
                        Double.POSITIVE_INFINITY,
                        false
                )
        );
    }
}
