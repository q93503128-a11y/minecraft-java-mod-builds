package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerHealingAuthority;
import org.junit.jupiter.api.Test;

class PlayerHealingAuthorityTest {
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
