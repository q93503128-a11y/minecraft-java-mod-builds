package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationAssetSelection;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Atomic client publication seam between an accepted boss runtime asset and the registered entity renderer.
 *
 * <p>The runtime starts inactive and has no fallback model, texture or material. Resource preparation must begin
 * with {@link #beginReload()} and may only publish with the returned generation-bound ticket. Starting a newer
 * reload or clearing the runtime invalidates every older ticket, so a delayed asynchronous completion cannot
 * resurrect stale resources after a newer reload or client lifecycle transition.</p>
 *
 * <p>Publication also requires the physically validated boss-presentation snapshot produced by the same client
 * resource reload. The complete pipeline, validated asset selection and Minecraft material are stored as one
 * immutable binding; renderer code can therefore never observe a geometry pipeline from one resource generation
 * while simultaneously observing a material/selection from another.</p>
 */
public final class Region01BossClientRenderRuntime {
    private static final GenerationPublicationSlot<SubmissionBinding> CURRENT = new GenerationPublicationSlot<>();

    private Region01BossClientRenderRuntime() {}

    public static Optional<SubmissionBinding> current() {
        return CURRENT.current();
    }

    /**
     * Starts a new complete-resource preparation cycle and immediately removes any previously published binding.
     */
    public static ReloadTicket beginReload() {
        return new ReloadTicket(CURRENT.beginUpdate());
    }

    /**
     * Publishes one complete accepted binding only if {@code ticket} still belongs to the newest reload generation.
     *
     * <p>The supplied asset snapshot must already be physically validated and ready. Inactive snapshots are rejected
     * rather than allowing an accepted geometry pipeline to be paired with unproven resource-pack paths.</p>
     *
     * @return {@code false} when a newer reload/clear already invalidated the ticket
     */
    public static boolean publish(
        ReloadTicket ticket,
        BossPresentationClientAssetRuntime.Snapshot assetSnapshot,
        BossCustomGeometryRenderPipeline pipeline,
        RenderType renderType,
        int packedOverlay,
        int packedColor
    ) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(assetSnapshot, "assetSnapshot");
        if (!assetSnapshot.ready()) {
            throw new IllegalArgumentException("boss render publication requires a ready validated asset snapshot");
        }
        SubmissionBinding binding = new SubmissionBinding(
            assetSnapshot.contentGeneration(),
            assetSnapshot.selection().orElseThrow(),
            pipeline,
            renderType,
            packedOverlay,
            packedColor
        );
        return CURRENT.publish(ticket.delegate, binding);
    }

    /**
     * Fail-closed lifecycle invalidation. Any in-flight reload completion from before this call becomes stale.
     */
    public static void clear() {
        CURRENT.invalidate();
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

    /** Opaque reload-generation capability. Tickets cannot be manufactured from generation numbers. */
    public static final class ReloadTicket {
        private final GenerationPublicationSlot.Ticket delegate;

        private ReloadTicket(GenerationPublicationSlot.Ticket delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public long generation() {
            return delegate.generation();
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
