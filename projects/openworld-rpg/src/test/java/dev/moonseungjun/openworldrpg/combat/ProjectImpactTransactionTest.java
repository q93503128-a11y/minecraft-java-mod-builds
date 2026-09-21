package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectImpactTransactionTest {
    @Test
    void directMagicDamageUsesCanonicalStatDefenseAndMitigationOrder() {
        var source = new ProjectImpactTransaction.DamageSourceSnapshot(8, 30.0, 20.0, 0.0, 1.0);
        var target = new ProjectImpactTransaction.DamageTargetSnapshot(45.0, 35.0, 1.0, 0.0, 190.0);

        var result = ProjectImpactTransaction.resolveDirectDamage(
                new ProjectImpactTransaction.DirectDamageRequest(
                        source,
                        target,
                        ProjectImpactTransaction.DamageSchool.MAGIC,
                        1.20,
                        1.0,
                        1.0
                )
        );

        assertEquals(42.48, result.rawActionDamage(), 0.0001);
        assertEquals(42.48, result.preMitigationDamage(), 0.0001);
        assertEquals(0.7479747974, result.armorTakenMultiplier(), 0.0000001);
        assertEquals(32.0, result.finalDamage());
    }

    @Test
    void mitigationCapsRespectSeventyAndEightyPercentCanon() {
        assertEquals(
                0.30,
                ProjectCombatRules.defenseTakenMultiplier(8, 1_000_000.0),
                0.0000001
        );
        assertEquals(
                0.20,
                ProjectCombatRules.routineTakenMultiplier(0.30, 1.0, 0.90),
                0.0000001
        );
    }

    @Test
    void healingDiscardsOverhealAndReportsEffectiveAmount() {
        var result = ProjectImpactTransaction.resolveHealing(
                new ProjectImpactTransaction.HealingRequest(
                        8,
                        20.0,
                        0.30,
                        0.10,
                        0.20,
                        50.0
                )
        );

        assertEquals(65.0, result.rawHeal());
        assertEquals(50.0, result.effectiveHeal());
        assertEquals(15.0, result.overheal());
    }

    @Test
    void strongestStatusKeepsMagnitudeAndRefreshesDuration() {
        var current = new ProjectImpactTransaction.StatusState(
                "openworld_rpg:burning",
                "source:a",
                10.0,
                20
        );

        var result = ProjectImpactTransaction.resolveStatus(
                Optional.of(current),
                new ProjectImpactTransaction.StatusApplicationRequest(
                        "openworld_rpg:burning",
                        "source:b",
                        8.0,
                        100,
                        ProjectImpactTransaction.StatusMergeRule.STRONGEST_MAGNITUDE_REFRESH
                )
        );

        assertEquals(10.0, result.state().magnitude());
        assertEquals(100, result.state().remainingTicks());
        assertFalse(result.magnitudeChanged());
        assertTrue(result.durationRefreshed());
    }

    @Test
    void poisePressureCanBreakWithoutBorrowingDamageCritScaling() {
        var result = ProjectImpactTransaction.resolvePoise(
                new ProjectImpactTransaction.PoiseRequest(
                        4.0,
                        190.0,
                        1.0,
                        0.50,
                        1.0,
                        1.0
                )
        );

        assertEquals(5.0, result.poiseDamage());
        assertEquals(0.0, result.remainingPoise());
        assertTrue(result.broken());
    }
}
