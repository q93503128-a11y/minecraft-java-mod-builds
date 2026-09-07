package kr.moonseungjun.riftfrontier.content;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ContentRegistry {
    private final Map<CoreDefinition.Kind, Map<ContentId, CoreDefinition>> byKind = new EnumMap<>(CoreDefinition.Kind.class);

    public ContentRegistry() {
        for (CoreDefinition.Kind kind : CoreDefinition.Kind.values()) byKind.put(kind, new LinkedHashMap<>());
    }

    public <T extends CoreDefinition> T register(T definition) {
        Objects.requireNonNull(definition, "definition");
        Map<ContentId, CoreDefinition> bucket = byKind.get(definition.kind());
        CoreDefinition existing = bucket.putIfAbsent(definition.id(), definition);
        if (existing != null) throw new IllegalStateException("Duplicate " + definition.kind() + " id: " + definition.id());
        return definition;
    }

    public Optional<CoreDefinition> find(CoreDefinition.Kind kind, ContentId id) { return Optional.ofNullable(byKind.get(kind).get(id)); }
    public boolean contains(CoreDefinition.Kind kind, ContentId id) { return byKind.get(kind).containsKey(id); }
    public Collection<CoreDefinition> all() { return byKind.values().stream().flatMap(map -> map.values().stream()).toList(); }
    public int size() { return byKind.values().stream().mapToInt(Map::size).sum(); }
}
