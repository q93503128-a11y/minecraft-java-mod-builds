package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HeroSkillAudioStyleTest {
    @Test
    void everyCanonicalCoreHeroSkillHasACharacterAudioStyle() {
        Set<String> heroCues = new HashSet<>();
        for (String heroId : List.of("P01","P02","P03","P04","P05","P06","P07","P08")) {
            var definition = CanonicalData.definition(heroId);
            assertEquals(3, definition.skills().size(), heroId);
            String cue = null;
            for (var skill : definition.skills()) {
                var style = HeroSkillAudioStyle.resolve(heroId, skill.id());
                assertNotNull(style, heroId + " / " + skill.id());
                assertEquals(style, HeroSkillAudioStyle.resolveCue(style.cueId(), skill.id()));
                if (cue == null) cue = style.cueId();
                else assertEquals(cue, style.cueId(), heroId + " keeps one coherent timbre family");
            }
            assertTrue(heroCues.add(cue), heroId + " must not share its action timbre with another core hero");
        }
    }

    @Test
    void unknownAndCrossHeroPairsDoNotMasqueradeAsCoreAudio() {
        assertNull(HeroSkillAudioStyle.resolve("P01", "p08_frenzy"));
        assertNull(HeroSkillAudioStyle.resolve("E001", "e001_basic"));
        assertNull(HeroSkillAudioStyle.resolveCue("hero_kyren", "p02_accelerate"));
    }
}
