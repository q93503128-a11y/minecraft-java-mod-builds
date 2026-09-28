package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DrehmalFieldNpcCatalogTest {
    @Test void fieldNpcCatalogIsValid(){
        assertTrue(DrehmalFieldNpcCatalog.validate().isEmpty(),()->String.join("; ",DrehmalFieldNpcCatalog.validate()));
    }
    @Test void firstRouteHasThreePurposefulFieldNpcs(){
        assertEquals(3,DrehmalFieldNpcCatalog.all().size());
        assertTrue(DrehmalFieldNpcCatalog.all().stream().anyMatch(n->n.siteLocator().endsWith("/first_guide")));
        assertTrue(DrehmalFieldNpcCatalog.all().stream().anyMatch(n->n.siteLocator().endsWith("/tower_watch")));
        assertTrue(DrehmalFieldNpcCatalog.all().stream().anyMatch(n->n.siteLocator().endsWith("/camp_explorer")));
    }
}
