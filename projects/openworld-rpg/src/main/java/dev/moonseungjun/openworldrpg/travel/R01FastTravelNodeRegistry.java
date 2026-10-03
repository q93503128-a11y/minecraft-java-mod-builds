package dev.moonseungjun.openworldrpg.travel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/**
 * Server-owned production bindings for fast-travel nodes.
 *
 * <p>This registry intentionally does not read R01 spatial <em>candidate</em> coordinates. A world
 * binder may install nodes only after their coordinates/arrival points are promoted to accepted
 * production bindings. That keeps the backend usable without accidentally turning review probes into
 * live teleport destinations.</p>
 */
public final class R01FastTravelNodeRegistry {
    public static final int MAX_ARRIVAL_POINTS = 5;

    private static volatile Map<String, ProductionNode> nodes = Map.of();

    private R01FastTravelNodeRegistry() {
    }

    public static Optional<ProductionNode> node(String nodeId) {
        requireStableId(nodeId);
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public static synchronized void installAcceptedBindings(
            Collection<ProductionNode> acceptedNodes
    ) {
        Objects.requireNonNull(acceptedNodes, "acceptedNodes");
        Map<String, ProductionNode> next = new LinkedHashMap<>();
        for (ProductionNode node : acceptedNodes) {
            Objects.requireNonNull(node, "node");
            ProductionNode previous = next.put(node.id(), node);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate fast-travel node id: " + node.id()
                );
            }
        }
        nodes = Map.copyOf(next);
    }

    public static int productionNodeCount() {
        return nodes.size();
    }

    public enum PersonalUnlock {
        ALDERFORD_GATE_SHRINE,
        QUARRY_WAYSTONE
    }

    public record ProductionNode(
            String id,
            Vec3 interactionAnchor,
            List<Vec3> arrivalPoints,
            PersonalUnlock personalUnlock
    ) {
        public ProductionNode {
            requireStableId(id);
            interactionAnchor = requireFinite(
                    Objects.requireNonNull(
                            interactionAnchor,
                            "interactionAnchor"
                    )
            );
            arrivalPoints = List.copyOf(
                    Objects.requireNonNull(
                            arrivalPoints,
                            "arrivalPoints"
                    )
            );
            if (arrivalPoints.isEmpty()
                    || arrivalPoints.size() > MAX_ARRIVAL_POINTS) {
                throw new IllegalArgumentException(
                        "Fast-travel node requires one primary arrival and at most four fallbacks."
                );
            }
            arrivalPoints.forEach(R01FastTravelNodeRegistry::requireFinite);
            personalUnlock = Objects.requireNonNull(
                    personalUnlock,
                    "personalUnlock"
            );
        }
    }

    private static Vec3 requireFinite(Vec3 value) {
        if (!Double.isFinite(value.x())
                || !Double.isFinite(value.y())
                || !Double.isFinite(value.z())) {
            throw new IllegalArgumentException(
                    "Fast-travel coordinates must be finite."
            );
        }
        return value;
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected namespaced fast-travel node id."
            );
        }
    }
}
