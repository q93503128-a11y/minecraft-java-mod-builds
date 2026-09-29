package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerHealingAuthority;
import org.junit.jupiter.api.Test;

class PlayerHealingAuthorityTest {
    @Test
    void skillHealingUsesCanonicalReferenceDoneThenReceivedOrder() {
        double reference = PlayerHealingAuthority.healingReference(
                8,
                15.0,
                5.0
        );
        double expectedReference =
                dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                        .baseHp(8)
                * dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                        .attributeDamageMultiplier(13.0);
        assertEquals(expectedReference, reference, 0.0001);
        assertEquals(
                reference * 0.30 * 1.40 * 1.40,
                PlayerHealingAuthority.skillHealingAmount(
                        reference,
                        0.30,
                        0.40,
                        0.40
                ),
                0.0001
        );
    }

    @Test
    void healingReceivedAppliesAfterAuthoredBaseAmount() {
        assertEquals(
                49.0,
                PlayerHealingAuthority.receivedHealingAmount(35.0, 0.40),
                0.0001
        );
        assertEquals(
                35.0,
                PlayerHealingAuthority.receivedHealingAmount(35.0, 0.0),
                0.0001
        );
    }

    @Test
    void skillHealingRejectsHealingDoneBeyondCanonicalCap() {
        double reference = PlayerHealingAuthority.healingReference(
                8,
                10.0,
                10.0
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHealingAuthority.skillHealingAmount(
                        reference,
                        0.30,
                        0.401,
                        0.0
                )
        );
    }

    @Test
    void healingReceivedRejectsValuesOutsideCanonicalGearCap() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHealingAuthority.receivedHealingAmount(35.0, 0.401)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHealingAuthority.receivedHealingAmount(-1.0, 0.0)
        );
    }
}
