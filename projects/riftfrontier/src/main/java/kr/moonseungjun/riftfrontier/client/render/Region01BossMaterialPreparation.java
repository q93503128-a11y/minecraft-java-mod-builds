package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Typed material preparation gate for the Region 01 boss renderer.
 *
 * <p>This class does not approve or invent art. A caller must provide an explicit human-review receipt containing
 * the exact texture resource ID, expected SHA-256 and render-treatment evidence identifier. Preparation then reads
 * that texture only from the exact {@link ResourceManager} already retained by the current validated reload and
 * verifies its bytes before a render type can become renderer-visible.</p>
 *
 * <p>The resulting capability retains the exact reviewed-animation capability and reload generation. A newer client
 * resource reload invalidates it transitively, so an old texture/render treatment cannot be combined with geometry
 * or animation prepared from a newer resource pack.</p>
 */
public final class Region01BossMaterialPreparation {
    private Region01BossMaterialPreparation() {}

    public static PreparedMaterial prepare(
        Region01BossAnimationPreparation.PreparedAnimation preparedAnimation,
        MaterialReview review,
        RenderType reviewedRenderType,
        int packedOverlay,
        int packedColor
    ) throws IOException {
        return prepare(
            preparedAnimation,
            review,
            reviewedRenderType,
            packedOverlay,
            packedColor,
            Region01BossMaterialPreparation::readResourceBytes
        );
    }

    static PreparedMaterial prepare(
        Region01BossAnimationPreparation.PreparedAnimation preparedAnimation,
        MaterialReview review,
        RenderType reviewedRenderType,
        int packedOverlay,
        int packedColor,
        RawResourceReader reader
    ) throws IOException {
        Objects.requireNonNull(preparedAnimation, "preparedAnimation");
        Objects.requireNonNull(review, "review");
        Objects.requireNonNull(reviewedRenderType, "reviewedRenderType");
        Objects.requireNonNull(reader, "reader");

        Region01BossClientRenderRuntime.ValidatedReload reload = preparedAnimation.validatedReload();
        requireCurrent(preparedAnimation, reload);

        ResourceManager resourceManager = reload.resourceManager();
        byte[] textureBytes = Objects.requireNonNull(
            reader.read(resourceManager, review.textureResource()),
            "reviewed texture reader returned null bytes"
        );
        requireCurrent(preparedAnimation, reload);

        String actualSha256 = sha256(textureBytes);
        if (!actualSha256.equals(review.textureSha256())) {
            throw new MaterialIntegrityException(review.textureResource(), review.textureSha256(), actualSha256);
        }
        requireCurrent(preparedAnimation, reload);

        return new PreparedMaterial(
            preparedAnimation,
            reload,
            review,
            reviewedRenderType,
            packedOverlay,
            packedColor,
            reload.publicationGeneration(),
            reload.contentGeneration()
        );
    }

    private static byte[] readResourceBytes(ResourceManager resourceManager, Identifier resourceId) throws IOException {
        var resource = resourceManager.getResource(resourceId)
            .orElseThrow(() -> new FileNotFoundException("reviewed Region 01 boss texture is missing: " + resourceId));
        try (InputStream input = resource.open()) {
            return input.readAllBytes();
        }
    }

