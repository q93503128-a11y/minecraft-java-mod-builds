package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class M4ProgressionRulesTest {
    @Test
    void levelCapsAndProductionTuningFollowCanon() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        ProgressionDefinition tuning = registry.progressions().get("turnbound_re:default_progression");
        assertNotNull(tuning);
        assertEquals(12, tuning.partyCapacity());
        assertArrayEquals(new int[]{20, 30, 40, 50, 60, 70},
                new int[]{
                        ProgressionRules.levelCap(1), ProgressionRules.levelCap(2), ProgressionRules.levelCap(3),
                        ProgressionRules.levelCap(4), ProgressionRules.levelCap(5), ProgressionRules.levelCap(6)});
        assertEquals(10, ProgressionRules.unlockShardCost(tuning, 2));
        assertTrue(ProgressionRules.ascensionCost(tuning, 6).coin() > ProgressionRules.ascensionCost(tuning, 5).coin());
    }

    @Test
    void visibleBaseGrowthAndCurrentAscensionFlatProduceExactStats() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        CharacterDefinition zombie = registry.characters().get("turnbound_re:zombie");

        var star2 = new CharacterProgress(zombie.id(), 2, 2, 30);
        assertEquals(new CharacterDefinition.Stats(372, 92, 80, 28, 114), ProgressionRules.stats(zombie, star2));

        var star3 = new CharacterProgress(zombie.id(), 2, 3, 30);
        assertEquals(new CharacterDefinition.Stats(390, 96, 84, 29, 119), ProgressionRules.stats(zombie, star3));
    }

    @Test
    void originStarCannotBeReinterpretedToCreateHiddenLowStarGrowth() throws IOException {
        CharacterDefinition zombie = ProductionDefinitionFixture.load().registry().characters().get("turnbound_re:zombie");
        var forged = new CharacterProgress(zombie.id(), 3, 3, 1);
        assertThrows(IllegalArgumentException.class, () -> ProgressionRules.stats(zombie, forged));
    }
}
