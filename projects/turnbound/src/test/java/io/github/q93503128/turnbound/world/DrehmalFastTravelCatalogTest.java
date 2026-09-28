package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DrehmalFastTravelCatalogTest {
    @Test void catalogIsValid(){
        assertTrue(DrehmalFastTravelCatalog.validate().isEmpty(),
                ()->String.join("; ",DrehmalFastTravelCatalog.validate()));
    }

    @Test void firstRouteUsesSparseDiscoveryNodes(){
        var nodes=DrehmalFastTravelCatalog.nodes();
        assertEquals(3,nodes.size());
        assertEquals("프라이멀 길머리",nodes.get(0).label());
        assertEquals("캐피털 밸리 탑",nodes.get(1).label());
        assertEquals("뉴 드라비엘",nodes.get(2).label());
        for(int i=1;i<nodes.size();i++){
            double distance=Math.hypot(nodes.get(i).mapX()-nodes.get(i-1).mapX(),nodes.get(i).mapZ()-nodes.get(i-1).mapZ());
            assertTrue(distance>=450.0,"travel nodes must not erase first-route traversal");
        }
    }
}
