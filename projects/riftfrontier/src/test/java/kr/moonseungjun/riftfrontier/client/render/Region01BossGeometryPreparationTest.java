package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationAssetManifest;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationAssetSelection;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossGeometryPreparationTest {
    private static final Identifier DERIVATION = Identifier.fromNamespaceAndPath(
        "riftfrontier",
        "boss_runtime/region_01/dragon_evolved.accepted.gltf"
    );

    @AfterEach
    void clearRuntime() {
        Region01BossClientRenderRuntime.clear();
    }

    @Test
    void staleValidatedReloadIsRejectedBeforeResourceRead() {
        ResourceManager oldResources = dummyResources("old");
        Region01BossClientRenderRuntime.ValidatedReload oldReload = stage(oldResources, 31L);
        Region01BossClientRenderRuntime.beginReload(dummyResources("new"));

        AtomicBoolean read = new AtomicBoolean();
        Region01BossGeometryPreparation.StaleReloadException failure = assertThrows(
            Region01BossGeometryPreparation.StaleReloadException.class,
            () -> Region01BossGeometryPreparation.prepare(oldReload, DERIVATION, (manager, resourceId) -> {
                read.set(true);
                return new byte[] {1};
            })
        );

        assertFalse(read.get(), "stale reload must fail before touching resource bytes");
        assertTrue(failure.publicationGeneration() > 0L);
    }

    @Test
    void currentReloadReadsOnlyItsExactResourceManagerAndStillRequiresAcceptedSha() {
        ResourceManager resources = dummyResources("current");
        Region01BossClientRenderRuntime.ValidatedReload reload = stage(resources, 32L);

        IllegalArgumentException failure = assertThrows(
            IllegalArgumentException.class,
            () -> Region01BossGeometryPreparation.prepare(reload, DERIVATION, (manager, resourceId) -> {
                assertSame(resources, manager, "geometry bytes must come from the staged ResourceManager instance");
                assertSame(DERIVATION, resourceId);
                return new byte[] {1, 2, 3, 4};
            })
        );

        assertTrue(failure.getMessage().contains("derivation SHA-256 mismatch"));
        assertTrue(Region01BossClientRenderRuntime.staged().orElseThrow() == reload);
    }

    @Test
    void reloadStartedDuringIoInvalidatesBytesBeforeImporterRuns() {
        ResourceManager resources = dummyResources("current");
        Region01BossClientRenderRuntime.ValidatedReload reload = stage(resources, 33L);
        AtomicBoolean read = new AtomicBoolean();

        assertThrows(
            Region01BossGeometryPreparation.StaleReloadException.class,
            () -> Region01BossGeometryPreparation.prepare(reload, DERIVATION, (manager, resourceId) -> {
                assertSame(resources, manager);
                read.set(true);
                Region01BossClientRenderRuntime.beginReload(dummyResources("replacement"));
                return new byte[] {9, 9, 9};
            })
        );

        assertTrue(read.get());
    }

    private static Region01BossClientRenderRuntime.ValidatedReload stage(
        ResourceManager resources,
        long contentGeneration
    ) {
        BossPresentationAssetSelection selection = BossPresentationAssetSelection.validate(
            new BossPresentationAssetManifest(List.of()),
            (kind, resourceId) -> true
        ).selection().orElseThrow();
        BossPresentationClientAssetRuntime.Snapshot ready = new BossPresentationClientAssetRuntime.Snapshot(
            contentGeneration,
            Optional.of(selection)
        );
        return Region01BossClientRenderRuntime.stageValidated(
            Region01BossClientRenderRuntime.beginReload(resources),
            ready
        ).orElseThrow();
    }

    private static ResourceManager dummyResources(String label) {
        return (ResourceManager) Proxy.newProxyInstance(
            ResourceManager.class.getClassLoader(),
            new Class<?>[] {ResourceManager.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "toString" -> "DummyResourceManager[" + label + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new AssertionError("unexpected ResourceManager call: " + method);
            }
        );
    }
}
