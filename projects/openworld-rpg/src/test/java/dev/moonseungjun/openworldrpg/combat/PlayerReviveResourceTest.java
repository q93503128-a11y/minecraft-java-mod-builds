package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import org.junit.jupiter.api.Test;

class PlayerReviveResourceTest {
    @Test
    void reviveResourceResetUsesExactCanonicalFractions() {
        PlayerCombatState state = new PlayerCombatState(5, 0L);
        assertTrue(state.spendMana(80.0, 0L));
        assertTrue(state.spendStamina(90.0, 0L, 0L));

        state.setReviveResourceFractions(0.25, 0.50, 0L);

        assertEquals(25.0, state.mana(0L), 0.0001);
        assertEquals(50.0, state.stamina(0L), 0.0001);
    }
}
