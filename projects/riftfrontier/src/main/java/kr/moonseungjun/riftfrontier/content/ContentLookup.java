package kr.moonseungjun.riftfrontier.content;

import java.util.Collection;
import java.util.Optional;

/**
 * Read-only lookup boundary shared by validated build-time registries and published runtime snapshots.
 * Gameplay code must depend on this contract rather than receiving a mutable registry implementation.
 */
public interface ContentLookup {
    Optional<CoreDefinition> find(CoreDefinition.Kind kind, ContentId id);

    Collection<CoreDefinition> all();
}
