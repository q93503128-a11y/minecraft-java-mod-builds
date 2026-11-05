package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class CombatMathTest {
    @Test void contentIndependentDamageKeepsPreviouslyVerifiedFormula() {
        assertEquals(100, CombatMath.calculateDamage(100, 0, 1.0));
        assertEquals(80, CombatMath.calculateDamage(100, 100, 1.0));
        assertEquals(50, CombatMath.calculateDamage(100, 400, 1.0));
        assertEquals(35, CombatMath.calculateDamage(100, 8000, 1.0));
        assertEquals(1, CombatMath.calculateDamage(0, 0, 1.0));
        assertEquals(16, CombatMath.calculateDamage(20, 20, 1.0));
        for (int attack : new int[]{0, 1, 8, 100, 300})
            for (int defense : new int[]{0, 5, 100, 1000})
                for (double potency : new double[]{0.0, 0.5, 1.0, 2.4})
                    assertEquals(CombatMath.calculateDamage(attack, defense, potency),
                            BattleEngine.calculateDamage(attack, defense, potency));
    }
}
