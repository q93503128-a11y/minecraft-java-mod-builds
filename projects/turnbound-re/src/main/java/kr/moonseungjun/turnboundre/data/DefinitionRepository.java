package kr.moonseungjun.turnboundre.data;

import java.util.Map;

/**
 * Atomic server-side definition snapshot. Reload builds and validates a complete replacement first;
 * invalid data never partially mutates the active registry.
 */
public final class DefinitionRepository {
    public record Snapshot(DefinitionRegistry registry, String hash, long generation) {
        public Snapshot {
            if (registry == null) throw new IllegalArgumentException("registry must not be null");
            if (hash == null || hash.length() != 64) throw new IllegalArgumentException("hash must be SHA-256 hex");
            if (generation < 0) throw new IllegalArgumentException("generation must be >= 0");
        }
    }

    private volatile Snapshot snapshot;

    public DefinitionRepository() {
        DefinitionBundleParser.Parsed empty = DefinitionBundleParser.parse(Map.of());
        this.snapshot = new Snapshot(empty.registry(), empty.hash(), 0L);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public DefinitionRegistry current() {
        return snapshot.registry();
    }

    public synchronized Snapshot install(DefinitionRegistry registry, String hash) {
        Snapshot previous = snapshot;
        Snapshot next = new Snapshot(registry, hash, previous.generation() + 1L);
        snapshot = next;
        return next;
    }
}
