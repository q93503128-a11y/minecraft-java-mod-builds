package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import org.junit.jupiter.api.Test;

class CombatDamageAuthorityTest {
    @Test
    void acceptsFinitePositiveBetterCombatProposalExactlyOnceAtM0Boundary() {
        CombatDamageAuthority.DamageDecision decision =
                CombatDamageAuthority.authorizeBetterCombatMelee(7.25F, 2);

        assertTrue(decision.accepted());
        assertEquals(7.25F, decision.amount());
    }

    @Test
    void rejectsInvalidOrInactiveBetterCombatDamage() {
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(5.0F, -1).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(0.0F, 0).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(-1.0F, 0).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(Float.NaN, 0).accepted());
        assertFalse(CombatDamageAuthority.authorizeBetterCombatMelee(Float.POSITIVE_INFINITY, 0).accepted());
    }
}
