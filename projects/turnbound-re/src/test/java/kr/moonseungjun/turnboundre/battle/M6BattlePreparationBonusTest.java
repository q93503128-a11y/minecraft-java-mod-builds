package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M6BattlePreparationBonusTest {
    private static final CharacterDefinition.Stats BASE = new CharacterDefinition.Stats(140, 32, 28, 18, 100);

    @Test
    void nonePreservesEveryStatExactly() {
        assertEquals(BASE, BattlePreparationBonus.NONE.apply(BASE));
        assertFalse(BattlePreparationBonus.NONE.active());
    }

    @Test
    void preparationScalesOnlyDeclaredStatsWithDeterministicIntegerMath() {
        var iron = new BattlePreparationBonus("iron", 0, 0, 8, 8).apply(BASE);
        assertEquals(140, iron.hp());
        assertEquals(32, iron.atk());
        assertEquals(30, iron.def());
        assertEquals(18, iron.spd());
        assertEquals(108, iron.poise());

        var food = new BattlePreparationBonus("food", 10, 0, 0, 0).apply(BASE);
        assertEquals(154, food.hp());
        assertEquals(32, food.atk());
        assertEquals(18, food.spd());

        var fish = new BattlePreparationBonus("fish", 0, 8, 0, 0).apply(BASE);
        assertEquals(34, fish.atk());
        assertEquals(28, fish.def());
        assertEquals(18, fish.spd());
    }

    @Test
    void preparationCannotSmuggleExtremeOrAnonymousBonuses() {
        assertThrows(IllegalArgumentException.class,
                () -> new BattlePreparationBonus("", 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BattlePreparationBonus("bad", 51, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BattlePreparationBonus("bad", 0, -1, 0, 0));
    }
}
