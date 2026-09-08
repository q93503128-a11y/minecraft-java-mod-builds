package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit production-asset boundary for boss presentation.
 *
 * <p>Presentation profiles reference stable logical keys. This manifest is the only place where those
 * logical keys are promoted to selected production resources and provenance. Keeping the two layers
 * separate prevents placeholder files or guessed paths from silently becoming production art.</p>
 */
public final class BossPresentationAssetManifest {
    public enum Kind { MODEL, ANIMATION, VFX, SOUND }

    public enum Code {
        MISSING_LOGICAL_KEY,
        WRONG_ASSET_KIND,
        MISSING_RESOURCE
    }

    @FunctionalInterface
    public interface ResourceProbe {
        boolean exists(Kind kind, ContentId resourceId);
    }

    public record Asset(
        ContentId logicalKey,
        Kind kind,
        ContentId resourceId,
        String source,
        String licenseNote
    ) {
        public Asset {
            Objects.requireNonNull(logicalKey, "logicalKey");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(resourceId, "resourceId");
            source = requireText(source, "source");
            licenseNote = requireText(licenseNote, "licenseNote");
        }
    }

    public record Issue(Code code, ContentId logicalKey, String message) { }

    public record Report(List<Issue> issues) {
        public Report { issues = List.copyOf(issues); }
        public boolean hasErrors() { return !issues.isEmpty(); }
        public List<Issue> byCode(Code code) {
            return issues.stream().filter(issue -> issue.code() == code).toList();
        }
    }

    private final Map<ContentId, Asset> assets;

    public BossPresentationAssetManifest(Collection<Asset> assets) {
        Objects.requireNonNull(assets, "assets");
        Map<ContentId, Asset> copy = new HashMap<>();
        for (Asset asset : assets) {
            Objects.requireNonNull(asset, "asset");
            Asset previous = copy.putIfAbsent(asset.logicalKey(), asset);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate presentation logical key: " + asset.logicalKey());
            }
        }
        this.assets = Map.copyOf(copy);
    }

    public Map<ContentId, Asset> assets() {
        return assets;
    }

    /** Read-only lookup used only after the manifest has passed its profile/resource gates. */
    public Optional<Asset> find(ContentId logicalKey) {
        return Optional.ofNullable(assets.get(Objects.requireNonNull(logicalKey, "logicalKey")));
    }

    /** Validates that every logical key referenced by the authored snapshot has one correctly typed selection. */
    public Report validateProfiles(Collection<BossPresentationProfile> profiles) {
        Objects.requireNonNull(profiles, "profiles");
        List<Issue> issues = new ArrayList<>();
        for (BossPresentationProfile profile : profiles) {
            require(profile.modelKey(), Kind.MODEL, issues);
            for (BossPresentationProfile.AssetBinding binding : profile.bindings().values()) {
                require(binding.animationKey(), Kind.ANIMATION, issues);
                require(binding.vfxKey(), Kind.VFX, issues);
                require(binding.soundKey(), Kind.SOUND, issues);
            }
        }
        return sorted(issues);
    }

    /**
     * Validates the physical resource boundary after assets have actually been selected.
     * The caller owns loader-specific path semantics; this class only guarantees that the selected resource
     * for each logical key exists according to the appropriate kind-aware probe.
     */
    public Report validateResources(ResourceProbe probe) {
        Objects.requireNonNull(probe, "probe");
        List<Issue> issues = new ArrayList<>();
        for (Asset asset : assets.values()) {
            if (!probe.exists(asset.kind(), asset.resourceId())) {
                issues.add(new Issue(Code.MISSING_RESOURCE, asset.logicalKey(),
                    "selected " + asset.kind() + " resource does not exist: " + asset.resourceId()));
            }
        }
        return sorted(issues);
    }

    private void require(ContentId logicalKey, Kind expected, List<Issue> issues) {
        Asset asset = assets.get(logicalKey);
        if (asset == null) {
            issues.add(new Issue(Code.MISSING_LOGICAL_KEY, logicalKey, "no selected production asset for " + expected));
            return;
        }
        if (asset.kind() != expected) {
            issues.add(new Issue(Code.WRONG_ASSET_KIND, logicalKey,
                "expected " + expected + " but manifest declares " + asset.kind()));
        }
    }

    private static Report sorted(List<Issue> issues) {
        issues.sort(Comparator.comparing((Issue issue) -> issue.logicalKey().toString())
            .thenComparing(issue -> issue.code().name())
            .thenComparing(Issue::message));
        return new Report(issues);
    }

    private static String requireText(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
