package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.client.BossPresentationClientState;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSampleBridge;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationResolver;
import kr.moonseungjun.riftfrontier.combat.presentation.BossSkinnedMeshFrameSampler;
import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-facing client render pipeline from one server-authoritative boss semantic snapshot to immutable custom geometry.
 *
 * <p>The pipeline deliberately owns no animation clock, hit timing, material lookup, texture fallback, or source-asset
 * substitution. It resolves the authored presentation binding, derives animation time from the authoritative semantic
 * progress, skins the accepted mesh, and prepares the exact triangle stream consumed by Minecraft's custom-geometry
 * submission path.</p>
 */
public final class BossCustomGeometryRenderPipeline {
    private final ContentId bossProfile;
    private final String variant;
    private final BossPresentationResolver presentationResolver;
    private final BossAnimationSampleBridge animationBridge;
    private final BossSkinnedMeshFrameSampler frameSampler;

    public BossCustomGeometryRenderPipeline(
        ContentId bossProfile,
        String variant,
        BossPresentationResolver presentationResolver,
        BossAnimationSampleBridge animationBridge,
        BossSkinnedMeshFrameSampler frameSampler
    ) {
        this.bossProfile = Objects.requireNonNull(bossProfile, "bossProfile");
        this.variant = requireVariant(variant);
        this.presentationResolver = Objects.requireNonNull(presentationResolver, "presentationResolver");
        this.animationBridge = Objects.requireNonNull(animationBridge, "animationBridge");
        this.frameSampler = Objects.requireNonNull(frameSampler, "frameSampler");
    }

    /** Resolves one explicit semantic snapshot without consulting client-global time. */
    public Optional<PreparedFrame> prepare(BossPresentationSemanticState state) {
        Objects.requireNonNull(state, "state");
        if (!state.active()) return Optional.empty();

        return presentationResolver.resolve(bossProfile, variant, state).flatMap(resolved ->
            animationBridge.sample(state, resolved).map(sample -> {
                BossSkinnedMeshFrameSampler.FrameSample frameSample = frameSampler.sample(sample);
                return new PreparedFrame(
                    state.entityId(),
                    state.entityUuid(),
                    resolved,
                    frameSample,
                    SkinnedMeshCustomGeometryAdapter.prepare(frameSample)
                );
            })
        );
    }

    /** Compatibility lookup for pure-Java tests. Production render submission must use the UUID-checked overload. */
    public Optional<PreparedFrame> prepareCurrent(int entityId) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        return BossPresentationClientState.current(entityId).flatMap(this::prepare);
    }

    /** Uses the monotonic client cache only when both the current numeric id and Minecraft UUID match. */
    public Optional<PreparedFrame> prepareCurrent(int entityId, UUID entityUuid) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        Objects.requireNonNull(entityUuid, "entityUuid");
        return BossPresentationClientState.current(entityId, entityUuid).flatMap(this::prepare);
    }

    /**
     * Entity-renderer call boundary. Material/texture choice remains an explicit caller responsibility.
     * Returns false when no active, fully resolvable authoritative presentation exists for this exact actor identity.
     */
    public boolean submitCurrent(
        int entityId,
        UUID entityUuid,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        Optional<PreparedFrame> prepared = prepareCurrent(entityId, entityUuid);
        if (prepared.isEmpty()) return false;
        SkinnedMeshCustomGeometryAdapter.submit(
            prepared.get().frameSample(),
            poseStack,
            collector,
            renderType,
            packedLight,
            packedOverlay,
            packedColor
        );
        return true;
    }

    public ContentId bossProfile() {
        return bossProfile;
    }

    public String variant() {
        return variant;
    }

    public record PreparedFrame(
        int entityId,
        UUID entityUuid,
        BossPresentationResolver.ResolvedPresentation resolvedPresentation,
        BossSkinnedMeshFrameSampler.FrameSample frameSample,
        SkinnedMeshCustomGeometryAdapter.PreparedGeometry geometry
    ) {
        public PreparedFrame {
            if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
            Objects.requireNonNull(entityUuid, "entityUuid");
            Objects.requireNonNull(resolvedPresentation, "resolvedPresentation");
            Objects.requireNonNull(frameSample, "frameSample");
            Objects.requireNonNull(geometry, "geometry");
            if (geometry.vertexCount() != frameSample.frame().vertexCount()
                || geometry.triangleCount() != frameSample.frame().triangleCount()) {
                throw new IllegalArgumentException("prepared geometry no longer matches authoritative skinned frame topology");
            }
        }
    }

    private static String requireVariant(String value) {
        String normalized = Objects.requireNonNull(value, "variant").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("variant must be non-blank");
        return normalized;
    }
}
