package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API-free regression coverage for the provenance discipline used by Region01BossAnimationPreparation/publication.
 * Production Minecraft class compatibility is covered by clean/client compilation and client smoke.
 */
class Region01BossAnimationPreparationTest {
    @Test
    void staleReloadCannotPrepareReviewedAnimation() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        Object geometry = new Object();
        AtomicBoolean reviewResolved = new AtomicBoolean();
        slot.beginUpdate();

        assertThrows(StalePreparation.class, () -> prepare(
            slot,
            reload,
            geometry,
            true,
            () -> reviewResolved.set(true)
        ));
        assertFalse(reviewResolved.get(), "stale reload must fail before reviewed source windows are resolved");
    }

    @Test
    void unreviewedWindowsCannotBecomePreparedCapability() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();

        assertThrows(UnreviewedWindows.class, () -> prepare(
            slot,
            reload,
            new Object(),
            false,
            () -> { }
        ));
    }

    @Test
    void publicationRequiresExactPreparedGeometryAnimationBridgeAndSemanticResolverIdentity() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        Object geometry = new Object();
        Object semanticResolver = new Object();
        Object bridge = new Object();
        Prepared prepared = new Prepared(reload, geometry, semanticResolver, bridge);

        assertTrue(canPublish(slot, prepared, geometry, semanticResolver, bridge));
        assertFalse(canPublish(slot, prepared, new Object(), semanticResolver, bridge));
        assertFalse(canPublish(slot, prepared, geometry, new Object(), bridge));
        assertFalse(canPublish(slot, prepared, geometry, semanticResolver, new Object()));

        slot.beginUpdate();
        assertFalse(canPublish(slot, prepared, geometry, semanticResolver, bridge));
    }

    private static Prepared prepare(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload,
        Object geometry,
        boolean reviewedWindows,
        Runnable resolveReviewedSources
    ) {
        requireCurrent(slot, reload);
        if (!reviewedWindows) throw new UnreviewedWindows();
        resolveReviewedSources.run();
        requireCurrent(slot, reload);
        Object semanticResolver = new Object();
        Object bridge = new Object();
        requireCurrent(slot, reload);
        return new Prepared(reload, geometry, semanticResolver, bridge);
    }

    private static boolean canPublish(
        GenerationPublicationSlot<String> slot,
        Prepared prepared,
        Object geometry,
        Object semanticResolver,
        Object bridge
    ) {
        if (!slot.isCurrent(prepared.reload())) return false;
        return prepared.geometry() == geometry
            && prepared.semanticResolver() == semanticResolver
            && prepared.bridge() == bridge;
    }

    private static void requireCurrent(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload
    ) {
        if (!slot.isCurrent(reload)) throw new StalePreparation();
    }

    private record Prepared(
        GenerationPublicationSlot.Ticket reload,
        Object geometry,
        Object semanticResolver,
        Object bridge
    ) { }

    private static final class StalePreparation extends IllegalStateException { }
    private static final class UnreviewedWindows extends IllegalStateException { }
}
