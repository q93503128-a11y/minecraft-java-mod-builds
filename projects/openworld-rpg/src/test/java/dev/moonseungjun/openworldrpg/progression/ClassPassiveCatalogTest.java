package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class ClassPassiveCatalogTest {
    @Test
    void bundledCatalogMatchesCanonicalRootAndBranchShape() {
        var catalog = ClassPassiveCatalog.bundled();
        assertEquals(115, catalog.size());
        for (RootClass rootClass : RootClass.values()) {
            var root = catalog.rootNodes(rootClass);
            assertEquals(7, root.size());
            assertEquals(17, root.stream().mapToInt(ClassPassiveNodeSpec::maxRank).sum());
        }
        for (ClassSpecialization specialization : ClassSpecialization.values()) {
            var branch = catalog.branchNodes(specialization);
            assertEquals(8, branch.size());
            assertEquals(18, branch.stream().mapToInt(ClassPassiveNodeSpec::maxRank).sum());
            assertEquals(2L, branch.stream().filter(n -> n.tier() == ClassPassiveTier.I).count());
            assertEquals(2L, branch.stream().filter(n -> n.tier() == ClassPassiveTier.II).count());
            assertEquals(2L, branch.stream().filter(n -> n.tier() == ClassPassiveTier.III).count());
            assertEquals(2L, branch.stream().filter(n -> n.tier() == ClassPassiveTier.CAPSTONE).count());
        }
        for (ClassInsight insight : ClassInsight.values()) {
            assertTrue(ClassInsight.forRoot(insight.rootClass()).contains(insight));
        }
        for (RootClass rootClass : RootClass.values()) {
            assertEquals(8, ClassInsight.forRoot(rootClass).size());
        }
    }
}
