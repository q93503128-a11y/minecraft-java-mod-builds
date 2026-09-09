package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSourceBinding;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationAssetSelection;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01BossRuntimeAsset;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic client publication seam between an accepted boss runtime asset and the registered entity renderer.
 *
 * <p>A reload is a staged transaction bound to one exact {@link ResourceManager}. Geometry, reviewed animation and
 * reviewed material must all be prepared from that same reload before renderer publication can succeed. Starting a
 * newer reload or clearing the runtime invalidates every older capability.</p>
 */
public final class Region01BossClientRenderRuntime {
    private static final GenerationPublicationSlot<SubmissionBinding> CURRENT = new GenerationPublicationSlot<>();
    private static final AtomicReference<ValidatedReload> STAGED = new AtomicReference<>();

    private Region01BossClientRenderRuntime() {}

    public static Optional<SubmissionBinding> current() {
        return CURRENT.current();
    }

    public static ReloadTicket beginReload(ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        GenerationPublicationSlot.Ticket delegate = CURRENT.beginUpdate();
        STAGED.set(null);
        return new ReloadTicket(delegate, resourceManager);
    }

    public static Optional<ValidatedReload> stageValidated(
        ReloadTicket ticket,
        BossPresentationClientAssetRuntime.Snapshot assetSnapshot
    ) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(assetSnapshot, "assetSnapshot");
        if (!assetSnapshot.ready()) {
            throw new IllegalArgumentException("boss render staging requires a ready validated asset snapshot");
        }
        if (!CURRENT.isCurrent(ticket.delegate)) return Optional.empty();

        ValidatedReload candidate = new ValidatedReload(ticket.delegate, ticket.resourceManager, assetSnapshot);
        STAGED.set(candidate);
        if (!CURRENT.isCurrent(ticket.delegate)) {
            STAGED.compareAndSet(candidate, null);
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    public static Optional<ValidatedReload> staged() {
        return Optional.ofNullable(STAGED.get());
    }

    /**
     * Publishes one complete accepted binding only from a reviewed material capability that already retains the exact
     * prepared animation and geometry from this staged reload. The old free-standing RenderType/overlay/color
     * publication seam intentionally does not exist: material bytes, render treatment and renderer-visible state must
     * cross one provenance gate together.
     *
     * @return {@code false} when a newer reload/clear already invalidated the prepared material capability
     */
    public static boolean publish(
        Region01BossMaterialPreparation.PreparedMaterial preparedMaterial,
        BossCustomGeometryRenderPipeline pipeline
    ) {
        Objects.requireNonNull(preparedMaterial, "preparedMaterial");
        Objects.requireNonNull(pipeline, "pipeline");

        final Region01BossAnimationPreparation.PreparedAnimation preparedAnimation;
        final Region01BossGeometryPreparation.PreparedGeometry preparedGeometry;
        final ValidatedReload reload;
        final Region01BossRuntimeAsset runtimeAsset;
        final BossAnimationSourceBinding reviewedBinding;
        final Region01BossMaterialPreparation.MaterialReview materialReview;
        final RenderType renderType;
        final int packedOverlay;
        final int packedColor;
        try {
            preparedAnimation = preparedMaterial.preparedAnimation();
            preparedGeometry = preparedAnimation.preparedGeometry();
            reload = preparedMaterial.validatedReload();
            runtimeAsset = preparedAnimation.runtimeAsset();
            reviewedBinding = preparedAnimation.sourceBinding().requireReviewedPhaseWindows();
            materialReview = preparedMaterial.review();
            renderType = preparedMaterial.renderType();
            packedOverlay = preparedMaterial.packedOverlay();
            packedColor = preparedMaterial.packedColor();
        } catch (Region01BossGeometryPreparation.StaleReloadException
                 | Region01BossMaterialPreparation.StaleMaterialPreparationException stale) {
            return false;
        }

        if (STAGED.get() != reload) return false;
        if (preparedMaterial.publicationGeneration() != reload.publicationGeneration()
            || preparedMaterial.contentGeneration() != reload.contentGeneration()
            || preparedAnimation.publicationGeneration() != reload.publicationGeneration()
            || preparedAnimation.contentGeneration() != reload.contentGeneration()
            || preparedGeometry.publicationGeneration() != reload.publicationGeneration()
            || preparedGeometry.contentGeneration() != reload.contentGeneration()) {
            throw new IllegalArgumentException("prepared boss presentation generation no longer matches its validated reload");
        }
        if (preparedMaterial.preparedAnimation() != preparedAnimation) {
            throw new IllegalArgumentException("prepared boss material must retain its exact prepared animation capability");
        }
        if (preparedAnimation.preparedGeometry() != preparedGeometry) {
            throw new IllegalArgumentException("prepared boss animation must retain its exact prepared geometry capability");
        }
        if (pipeline.skinnedMeshAsset() != runtimeAsset.skinnedMesh()) {
            throw new IllegalArgumentException(
                "boss render pipeline must consume the exact skinned mesh prepared from the current accepted derivation"
            );
        }
        if (pipeline.animationBridge() != preparedAnimation.animationBridge()) {
            throw new IllegalArgumentException(
                "boss render pipeline must consume the exact reviewed animation bridge prepared from the current reload"
            );
        }

        BossPresentationClientAssetRuntime.Snapshot assetSnapshot = reload.assetSnapshot;
        SubmissionBinding binding = new SubmissionBinding(
            assetSnapshot.contentGeneration(),
            assetSnapshot.selection().orElseThrow(),
            reviewedBinding,
            materialReview,
            pipeline,
            renderType,
            packedOverlay,
            packedColor
        );
        boolean published = CURRENT.publish(reload.delegate, binding);
        if (published) STAGED.compareAndSet(reload, null);
        return published;
    }

    public static void clear() {
        CURRENT.invalidate();
        STAGED.set(null);
    }

    public static boolean submit(
        int entityId,
        UUID entityUuid,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight
    ) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        Objects.requireNonNull(entityUuid, "entityUuid");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(collector, "collector");
        Optional<SubmissionBinding> binding = CURRENT.current();
        if (binding.isEmpty()) return false;
        SubmissionBinding active = binding.orElseThrow();
        return active.pipeline().submitCurrent(
            entityId,
            entityUuid,
            poseStack,
            collector,
            active.renderType(),
            packedLight,
            active.packedOverlay(),
            active.packedColor()
        );
    }

