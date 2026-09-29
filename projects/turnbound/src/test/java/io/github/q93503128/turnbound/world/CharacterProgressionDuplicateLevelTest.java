package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterProgressionDuplicateLevelTest {
    @Test
    void duplicateBonusStacksIndependentlyToPlusTen() {
        CharacterProgression.State state = new CharacterProgression.State(60, 0);
        for (int i = 0; i < 15; i++) state = state.grantDuplicateBonus();

        assertEquals(60, state.level());
        assertEquals(10, state.bonusLevel());
        assertEquals(70, state.effectiveLevel());
        assertEquals(GrowthRulesV1.effectiveMaxLevel(), state.effectiveLevel());
    }

    @Test
    void xpGrowthPreservesDuplicateBonusAndStopsBaseAtSixty() {
        CharacterProgression.State state = new CharacterProgression.State(20, 0, 3);
        CharacterProgression.Gain gain = CharacterProgression.gain(state, 5_000_000, 60);

        assertEquals(60, gain.after().level());
        assertEquals(3, gain.after().bonusLevel());
        assertEquals(63, gain.after().effectiveLevel());
        assertEquals(0, gain.after().xp());
    }
}
