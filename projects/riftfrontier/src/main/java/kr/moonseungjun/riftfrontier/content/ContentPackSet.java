package kr.moonseungjun.riftfrontier.content;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Merges independently decoded content documents before graph validation.
 * References may cross document boundaries. Pack dependencies are resolved first so ordering is deterministic.
 */
public final class ContentPackSet {
    public record Merged(ContentRegistry registry, List<String> packIds, Map<String, String> provenance) {
        public Merged {
            registry = Objects.requireNonNull(registry, "registry");
            packIds = List.copyOf(packIds);
            provenance = Map.copyOf(provenance);
        }
    }

    private ContentPackSet() {}

    public static Merged merge(List<ContentPackLoader.LoadedPack> packs) {
        Objects.requireNonNull(packs, "packs");
        if (packs.isEmpty()) throw new IllegalArgumentException("At least one content pack is required");

        Map<String, ContentPackLoader.LoadedPack> byId = new LinkedHashMap<>();
        for (ContentPackLoader.LoadedPack pack : packs) {
            ContentPackLoader.LoadedPack previous = byId.putIfAbsent(pack.packId(), pack);
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate content pack id '" + pack.packId() + "' from " + previous.source() + " and " + pack.source()
                );
            }
        }

        for (ContentPackLoader.LoadedPack pack : packs) {
            for (String dependency : pack.dependencies()) {
                if (!byId.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                        "Content pack '" + pack.packId() + "' from " + pack.source() + " requires missing pack '" + dependency + "'"
                    );
                }
            }
        }

        List<ContentPackLoader.LoadedPack> ordered = topologicalOrder(byId);
        ContentRegistry merged = new ContentRegistry();
        Map<String, String> provenance = new HashMap<>();
        List<String> packIds = new ArrayList<>();

        for (ContentPackLoader.LoadedPack pack : ordered) {
            packIds.add(pack.packId());
            provenance.put(pack.packId(), pack.source());
            for (CoreDefinition definition : pack.registry().all()) {
                try {
                    merged.register(definition);
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException(
                        "Failed merging definition " + definition.id() + " from pack '" + pack.packId() + "' (" + pack.source() + "): " + error.getMessage(),
                        error
                    );
                }
            }
        }

        return new Merged(merged, packIds, provenance);
    }

    private static List<ContentPackLoader.LoadedPack> topologicalOrder(Map<String, ContentPackLoader.LoadedPack> byId) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, Set<String>> dependants = new HashMap<>();
        for (String id : byId.keySet()) {
            indegree.put(id, 0);
            dependants.put(id, new HashSet<>());
        }

        for (ContentPackLoader.LoadedPack pack : byId.values()) {
            for (String dependency : pack.dependencies()) {
                if (dependants.get(dependency).add(pack.packId())) {
                    indegree.put(pack.packId(), indegree.get(pack.packId()) + 1);
                }
            }
        }

        var ready = new java.util.PriorityQueue<String>(Comparator.naturalOrder());
        indegree.forEach((id, degree) -> {
            if (degree == 0) ready.add(id);
        });

        List<ContentPackLoader.LoadedPack> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.remove();
            ordered.add(byId.get(id));
            List<String> next = dependants.get(id).stream().sorted().toList();
            for (String dependant : next) {
                int degree = indegree.computeIfPresent(dependant, (ignored, value) -> value - 1);
                if (degree == 0) ready.add(dependant);
            }
        }

        if (ordered.size() != byId.size()) {
            Set<String> unresolved = new HashSet<>(byId.keySet());
            ordered.forEach(pack -> unresolved.remove(pack.packId()));
            throw new IllegalArgumentException("Content pack dependency cycle detected among: " + unresolved.stream().sorted().toList());
        }
        return List.copyOf(ordered);
    }
}
