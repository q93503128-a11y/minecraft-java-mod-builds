package kr.moonseungjun.riftfrontier.content;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable publication boundary for validated content definitions. */
public final class ContentRuntimeSnapshot implements ContentLookup {
    private final long generation;
    private final Instant loadedAt;
    private final List<String> packIds;
    private final ContentRegistry registry;
    private final ContentCatalog catalog;

    ContentRuntimeSnapshot(long generation, Instant loadedAt, List<String> packIds, ContentRegistry registry) {
        if (generation < 0) throw new IllegalArgumentException("generation must be >= 0");
        this.generation = generation;
        this.loadedAt = Objects.requireNonNull(loadedAt, "loadedAt");
        this.packIds = List.copyOf(packIds);
        this.registry = Objects.requireNonNull(registry, "registry");
        this.catalog = ContentCatalog.from(registry);
    }

    public long generation() { return generation; }
    public Instant loadedAt() { return loadedAt; }
    public List<String> packIds() { return packIds; }
    public String fingerprint() { return catalog.fingerprint(); }
    public int definitionCount() { return registry.size(); }

    @Override
    public Optional<CoreDefinition> find(CoreDefinition.Kind kind, ContentId id) {
        return registry.find(kind, id);
    }

    @Override
    public Collection<CoreDefinition> all() {
        return registry.all();
    }

    public ContentCatalog catalog() { return catalog; }
}
