package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationAssetSelection;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
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
 * <p>A reload is a three-stage transaction: begin against one exact {@link ResourceManager}, stage the physically
 * validated presentation snapshot produced while inspecting that same resource manager, then publish the complete
 * geometry/material binding. Starting another reload or clearing the runtime invalidates both published and staged
 * state. Delayed work from an older resource pack therefore cannot resurrect stale resources.</p>
 *
 * <p>The staged capability is intentionally non-forgeable and retains the exact resource-manager object together
 * with the validated asset selection and generation. Geometry, approved animation binding, and material preparation
 * can therefore continue from one resource snapshot without passing a free-standing generation number or swapping
 * in a separately validated selection.</p>
 */
public final class Region01BossClientRenderRuntime {
    private static final GenerationPublicationSlot<SubmissionBinding> CURRENT = new GenerationPublicationSlot<>();
    private static final AtomicReference<ValidatedReload> STAGED = new AtomicReference<>();

    private Region01BossClientRenderRuntime() {}

    public static Optional<SubmissionBinding> current() {
        return CURRENT.current();
    }

    /**
     * Starts a new complete-resource preparation cycle and immediately removes any previous staged/published state.
     */
    public static ReloadTicket beginReload(ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        GenerationPublicationSlot.Ticket delegate = CURRENT.beginUpdate();
        STAGED.set(null);
        return new ReloadTicket(delegate, resourceManager);
    }

    /**
     * Retains a ready physical-resource validation result as the only capability that can later publish this reload.
     *
     * @return empty when a newer reload/clear made {@code ticket} stale before staging completed
     */
    public static Optional<ValidatedReload> stageValidated(
        ReloadTicket ticket,
        BossPresentationClientAssetRuntime.Snapshot assetSnapshot
    ) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(assetSnapshot, "assetSnapshot");
        if (!assetSnapshot.ready()) {
            throw new IllegalArgumentException("boss render staging requires a ready validated asset snapshot");
        }
        if (!CURRENT.isCurrent(ticket.delegate)) {
            return Optional.empty();
        }

        ValidatedReload candidate = new ValidatedReload(ticket.delegate, ticket.resourceManager, assetSnapshot);
        STAGED.set(candidate);

        // Close the race where a newer reload begins between the first generation check and STAGED.set(candidate).
        if (!CURRENT.isCurrent(ticket.delegate)) {
            STAGED.compareAndSet(candidate, null);
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    /**
     * Returns the currently staged validated reload, if any. Future preparation code must derive all runtime bytes,
     * approved animation bindings and material state through this capability's exact resource-manager snapshot.
     */
    public static Optional<ValidatedReload> staged() {
        return Optional.ofNullable(STAGED.get());
    }

    /**
     * Publishes one complete accepted binding only if {@code reload} is still the currently staged reload.
     *
     * <p>No caller can provide a replacement asset snapshot at publication time: the validated selection is carried
     * by the non-forgeable staged capability itself.</p>
     *
     * @return {@code false} when a newer reload/clear already invalidated the staged capability
     */
    public static boolean publish(
        ValidatedReload reload,
        BossCustomGeometryRenderPipeline pipeline,
        RenderType renderType,
        int packedOverlay,
        int packedColor
    ) {
        Objects.requireNonNull(reload, "reload");
        Objects.requireNonNull(pipeline, "pipeline");
        Objects.requireNonNull(renderType, "renderType");
        if (STAGED.get() != reload) {
            return false;
        }

        BossPresentationClientAssetRuntime.Snapshot assetSnapshot = reload.assetSnapshot;
        SubmissionBinding binding = new SubmissionBinding(
            assetSnapshot.contentGeneration(),
            assetSnapshot.selection().orElseThrow(),
            pipeline,
            renderType,
            packedOverlay,
            packedColor
        );
        boolean published = CURRENT.publish(reload.delegate, binding);
        if (published) {
            STAGED.compareAndSet(reload, null);
        }
        return published;
    }

    /**
     * Fail-closed lifecycle invalidation. Any staged or in-flight completion from before this call becomes stale.
     */
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

    /** Opaque reload-generation capability bound to one exact client resource-manager snapshot. */
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

    /**
     * Non-forgeable proof that one exact resource-manager snapshot produced a ready validated asset selection.
     */
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
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(renderType, "renderType");
        }
    }
}
