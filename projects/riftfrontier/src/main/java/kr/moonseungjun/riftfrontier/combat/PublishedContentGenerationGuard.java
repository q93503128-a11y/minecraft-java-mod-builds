package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.content.ContentRuntimeSnapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Immutable authority token for capabilities derived from one atomically published content generation.
 *
 * <p>A successful content reload publishes a new generation. Runtime capabilities that still retain
 * definitions from an older snapshot must stop rather than continuing a stale attack graph alongside
 * the newly authoritative content. Detached unit/GameTest catalogs have no published generation and
 * therefore produce no guard; they remain technical fixtures rather than pretending to be publication.</p>
 */
final class PublishedContentGenerationGuard {
    private final long sourceGeneration;
    private final LongSupplier currentGeneration;

    PublishedContentGenerationGuard(long sourceGeneration, LongSupplier currentGeneration) {
        if (sourceGeneration < 0L) throw new IllegalArgumentException("sourceGeneration must be >= 0");
        this.sourceGeneration = sourceGeneration;
        this.currentGeneration = Objects.requireNonNull(currentGeneration, "currentGeneration");
    }

    static Optional<PublishedContentGenerationGuard> fromCatalog(CombatRuntimeCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        var generation = catalog.publishedGeneration();
        if (generation.isEmpty()) return Optional.empty();
        return Optional.of(new PublishedContentGenerationGuard(
            generation.getAsLong(),
            () -> ContentRuntime.current().map(ContentRuntimeSnapshot::generation).orElse(-1L)
        ));
    }

    long sourceGeneration() {
        return sourceGeneration;
    }

    void requireCurrent() {
        long current = currentGeneration.getAsLong();
        if (current != sourceGeneration) {
            throw new IllegalStateException(
                "Published combat runtime generation is stale: expected " + sourceGeneration + ", current " + current
            );
        }
    }
}
