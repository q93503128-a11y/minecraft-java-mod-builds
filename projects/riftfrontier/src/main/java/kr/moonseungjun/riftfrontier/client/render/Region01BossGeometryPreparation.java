package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01BossRuntimeAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Typed preparation gate for the exact accepted Region 01 boss derivation.
 *
 * <p>The derivation is read only from the {@link ResourceManager} carried by the currently staged
 * {@link Region01BossClientRenderRuntime.ValidatedReload}. Raw bytes are passed directly to
 * {@link Region01BossRuntimeAsset#importAccepted(byte[])} so the accepted SHA, topology, rig and source animation
 * inventory gates all remain authoritative. No animation meaning, material, texture or render type is selected here.
 *
 * <p>The staged capability is checked before I/O, after I/O, after import, and again whenever prepared geometry is
 * consumed. A newer reload or {@code clear()} therefore makes delayed preparation unusable instead of allowing an
 * old resource-pack snapshot to re-enter renderer publication.</p>
 */
public final class Region01BossGeometryPreparation {
    private Region01BossGeometryPreparation() {}

    public static PreparedGeometry prepare(
        Region01BossClientRenderRuntime.ValidatedReload reload,
        Identifier acceptedDerivationResource
    ) throws IOException {
        return prepare(reload, acceptedDerivationResource, Region01BossGeometryPreparation::readResourceBytes);
    }

    static PreparedGeometry prepare(
        Region01BossClientRenderRuntime.ValidatedReload reload,
        Identifier acceptedDerivationResource,
        RawResourceReader reader
    ) throws IOException {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(acceptedDerivationResource, "acceptedDerivationResource");
        Objects.requireNonNull(reader, "reader");

        requireCurrent(reload);
        ResourceManager resourceManager = reload.resourceManager();
        byte[] rawBytes = Objects.requireNonNull(
            reader.read(resourceManager, acceptedDerivationResource),
            "accepted derivation reader returned null bytes"
        );
        requireCurrent(reload);

        Region01BossRuntimeAsset runtimeAsset = Region01BossRuntimeAsset.importAccepted(rawBytes);
        requireCurrent(reload);

        return new PreparedGeometry(
            reload,
            acceptedDerivationResource,
            runtimeAsset,
            reload.publicationGeneration(),
            reload.contentGeneration()
        );
    }

    private static byte[] readResourceBytes(ResourceManager resourceManager, Identifier resourceId) throws IOException {
        var resource = resourceManager.getResource(resourceId)
            .orElseThrow(() -> new FileNotFoundException("accepted Region 01 boss derivation is missing: " + resourceId));
        try (InputStream input = resource.open()) {
            return input.readAllBytes();
        }
    }

    private static void requireCurrent(Region01BossClientRenderRuntime.ValidatedReload reload) {
        if (Region01BossClientRenderRuntime.staged().orElse(null) != reload) {
            throw new StaleReloadException(reload.publicationGeneration());
        }
    }

    @FunctionalInterface
    interface RawResourceReader {
        byte[] read(ResourceManager resourceManager, Identifier resourceId) throws IOException;
    }

    /**
     * Immutable prepared geometry capability. Access remains fail-closed after a newer reload invalidates its source.
     */
    public static final class PreparedGeometry {
        private final Region01BossClientRenderRuntime.ValidatedReload reload;
        private final Identifier resourceId;
        private final Region01BossRuntimeAsset runtimeAsset;
        private final long publicationGeneration;
        private final long contentGeneration;

        private PreparedGeometry(
            Region01BossClientRenderRuntime.ValidatedReload reload,
            Identifier resourceId,
            Region01BossRuntimeAsset runtimeAsset,
            long publicationGeneration,
            long contentGeneration
        ) {
            this.reload = Objects.requireNonNull(reload, "reload");
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
            this.runtimeAsset = Objects.requireNonNull(runtimeAsset, "runtimeAsset");
            this.publicationGeneration = publicationGeneration;
            this.contentGeneration = contentGeneration;
        }

        public Region01BossRuntimeAsset runtimeAsset() {
            requireCurrent(reload);
            return runtimeAsset;
        }

        public Identifier resourceId() {
            return resourceId;
        }

        public long publicationGeneration() {
            return publicationGeneration;
        }

        public long contentGeneration() {
            return contentGeneration;
        }

        Region01BossClientRenderRuntime.ValidatedReload validatedReload() {
            requireCurrent(reload);
            return reload;
        }
    }

    public static final class StaleReloadException extends IllegalStateException {
        private final long publicationGeneration;

        private StaleReloadException(long publicationGeneration) {
            super("Region 01 boss geometry preparation belongs to stale reload generation " + publicationGeneration);
            this.publicationGeneration = publicationGeneration;
        }

        public long publicationGeneration() {
            return publicationGeneration;
        }
    }
}
