package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CharacterPassiveCatalogTest {
    @Test
    void allCoreHeroesExposePassiveDescriptions(){
        for(String id:new String[]{"P01","P02","P03","P04","P05","P06","P07","P08"}){
            var passives=CharacterPassiveCatalog.forOwner(id);
            assertFalse(passives.isEmpty(),id);
            assertFalse(passives.getFirst().description().isBlank(),id);
            assertEquals(3, CanonicalData.definition(id).skills().size(), id + " skill-slot contract");
        }
        assertEquals(2,CharacterPassiveCatalog.forOwner("P06").size());
    }
}
