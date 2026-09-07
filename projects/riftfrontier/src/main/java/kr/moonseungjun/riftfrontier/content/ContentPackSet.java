package kr.moonseungjun.riftfrontier.content;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Merges independently decoded content documents before graph validation.
 * References are intentionally allowed to cross document boundaries; duplicate pack IDs and definition IDs are not.
 */
public final class ContentPackSet {
    public record Merged(ContentRegistry registry, List<String> packIds) {
        public Merged {
            registry = Objects.requireNonNull(registry, "registry");
            packIds = List.copyOf(packIds);
        }
    }

    private ContentPackSet() {}

    public static Merged merge(List<ContentPackLoader.LoadedPack> packs) {
        Objects.requireNonNull(packs, "packs");
        if (packs.isEmpty()) throw new IllegalArgumentException("At least one content pack is required");

        ContentRegistry merged = new ContentRegistry();
        Set<String> seenPackIds = new HashSet<>();
        List<String> packIds = packs.stream().map(pack -> {
            if (!seenPackIds.add(pack.packId())) {
                throw new IllegalArgumentException("Duplicate content pack id: " + pack.packId());
            }
            for (CoreDefinition definition : pack.registry().all()) {
                merged.register(definition);
            }
            return pack.packId();
        }).toList();

        return new Merged(merged, packIds);
    }
}
