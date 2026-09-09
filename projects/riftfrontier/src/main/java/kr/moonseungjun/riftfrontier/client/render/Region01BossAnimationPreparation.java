package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSampleBridge;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSourceBinding;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01BossRuntimeAsset;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;
import java.util.Objects;

/**
 * Typed preparation gate for a reviewed Region 01 boss logical-animation binding.
 *
 * <p>The capability can only be created from the exact {@link Region01BossGeometryPreparation.PreparedGeometry}
 * produced by the currently staged client resource reload. The supplied source binding must carry fine phase-window
 * review evidence, and every logical key is resolved only against the animation inventory imported from that exact
 * accepted runtime asset. No clip-name guessing, fallback clips, material choice or gameplay timing is introduced
 * here.</p>
 *
 * <p>Prepared animation remains tied to the same non-forgeable reload capability as geometry. A newer reload or
 * {@code clear()} invalidates later access, so a reviewed binding resolved against stale resource bytes cannot enter
 * renderer publication.</p>
 */
public final class Region01BossAnimationPreparation {
    private Region01BossAnimationPreparation() {}

    public static PreparedAnimation prepare(
        Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
        BossAnimationSourceBinding sourceBinding
    ) {
        Objects.requireNonNull(preparedGeometry, "preparedGeometry");
        Objects.requireNonNull(sourceBinding, "sourceBinding");

        Region01BossClientRenderRuntime.ValidatedReload reload = preparedGeometry.validatedReload();
        Region01BossRuntimeAsset runtimeAsset = preparedGeometry.runtimeAsset();
        BossAnimationSourceBinding reviewedBinding = sourceBinding.requireReviewedPhaseWindows();

        Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources =
            reviewedBinding.resolveWindows(runtimeAsset.animations());
        BossAnimationSampleBridge animationBridge = new BossAnimationSampleBridge(
            reviewedBinding,
            runtimeAsset.animations()
        );

        requireSamePreparedSource(preparedGeometry, reload, runtimeAsset);
        return new PreparedAnimation(
            preparedGeometry,
            reload,
            runtimeAsset,
            reviewedBinding,
            resolvedSources,
            animationBridge,
            reload.publicationGeneration(),
            reload.contentGeneration()
        );
    }

    private static void requireSamePreparedSource(
        Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
        Region01BossClientRenderRuntime.ValidatedReload reload,
        Region01BossRuntimeAsset runtimeAsset
    ) {
        if (preparedGeometry.validatedReload() != reload || preparedGeometry.runtimeAsset() != runtimeAsset) {
            throw new IllegalStateException("Region 01 boss animation preparation source changed during preparation");
        }
    }

    /** Immutable reviewed animation capability tied to one exact prepared geometry/reload transaction. */
    public static final class PreparedAnimation {
        private final Region01BossGeometryPreparation.PreparedGeometry preparedGeometry;
        private final Region01BossClientRenderRuntime.ValidatedReload reload;
        private final Region01BossRuntimeAsset runtimeAsset;
        private final BossAnimationSourceBinding sourceBinding;
        private final Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources;
        private final BossAnimationSampleBridge animationBridge;
        private final long publicationGeneration;
        private final long contentGeneration;

        private PreparedAnimation(
            Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
            Region01BossClientRenderRuntime.ValidatedReload reload,
            Region01BossRuntimeAsset runtimeAsset,
            BossAnimationSourceBinding sourceBinding,
            Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources,
            BossAnimationSampleBridge animationBridge,
            long publicationGeneration,
            long contentGeneration
        ) {
            this.preparedGeometry = Objects.requireNonNull(preparedGeometry, "preparedGeometry");
            this.reload = Objects.requireNonNull(reload, "reload");
            this.runtimeAsset = Objects.requireNonNull(runtimeAsset, "runtimeAsset");
            this.sourceBinding = Objects.requireNonNull(sourceBinding, "sourceBinding");
            this.resolvedSources = Map.copyOf(Objects.requireNonNull(resolvedSources, "resolvedSources"));
            this.animationBridge = Objects.requireNonNull(animationBridge, "animationBridge");
            this.publicationGeneration = publicationGeneration;
            this.contentGeneration = contentGeneration;
        }

        public Region01BossGeometryPreparation.PreparedGeometry preparedGeometry() {
            requireCurrent();
            return preparedGeometry;
        }

        public Region01BossRuntimeAsset runtimeAsset() {
            requireCurrent();
            return runtimeAsset;
        }

        public BossAnimationSourceBinding sourceBinding() {
            requireCurrent();
            return sourceBinding;
        }

        public Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources() {
            requireCurrent();
            return resolvedSources;
        }

        public BossAnimationSampleBridge animationBridge() {
            requireCurrent();
            return animationBridge;
        }

        public long publicationGeneration() {
            return publicationGeneration;
        }

        public long contentGeneration() {
            return contentGeneration;
        }

        /** Safe lifecycle probe for cross-package client reload orchestration. */
        public boolean isCurrent() {
            try {
                requireCurrent();
                return true;
            } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
                return false;
            }
        }

        Region01BossClientRenderRuntime.ValidatedReload validatedReload() {
            requireCurrent();
            return reload;
        }

        private void requireCurrent() {
            requireSamePreparedSource(preparedGeometry, reload, runtimeAsset);
            sourceBinding.requireReviewedPhaseWindows();
        }
    }
}
