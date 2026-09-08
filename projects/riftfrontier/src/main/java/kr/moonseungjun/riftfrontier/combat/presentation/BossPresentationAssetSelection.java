package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Objects;
import java.util.Optional;

/**
 * Client/rendering-side promotion gate from logical presentation keys to physical resource IDs.
 *
 * <p>The selection is constructed only when the complete manifest passes a kind-aware physical resource probe.
 * This keeps authored semantic resolution independent from loader/resource-pack details while preventing a client
 * renderer from consuming unverified paths. No fallback resource is guessed here.</p>
 */
public final class BossPresentationAssetSelection {
    private final BossPresentationAssetManifest manifest;

    private BossPresentationAssetSelection(BossPresentationAssetManifest manifest) {
        this.manifest = manifest;
    }

    public static Result validate(
        BossPresentationAssetManifest manifest,
        BossPresentationAssetManifest.ResourceProbe probe
    ) {
        Objects.requireNonNull(manifest, "manifest");
        BossPresentationAssetManifest.Report report = manifest.validateResources(Objects.requireNonNull(probe, "probe"));
        return report.hasErrors()
            ? new Result(report, Optional.empty())
            : new Result(report, Optional.of(new BossPresentationAssetSelection(manifest)));
    }

    public Optional<PhysicalPresentation> resolve(BossPresentationResolver.ResolvedPresentation logical) {
        Objects.requireNonNull(logical, "logical");
        Optional<BossPresentationAssetManifest.Asset> model = typed(logical.modelKey(), BossPresentationAssetManifest.Kind.MODEL);
        Optional<BossPresentationAssetManifest.Asset> animation = typed(logical.animationKey(), BossPresentationAssetManifest.Kind.ANIMATION);
        Optional<BossPresentationAssetManifest.Asset> vfx = typed(logical.vfxKey(), BossPresentationAssetManifest.Kind.VFX);
        Optional<BossPresentationAssetManifest.Asset> sound = typed(logical.soundKey(), BossPresentationAssetManifest.Kind.SOUND);
        if (model.isEmpty() || animation.isEmpty() || vfx.isEmpty() || sound.isEmpty()) return Optional.empty();

        return Optional.of(new PhysicalPresentation(
            logical.presentationProfile(),
            model.get().resourceId(),
            animation.get().resourceId(),
            vfx.get().resourceId(),
            sound.get().resourceId(),
            logical.phase(),
            logical.phaseProgress(),
            logical.hitWindowOpen()
        ));
    }

    private Optional<BossPresentationAssetManifest.Asset> typed(ContentId logicalKey, BossPresentationAssetManifest.Kind expected) {
        return manifest.find(logicalKey).filter(asset -> asset.kind() == expected);
    }

    public record Result(
        BossPresentationAssetManifest.Report report,
        Optional<BossPresentationAssetSelection> selection
    ) {
        public Result {
            Objects.requireNonNull(report, "report");
            selection = Objects.requireNonNull(selection, "selection");
            if (report.hasErrors() == selection.isPresent()) {
                throw new IllegalArgumentException("selection must exist exactly when resource validation succeeds");
            }
        }

        public boolean ready() {
            return selection.isPresent();
        }
    }

    public record PhysicalPresentation(
        ContentId presentationProfile,
        ContentId modelResource,
        ContentId animationResource,
        ContentId vfxResource,
        ContentId soundResource,
        kr.moonseungjun.riftfrontier.combat.AttackTimeline.Phase phase,
        double phaseProgress,
        boolean hitWindowOpen
    ) {
        public PhysicalPresentation {
            Objects.requireNonNull(presentationProfile, "presentationProfile");
            Objects.requireNonNull(modelResource, "modelResource");
            Objects.requireNonNull(animationResource, "animationResource");
            Objects.requireNonNull(vfxResource, "vfxResource");
            Objects.requireNonNull(soundResource, "soundResource");
            Objects.requireNonNull(phase, "phase");
            if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0D || phaseProgress > 1.0D) {
                throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
            }
            if (hitWindowOpen != (phase == kr.moonseungjun.riftfrontier.combat.AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("physical presentation hit window must exactly match ACTIVE phase");
            }
        }
    }
}