    private static void requireCurrent(
        Region01BossAnimationPreparation.PreparedAnimation preparedAnimation,
        Region01BossClientRenderRuntime.ValidatedReload reload
    ) {
        if (!preparedAnimation.isCurrent() || Region01BossClientRenderRuntime.staged().orElse(null) != reload) {
            throw new StaleMaterialPreparationException(reload.publicationGeneration());
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    /**
     * Explicit review receipt. Creating one is an authoring assertion, not automatic art approval; production code
     * must only instantiate it from a recorded review decision and must never use the stripped Dragon source Atlas.
     */
    public record MaterialReview(
        String evidenceId,
        Identifier textureResource,
        String textureSha256,
        String renderTreatmentEvidenceId
    ) {
        public MaterialReview {
            evidenceId = requireEvidenceId(evidenceId, "evidenceId");
            Objects.requireNonNull(textureResource, "textureResource");
            textureSha256 = normalizeSha256(textureSha256);
            renderTreatmentEvidenceId = requireEvidenceId(renderTreatmentEvidenceId, "renderTreatmentEvidenceId");
        }

        private static String requireEvidenceId(String value, String field) {
            Objects.requireNonNull(value, field);
            String normalized = value.trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
            return normalized;
        }

        private static String normalizeSha256(String value) {
            Objects.requireNonNull(value, "textureSha256");
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("textureSha256 must be exactly 64 hex characters");
            }
            return normalized;
        }
    }

    /** Immutable material capability bound to one exact reviewed animation/reload transaction. */
    public static final class PreparedMaterial {
        private final Region01BossAnimationPreparation.PreparedAnimation preparedAnimation;
        private final Region01BossClientRenderRuntime.ValidatedReload reload;
        private final MaterialReview review;
        private final RenderType renderType;
        private final int packedOverlay;
        private final int packedColor;
        private final long publicationGeneration;
        private final long contentGeneration;

        private PreparedMaterial(
            Region01BossAnimationPreparation.PreparedAnimation preparedAnimation,
            Region01BossClientRenderRuntime.ValidatedReload reload,
            MaterialReview review,
            RenderType renderType,
            int packedOverlay,
            int packedColor,
            long publicationGeneration,
            long contentGeneration
        ) {
            this.preparedAnimation = Objects.requireNonNull(preparedAnimation, "preparedAnimation");
            this.reload = Objects.requireNonNull(reload, "reload");
            this.review = Objects.requireNonNull(review, "review");
            this.renderType = Objects.requireNonNull(renderType, "renderType");
            this.packedOverlay = packedOverlay;
            this.packedColor = packedColor;
            this.publicationGeneration = publicationGeneration;
            this.contentGeneration = contentGeneration;
        }

        public Region01BossAnimationPreparation.PreparedAnimation preparedAnimation() {
            requireCurrent(preparedAnimation, reload);
            return preparedAnimation;
        }

        public MaterialReview review() {
            requireCurrent(preparedAnimation, reload);
            return review;
        }

        public RenderType renderType() {
            requireCurrent(preparedAnimation, reload);
            return renderType;
        }

        public int packedOverlay() {
            requireCurrent(preparedAnimation, reload);
            return packedOverlay;
        }

        public int packedColor() {
            requireCurrent(preparedAnimation, reload);
            return packedColor;
        }

        public long publicationGeneration() {
            return publicationGeneration;
        }

        public long contentGeneration() {
            return contentGeneration;
        }

        public boolean isCurrent() {
            try {
                requireCurrent(preparedAnimation, reload);
                return true;
            } catch (StaleMaterialPreparationException stale) {
                return false;
            }
        }

        Region01BossClientRenderRuntime.ValidatedReload validatedReload() {
            requireCurrent(preparedAnimation, reload);
            return reload;
        }
    }

    @FunctionalInterface
    interface RawResourceReader {
        byte[] read(ResourceManager resourceManager, Identifier resourceId) throws IOException;
    }

    public static final class MaterialIntegrityException extends IllegalArgumentException {
        private final Identifier resourceId;
        private final String expectedSha256;
        private final String actualSha256;

        private MaterialIntegrityException(Identifier resourceId, String expectedSha256, String actualSha256) {
            super("reviewed Region 01 boss texture SHA-256 mismatch for " + resourceId
                + ": expected=" + expectedSha256 + ", actual=" + actualSha256);
            this.resourceId = resourceId;
            this.expectedSha256 = expectedSha256;
            this.actualSha256 = actualSha256;
        }

        public Identifier resourceId() {
            return resourceId;
        }

        public String expectedSha256() {
            return expectedSha256;
        }

        public String actualSha256() {
            return actualSha256;
        }
    }

    public static final class StaleMaterialPreparationException extends IllegalStateException {
        private final long publicationGeneration;

        private StaleMaterialPreparationException(long publicationGeneration) {
            super("Region 01 boss material preparation belongs to stale reload generation " + publicationGeneration);
            this.publicationGeneration = publicationGeneration;
        }

        public long publicationGeneration() {
            return publicationGeneration;
        }
    }
}
