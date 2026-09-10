package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSampleBridge;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSemanticBinding;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSourceBinding;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01BossRuntimeAsset;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;
import java.util.Objects;

/**
 * Typed preparation gate for a semantically authorized, visually reviewed Region 01 boss animation binding.
 *
 * <p>The capability can only be created from the exact prepared geometry produced by the currently staged client
 * resource reload. The supplied semantic binding must already prove that reviewed source windows cover the logical
 * animation keys required by validated server-authoritative boss semantics. Every key is then resolved only against
 * the animation inventory imported from that exact accepted runtime asset.</p>
 */
public final class Region01BossAnimationPreparation {
    private Region01BossAnimationPreparation() {}

    public static PreparedAnimation prepare(
        Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
        BossAnimationSemanticBinding semanticBinding
    ) {
        Objects.requireNonNull(preparedGeometry, "preparedGeometry");
        Objects.requireNonNull(semanticBinding, "semanticBinding");

        Region01BossClientRenderRuntime.ValidatedReload reload = preparedGeometry.validatedReload();
        Region01BossRuntimeAsset runtimeAsset = preparedGeometry.runtimeAsset();
        BossAnimationSourceBinding reviewedBinding = semanticBinding.sourceBinding().requireReviewedPhaseWindows();

        Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources =
            reviewedBinding.resolveWindows(runtimeAsset.animations());
        if (!resolvedSources.keySet().equals(semanticBinding.requiredLogicalAnimationKeys())) {
            throw new IllegalStateException("prepared boss animation did not resolve exact server-required logical keys");
        }
        BossAnimationSampleBridge animationBridge = new BossAnimationSampleBridge(reviewedBinding, runtimeAsset.animations());

        requireSamePreparedSource(preparedGeometry, reload, runtimeAsset);
        return new PreparedAnimation(
            preparedGeometry,
            reload,
            runtimeAsset,
            semanticBinding,
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

    /** Immutable semantic+reviewed animation capability tied to one exact prepared geometry/reload transaction. */
    public static final class PreparedAnimation {
        private final Region01BossGeometryPreparation.PreparedGeometry preparedGeometry;
        private final Region01BossClientRenderRuntime.ValidatedReload reload;
        private final Region01BossRuntimeAsset runtimeAsset;
        private final BossAnimationSemanticBinding semanticBinding;
        private final BossAnimationSourceBinding sourceBinding;
        private final Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources;
        private final BossAnimationSampleBridge animationBridge;
        private final long publicationGeneration;
        private final long contentGeneration;

        private PreparedAnimation(
            Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
            Region01BossClientRenderRuntime.ValidatedReload reload,
            Region01BossRuntimeAsset runtimeAsset,
            BossAnimationSemanticBinding semanticBinding,
            BossAnimationSourceBinding sourceBinding,
            Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources,
            BossAnimationSampleBridge animationBridge,
            long publicationGeneration,
            long contentGeneration
        ) {
            this.preparedGeometry = Objects.requireNonNull(preparedGeometry, "preparedGeometry");
            this.reload = Objects.requireNonNull(reload, "reload");
            this.runtimeAsset = Objects.requireNonNull(runtimeAsset, "runtimeAsset");
            this.semanticBinding = Objects.requireNonNull(semanticBinding, "semanticBinding");
            this.sourceBinding = Objects.requireNonNull(sourceBinding, "sourceBinding");
            this.resolvedSources = Map.copyOf(Objects.requireNonNull(resolvedSources, "resolvedSources"));
            this.animationBridge = Objects.requireNonNull(animationBridge, "animationBridge");
            this.publicationGeneration = publicationGeneration;
            this.contentGeneration = contentGeneration;
        }

        public Region01BossGeometryPreparation.PreparedGeometry preparedGeometry() { requireCurrent(); return preparedGeometry; }
        public Region01BossRuntimeAsset runtimeAsset() { requireCurrent(); return runtimeAsset; }
        public BossAnimationSemanticBinding semanticBinding() { requireCurrent(); return semanticBinding; }
        public BossAnimationSourceBinding sourceBinding() { requireCurrent(); return sourceBinding; }
        public Map<ContentId, BossAnimationSourceBinding.ResolvedSource> resolvedSources() { requireCurrent(); return resolvedSources; }
        public BossAnimationSampleBridge animationBridge() { requireCurrent(); return animationBridge; }
        public long publicationGeneration() { return publicationGeneration; }
        public long contentGeneration() { return contentGeneration; }

        public boolean isCurrent() {
            try {
                requireCurrent();
                return true;
            } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
                return false;
            }
        }

        Region01BossClientRenderRuntime.ValidatedReload validatedReload() { requireCurrent(); return reload; }

        private void requireCurrent() {
            requireSamePreparedSource(preparedGeometry, reload, runtimeAsset);
            BossAnimationSourceBinding current = semanticBinding.sourceBinding().requireReviewedPhaseWindows();
            if (current != sourceBinding || !resolvedSources.keySet().equals(semanticBinding.requiredLogicalAnimationKeys())) {
                throw new IllegalStateException("Region 01 boss semantic animation preparation no longer retains exact reviewed coverage");
            }
        }
    }
}
