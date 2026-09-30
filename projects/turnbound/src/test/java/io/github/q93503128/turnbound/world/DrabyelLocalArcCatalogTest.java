package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DrabyelLocalArcCatalogTest {
    @Test void localTargetsStayInsideEarlyGameWalkingRing(){
        assertTrue(DrabyelLocalArcCatalog.validate().isEmpty(),()->String.join("; ",DrabyelLocalArcCatalog.validate()));
        assertEquals(3,DrabyelLocalArcCatalog.sites().size());
        for(var site:DrabyelLocalArcCatalog.sites()){
            double d=Math.hypot(site.seed().x()-530,site.seed().z()-1848);
            assertTrue(d>=64.0D&&d<=110.0D,site.locator()+" d="+d);
        }
    }
}
