package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M1DamageServiceTest {
    @Test void sameSeedAndRequestsProduceIdenticalDamageStream() {
        var left = new DeterministicBattleRng(20260906L);
        var right = new DeterministicBattleRng(20260906L);
        var request = DamageService.DamageRequest.standard(
                DamageTag.MELEE, AffinityGrade.WEAK, 100, 10, 100, 100, false);

        for (int i = 0; i < 50; i++) {
            var a = DamageService.resolve(request, left);
            var b = DamageService.resolve(request, right);
            assertEquals(a, b);
            assertEquals(a.breakdown(), b.breakdown());
        }
        assertEquals(100L, left.draws());
        assertEquals(left.draws(), right.draws());
    }

    @Test void canonicalAffinityAndImmuneContractsAreApplied() {
        var weak = DamageService.resolve(noCrit(AffinityGrade.WEAK, false), new DeterministicBattleRng(11L));
        var normal = DamageService.resolve(noCrit(AffinityGrade.NORMAL, false), new DeterministicBattleRng(11L));
        var resist = DamageService.resolve(noCrit(AffinityGrade.RESIST, false), new DeterministicBattleRng(11L));
        var immune = DamageService.resolve(noCrit(AffinityGrade.IMMUNE, false), new DeterministicBattleRng(11L));

        assertEquals(1.25D, weak.affinityMultiplier());
        assertEquals(1.00D, normal.affinityMultiplier());
        assertEquals(0.75D, resist.affinityMultiplier());
        assertEquals(0.00D, immune.affinityMultiplier());
        assertEquals(15, weak.poiseDamage());
        assertEquals(10, normal.poiseDamage());
        assertEquals(7, resist.poiseDamage());
        assertEquals(0, immune.poiseDamage());
        assertEquals(0, immune.finalDamage());
        assertEquals(2L, immune.rngDrawsAfter());
    }

    @Test void exposedMultiplierIsCanonicalAndBreakdownIsReplayReadable() {
        var normal = DamageService.resolve(noCrit(AffinityGrade.NORMAL, false), new DeterministicBattleRng(99L));
        var exposed = DamageService.resolve(noCrit(AffinityGrade.NORMAL, true), new DeterministicBattleRng(99L));

        assertEquals(normal.variance(), exposed.variance());
        assertEquals(1.0D, normal.exposedMultiplier());
        assertEquals(1.2D, exposed.exposedMultiplier());
        assertTrue(exposed.finalDamage() >= normal.finalDamage());
        assertTrue(exposed.breakdown().contains("tag=FIRE"));
        assertTrue(exposed.breakdown().contains("affinity=NORMAL"));
        assertTrue(exposed.breakdown().contains("variance="));
        assertTrue(exposed.breakdown().contains("final="));
        assertTrue(exposed.breakdown().contains("poise="));
        assertTrue(exposed.breakdown().contains("rngDraws=2"));
    }

    @Test void everyCanonicalDamageTagIsRepresentable() {
        assertEquals(6, DamageTag.values().length);
        assertArrayEquals(new DamageTag[]{
                DamageTag.MELEE, DamageTag.PROJECTILE, DamageTag.FIRE,
                DamageTag.BLAST, DamageTag.ARCANE, DamageTag.VOID
        }, DamageTag.values());
    }

    @Test void requestRejectsInvalidDamageInputs() {
        assertThrows(IllegalArgumentException.class, () -> new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 100, 10, 100, 100, false,
                -0.01D, 1.5D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 100, 10, 100, 100, false,
                0.05D, 0.99D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 100, 10, 100, 100, false,
                0.05D, 1.5D, Double.NaN));
    }

    private static DamageService.DamageRequest noCrit(AffinityGrade affinity, boolean exposed) {
        return new DamageService.DamageRequest(
                DamageTag.FIRE, affinity, 100, 10, 100, 100, exposed,
                0.0D, 1.5D, 1.0D);
    }
}
