package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic client publication seam between an accepted boss runtime asset and the registered entity renderer.
 *
 * <p>The runtime starts inactive and has no fallback model, texture or material. A future approved resource reload
 * publishes one complete submission binding only after the exact Dragon derivation, inspected logical animation
 * binding and approved RenderType are all available. This prevents a registered entity renderer from silently
 * substituting placeholder art or an independently timed animation path.</p>
 */
public final class Region01BossClientRenderRuntime {
    private static final AtomicReference<Optional<SubmissionBinding>> CURRENT = new AtomicReference<>(Optional.empty());

    private Region01BossClientRenderRuntime() {}

    public static Optional<SubmissionBinding> current() {
        return CURRENT.get();
    }

    public static SubmissionBinding publish(
        BossCustomGeometryRenderPipeline pipeline,
        RenderType renderType,
        int packedOverlay,
        int packedColor
    ) {
        SubmissionBinding binding = new SubmissionBinding(pipeline, renderType, packedOverlay, packedColor);
        CURRENT.set(Optional.of(binding));
        return binding;
    }

    public static void clear() {
        CURRENT.set(Optional.empty());
    }

    public static boolean submit(
        int entityId,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight
    ) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(collector, "collector");
        Optional<SubmissionBinding> binding = CURRENT.get();
        if (binding.isEmpty()) return false;
        SubmissionBinding active = binding.orElseThrow();
        return active.pipeline().submitCurrent(
            entityId,
            poseStack,
            collector,
            active.renderType(),
            packedLight,
            active.packedOverlay(),
            active.packedColor()
        );
    }

    public record SubmissionBinding(
        BossCustomGeometryRenderPipeline pipeline,
        RenderType renderType,
        int packedOverlay,
        int packedColor
    ) {
        public SubmissionBinding {
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(renderType, "renderType");
        }
    }
}
