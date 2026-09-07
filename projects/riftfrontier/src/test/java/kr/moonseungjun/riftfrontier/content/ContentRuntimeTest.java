package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContentRuntimeTest {
    @AfterEach
    void reset() {
        ContentRuntime.resetForTests();
    }

    @Test
    void publishesValidatedSnapshotWithDeterministicCatalog() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var snapshot = ContentRuntime.installValidated(pack.registry(), List.of(pack.packId()));

        assertEquals(1, snapshot.generation());
        assertEquals(10, snapshot.definitionCount());
        assertEquals(List.of(pack.packId()), snapshot.packIds());
        assertEquals(ContentCatalog.from(pack.registry()).fingerprint(), snapshot.fingerprint());
        assertSame(snapshot, ContentRuntime.requireCurrent());
    }

    @Test
    void rejectedCandidateDoesNotReplaceLastKnownGoodSnapshot() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var good = ContentRuntime.installValidated(pack.registry(), List.of(pack.packId()));

        ContentRegistry broken = new ContentRegistry();
        broken.register(new CoreDefinition.Creature(
            ContentId.rift("creature/broken_runtime"),
            ContentId.rift("region/missing"),
            ContentId.rift("archetype/missing"),
            ContentId.rift("loot/missing"),
            Set.of("pursue")
        ));

        assertThrows(IllegalStateException.class, () -> ContentRuntime.installValidated(broken, List.of("riftfrontier:broken")));
        assertSame(good, ContentRuntime.requireCurrent());
        assertEquals(1, ContentRuntime.requireCurrent().generation());
    }

    @Test
    void successfulReplacementAdvancesGenerationAtomically() {
        var firstPack = CoreContentBootstrap.bootstrapAndValidate();
        var first = ContentRuntime.installValidated(firstPack.registry(), List.of(firstPack.packId()));

        var secondPack = CoreContentBootstrap.bootstrapAndValidate();
        var second = ContentRuntime.installValidated(secondPack.registry(), List.of(secondPack.packId()));

        assertEquals(first.generation() + 1, second.generation());
        assertSame(second, ContentRuntime.requireCurrent());
    }
}
