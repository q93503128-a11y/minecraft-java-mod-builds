package dev.moonseungjun.openworldrpg.gathering;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingData;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Binds authored R01 gathering node identities to the validated Azari spatial dataset.
 *
 * <p>Candidate anchors are inspectable for planning, but gameplay may resolve only production
 * anchors through {@link #productionNode(String)}.</p>
 */
public final class R01GatheringSpatialRegistry {
    private static final String PREFIX = "openworld_rpg:r01/gathering/";

    private R01GatheringSpatialRegistry() {
    }

    public static List<BoundNode> allAuthoredNodes() {
        List<BoundNode> nodes = new ArrayList<>();
        for (R01SpatialBindingData.Anchor anchor
                : R01SpatialBindingRegistry.data().anchors()) {
            if (!anchor.id().startsWith(PREFIX)) {
                continue;
            }
            nodes.add(new BoundNode(
                    anchor.id(),
                    resourceIdForNode(anchor.id()),
                    anchor
            ));
        }
        nodes.sort(Comparator.comparing(BoundNode::nodeId));
        return List.copyOf(nodes);
    }

    public static Optional<BoundNode> node(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (!nodeId.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return R01SpatialBindingRegistry.data()
                .anchor(nodeId)
                .map(anchor -> new BoundNode(
                        nodeId,
                        resourceIdForNode(nodeId),
                        anchor
                ));
    }

    public static Optional<BoundNode> productionNode(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (!nodeId.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return R01SpatialBindingRegistry.productionAnchor(nodeId)
                .map(anchor -> new BoundNode(
                        nodeId,
                        resourceIdForNode(nodeId),
                        anchor
                ));
    }

    private static String resourceIdForNode(String nodeId) {
        int separator = nodeId.lastIndexOf('/');
        if (separator < 0 || separator == nodeId.length() - 1) {
            throw new IllegalArgumentException(
                    "Malformed R01 gathering node id: " + nodeId
            );
        }
        String leaf = nodeId.substring(separator + 1);
        String resourceId;
        if (leaf.startsWith("iron_ore_")) {
            resourceId = R01GatheringRules.IRON_ORE;
        } else if (leaf.startsWith("hardwood_")) {
            resourceId = R01GatheringRules.HARDWOOD;
        } else if (leaf.startsWith("healing_herb_")) {
            resourceId = R01GatheringRules.HEALING_HERB;
        } else if (leaf.startsWith("verdant_crystal_")) {
            resourceId = R01GatheringRules.VERDANT_CRYSTAL;
        } else {
            throw new IllegalArgumentException(
                    "Unknown R01 gathering resource family in node id: " + nodeId
            );
        }
        R01GatheringRules.requireResource(resourceId);
        return resourceId;
    }

    public record BoundNode(
            String nodeId,
            String resourceId,
            R01SpatialBindingData.Anchor anchor
    ) {
        public BoundNode {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(resourceId, "resourceId");
            Objects.requireNonNull(anchor, "anchor");
            if (!nodeId.equals(anchor.id())) {
                throw new IllegalArgumentException(
                        "Gathering node id must match its spatial anchor id."
                );
            }
        }

        public boolean production() {
            return anchor.production();
        }
    }
}
