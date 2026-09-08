package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        DUPLICATE_LOGICAL_KEY,
        MISSING_LOGICAL_KEY,
        WRONG_ASSET_KIND,
        MISSING_PROVENANCE,
        MISSING_LICENSE_NOTE
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
        return new Report(issues);
    }

    public Report validateCompleteness() {
        List<Issue> issues = new ArrayList<>();
        for (Asset asset : assets.values()) {
            if (asset.source().isBlank()) {
                issues.add(new Issue(Code.MISSING_PROVENANCE, asset.logicalKey(), "asset source is blank"));
            }
            if (asset.licenseNote().isBlank()) {
                issues.add(new Issue(Code.MISSING_LICENSE_NOTE, asset.logicalKey(), "asset license note is blank"));
            }
        }
        return new Report(issues);
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

    private static String requireText(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
