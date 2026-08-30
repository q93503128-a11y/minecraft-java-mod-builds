package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterMenuCatalogTest {
    @Test
    void v04MenuProfilesCoverExactlyTheTwelvePlayableCharacters() {
        Set<String> expected = Set.of("P01","P02","P03","P04","P05","P06","P07","P08","F01","F02","F03","F04");
        Set<String> actual = CharacterMenuCatalog.all().stream().map(CharacterMenuCatalog.Profile::id).collect(java.util.stream.Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void coreEightUseCharacterQuestProfileLocksButMaterialCharactersDoNot() {
        for (String id : Set.of("P01","P02","P03","P04","P05","P06","P07","P08")) assertTrue(CharacterMenuCatalog.profile(id).profileQuest());
        for (String id : Set.of("F01","F02","F03","F04")) assertFalse(CharacterMenuCatalog.profile(id).profileQuest());
    }

    @Test
    void fillerAwakeningSummariesStayCanonical() {
        assertEquals("집중 사격 사용 후 Gauge +80.", CharacterMenuCatalog.profile("F03").awakening());
        assertEquals("버티기 사용 시 MaxHP 5% Barrier 추가.", CharacterMenuCatalog.profile("F04").awakening());
    }
}
