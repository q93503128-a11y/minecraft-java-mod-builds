package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HeroSkillVfxStyleTest {
    @Test
    void everyCanonicalCoreHeroSkillHasAnAuthoredStyle() {
        for (String heroId : List.of("P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08")) {
            var definition = CanonicalData.definition(heroId);
            assertEquals(3, definition.skills().size(), heroId + " must keep Basic + Active1 + Active2");
            definition.skills().forEach(skill -> assertNotEquals(
                    HeroSkillVfxStyle.Style.GENERIC,
                    HeroSkillVfxStyle.resolve(heroId, skill.id()),
                    heroId + " / " + skill.id() + " must not fall back to generic hero VFX"));
        }
    }

    @Test
    void staleLynetteSkillIdDoesNotMasqueradeAsCurrentCanon() {
        assertEquals(HeroSkillVfxStyle.Style.GENERIC,
                HeroSkillVfxStyle.resolve("P05", "p05_piercing_shot"));
        assertEquals(HeroSkillVfxStyle.Style.P05_PURSUIT_MARK,
                HeroSkillVfxStyle.resolve("P05", "p05_pursuit_mark"));
        assertEquals(HeroSkillVfxStyle.Style.P05_FINISHER,
                HeroSkillVfxStyle.resolve("P05", "p05_finisher"));
    }
}
