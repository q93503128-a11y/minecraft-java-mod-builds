package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PublishedContentGenerationGuardTest {
    @Test
    void currentProbeTracksTheSameGenerationBoundaryAsRequireCurrent() {
        AtomicLong current = new AtomicLong(7L);
        PublishedContentGenerationGuard guard = new PublishedContentGenerationGuard(7L, current::get);

        assertTrue(guard.isCurrent(), "matching publication generation must remain current");
        guard.requireCurrent();

        current.set(8L);
        assertFalse(guard.isCurrent(), "a newer atomic publication must make the retained capability stale");
        assertThrows(IllegalStateException.class, guard::requireCurrent,
            "stale publication generation must still fail closed through the throwing guard");
    }
}
