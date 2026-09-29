package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import org.junit.jupiter.api.Test;

class ProjectPlayerIncomingDamageRuntimeTest {
    @Test
    void rebukedScalingChangesOnlyRawDirectDamage() {
        var hit = PlayerDefenseAuthority.IncomingHit.baseline(
                100.0,
                ProjectImpactTransaction.DamageSchool.MAGIC,
                8,
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                true,
                true,
                true
        );

        var scaled = ProjectPlayerIncomingDamageRuntime.scaleDirectDamage(
                hit,
                0.92
        );

        assertEquals(92.0, scaled.rawDamage(), 0.0001);
        assertEquals(hit.school(), scaled.school());
        assertEquals(hit.attackerLevel(), scaled.attackerLevel());
        assertEquals(hit.guardPressure(), scaled.guardPressure());
        assertEquals(hit.dodgeable(), scaled.dodgeable());
        assertEquals(hit.guardable(), scaled.guardable());
        assertEquals(hit.perfectGuardable(), scaled.perfectGuardable());
        assertEquals(
                hit.authoredDamageTakenMultiplier(),
                scaled.authoredDamageTakenMultiplier(),
                0.0001
        );
        assertEquals(
                hit.authoredDamageReduction(),
                scaled.authoredDamageReduction(),
                0.0001
        );
    }

    @Test
    void neutralMultiplierKeepsOriginalHitIdentity() {
        var hit = PlayerDefenseAuthority.IncomingHit.unguardable(
                50.0,
                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                8,
                true
        );
        assertSame(
                hit,
                ProjectPlayerIncomingDamageRuntime.scaleDirectDamage(
                        hit,
                        1.0
                )
        );
    }
}
