package io.github.q93503128.turnbound.content;

import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V1CharacterStatGrowthTest {
    @Test
    void formalHeroLevelGrowthHitsDataDrivenV1TargetAndIgnoresLegacyPromotedStars() {
        var levelOne = CanonicalData.definition("P01", 1, 4, false).stats();
        var levelSixty = CanonicalData.definition("P01", 60, 4, false).stats();
        var legacyStarSix = CanonicalData.definition("P01", 60, 6, false).stats();

        assertEquals((int)Math.floor(levelOne.maxHp() * GrowthRulesV1.characterLevelMultiplier(60)), levelSixty.maxHp());
        assertEquals((int)Math.floor(levelOne.attack() * GrowthRulesV1.characterLevelMultiplier(60)), levelSixty.attack());
        assertEquals((int)Math.floor(levelOne.defense() * GrowthRulesV1.characterLevelMultiplier(60)), levelSixty.defense());
        assertEquals(levelSixty, legacyStarSix);
        assertEquals(levelOne.speed(), levelSixty.speed(), "SPD must not grow with level");
    }
}