    public static final class ReloadTicket {
        private final GenerationPublicationSlot.Ticket delegate;
        private final ResourceManager resourceManager;

        private ReloadTicket(GenerationPublicationSlot.Ticket delegate, ResourceManager resourceManager) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
        }

        public long generation() {
            return delegate.generation();
        }
    }

    public static final class ValidatedReload {
        private final GenerationPublicationSlot.Ticket delegate;
        private final ResourceManager resourceManager;
        private final BossPresentationClientAssetRuntime.Snapshot assetSnapshot;

        private ValidatedReload(
            GenerationPublicationSlot.Ticket delegate,
            ResourceManager resourceManager,
            BossPresentationClientAssetRuntime.Snapshot assetSnapshot
        ) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
            this.assetSnapshot = Objects.requireNonNull(assetSnapshot, "assetSnapshot");
        }

        public ResourceManager resourceManager() {
            return resourceManager;
        }

        public BossPresentationClientAssetRuntime.Snapshot assetSnapshot() {
            return assetSnapshot;
        }

        public long publicationGeneration() {
            return delegate.generation();
        }

        public long contentGeneration() {
            return assetSnapshot.contentGeneration();
        }

        public BossPresentationAssetSelection assetSelection() {
            return assetSnapshot.selection().orElseThrow();
        }
    }

    /** One immutable renderer-visible publication assembled from one validated resource reload. */
    public record SubmissionBinding(
        long contentGeneration,
        BossPresentationAssetSelection assetSelection,
        BossAnimationSourceBinding reviewedAnimationBinding,
        Region01BossMaterialPreparation.MaterialReview materialReview,
        BossCustomGeometryRenderPipeline pipeline,
        RenderType renderType,
        int packedOverlay,
        int packedColor
    ) {
        public SubmissionBinding {
            if (contentGeneration < 0L) {
                throw new IllegalArgumentException("contentGeneration must be >= 0");
            }
            Objects.requireNonNull(assetSelection, "assetSelection");
            Objects.requireNonNull(reviewedAnimationBinding, "reviewedAnimationBinding").requireReviewedPhaseWindows();
            Objects.requireNonNull(materialReview, "materialReview");
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(renderType, "renderType");
        }
    }
}
