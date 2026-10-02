package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class R01CaveCentipedeDonorContactBridgeTest {
    @Test
    void onlyScuttleBiteIsBoundToThePinnedDonorMeleeContact() {
        assertTrue(R01CaveCentipedeDonorContactBridge.donorContactBound(
                R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE
        ));
        assertFalse(R01CaveCentipedeDonorContactBridge.donorContactBound(
                R01SecondaryCreatureEncounterData.ActionId.BODY_RAKE
        ));
        assertFalse(R01CaveCentipedeDonorContactBridge.donorContactBound(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP
        ));
    }

    @Test
    void scuttleBiteContactUsesLockedTellRecoveryAndDamageContract() {
        var bite = R01CaveCentipedeDonorContactBridge.scuttleBiteRule();

        assertEquals(7, bite.tellTicks());
        assertEquals(8, bite.recoveryTicks());
        assertEquals(2.3, bite.maximumRange(), 0.000001);
        assertEquals(0.11, bite.benchmarkDamageShare(), 0.000001);
        assertEquals(30.0, bite.poisonBuildup(), 0.000001);
        assertEquals(
                "alexsmobs:centipede_attack",
                R01CaveCentipedeDonorContactBridge.attackSoundId().toString()
        );
    }
}
