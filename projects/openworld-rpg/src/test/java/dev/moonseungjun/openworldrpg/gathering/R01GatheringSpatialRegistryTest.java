package dev.moonseungjun.openworldrpg.gathering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class R01GatheringSpatialRegistryTest {
    @Test
    void bundledAzariSliceContainsAllFortySevenAuthoredGatheringNodes() {
        var nodes = R01GatheringSpatialRegistry.allAuthoredNodes();

        assertEquals(47, nodes.size());
        Map<String, Long> counts = nodes.stream().collect(
                Collectors.groupingBy(
                        R01GatheringSpatialRegistry.BoundNode::resourceId,
                        Collectors.counting()
                )
        );

        assertEquals(16L, counts.get(R01GatheringRules.HEALING_HERB));
        assertEquals(16L, counts.get(R01GatheringRules.HARDWOOD));
        assertEquals(11L, counts.get(R01GatheringRules.IRON_ORE));
        assertEquals(4L, counts.get(R01GatheringRules.VERDANT_CRYSTAL));
    }

    @Test
    void currentCandidateNodesCannotLeakIntoLiveGatheringAuthority() {
        for (var node : R01GatheringSpatialRegistry.allAuthoredNodes()) {
            assertTrue(
                    R01GatheringSpatialRegistry
                            .productionNode(node.nodeId())
                            .isEmpty()
            );
        }
    }

    @Test
    void representativeNodeKeepsCanonicalResourceIdentity() {
        var node = R01GatheringSpatialRegistry.node(
                "openworld_rpg:r01/gathering/rootshade_grove/verdant_crystal_01"
        ).orElseThrow();

        assertEquals(R01GatheringRules.VERDANT_CRYSTAL, node.resourceId());
        assertEquals("candidate", node.anchor().status());
    }
}
