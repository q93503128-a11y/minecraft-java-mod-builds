package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

final class PublishedContentGenerationGuardTest {
    @Test
    void acceptsOnlyTheGenerationThatCreatedTheCapability() {
        AtomicLong current = new AtomicLong(7L);
        PublishedContentGenerationGuard guard = new PublishedContentGenerationGuard(7L, current::get);

        assertEquals(7L, guard.sourceGeneration());
        assertDoesNotThrow(guard::requireCurrent);

        current.set(8L);
        IllegalStateException stale = assertThrows(IllegalStateException.class, guard::requireCurrent);
        assertTrue(stale.getMessage().contains("expected 7"));
        assertTrue(stale.getMessage().contains("current 8"));
    }

    @Test
    void missingPublishedRuntimeAlsoInvalidatesTheCapability() {
        PublishedContentGenerationGuard guard = new PublishedContentGenerationGuard(3L, () -> -1L);
        assertThrows(IllegalStateException.class, guard::requireCurrent);
    }

    @Test
    void detachedFixtureCatalogDoesNotPretendToOwnAPublishedGeneration() {
        CombatRuntimeCatalog detached = new CombatRuntimeCatalog(new ContentRegistry());
        assertTrue(detached.publishedGeneration().isEmpty());
        assertTrue(PublishedContentGenerationGuard.fromCatalog(detached).isEmpty());
    }

    @Test
    void negativeSourceGenerationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PublishedContentGenerationGuard(-1L, () -> -1L));
    }
}
