package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerWeaponFieldImpactProfileTest {
    @Test
    void reachCommitmentStaysLongerThanMobilePressureDuringFieldCalibration() {
        var mobile = PlayerWeaponFieldImpactProfile.find(
            ContentId.parse("riftfrontier:attack/player/mobile_pressure_entry")
        ).orElseThrow();
        var finisher = PlayerWeaponFieldImpactProfile.find(
            ContentId.parse("riftfrontier:attack/player/mobile_pressure_finisher")
        ).orElseThrow();
        var reach = PlayerWeaponFieldImpactProfile.find(
            ContentId.parse("riftfrontier:attack/player/reach_commitment_strike")
        ).orElseThrow();

        assertEquals(PlayerWeaponFieldImpactProfile.Shape.FORWARD_ARC, mobile.shape());
        assertEquals(PlayerWeaponFieldImpactProfile.Shape.FORWARD_LANE, finisher.shape());
        assertEquals(PlayerWeaponFieldImpactProfile.Shape.FORWARD_LANE, reach.shape());
        assertTrue(mobile.reach() < finisher.reach());
        assertTrue(finisher.reach() < reach.reach());
        assertEquals(1.0F, mobile.diagnosticDamage());
        assertEquals(1.0F, finisher.diagnosticDamage());
        assertEquals(1.0F, reach.diagnosticDamage());
    }

    @Test
    void unrelatedAttackHasNoFieldImpactProfile() {
        assertTrue(PlayerWeaponFieldImpactProfile.find(
            ContentId.parse("riftfrontier:attack/boss/region_01/committed_strike")
        ).isEmpty());
    }
}
