package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Objects;
import java.util.Optional;

/**
 * GeckoLib 5 resource-id adapter for physically validated boss presentation assets.
 *
 * <p>GeckoLib 5's {@code GeoModel} APIs consume model/animation identifiers relative to the library's
 * {@code geckolib/models/} and {@code geckolib/animations/} roots and without JSON suffixes. The presentation
 * manifest, by contrast, stores exact resource-pack resources so the client {@link BossPresentationAssetManifest.ResourceProbe}
 * can prove the files exist. This class is the single fail-closed bridge between those two contracts.</p>
 */
public final class BossPresentationGeckoLibResourceId {
    private static final String MODEL_ROOT = "geckolib/models/";
    private static final String ANIMATION_ROOT = "geckolib/animations/";

    private BossPresentationGeckoLibResourceId() { }

    public static Optional<ContentId> model(ContentId physicalResource) {
        return relative(physicalResource, MODEL_ROOT, ".geo.json", ".json");
    }

    public static Optional<ContentId> animation(ContentId physicalResource) {
        return relative(physicalResource, ANIMATION_ROOT, ".animation.json", ".json");
    }

    private static Optional<ContentId> relative(
        ContentId physicalResource,
        String root,
        String preferredSuffix,
        String fallbackSuffix
    ) {
        Objects.requireNonNull(physicalResource, "physicalResource");
        String path = physicalResource.path();
        if (!path.startsWith(root)) {
            return Optional.empty();
        }

        String relative = path.substring(root.length());
        if (relative.endsWith(preferredSuffix)) {
            relative = relative.substring(0, relative.length() - preferredSuffix.length());
        } else if (relative.endsWith(fallbackSuffix)) {
            relative = relative.substring(0, relative.length() - fallbackSuffix.length());
        } else {
            return Optional.empty();
        }

        if (relative.isBlank() || relative.startsWith("/") || relative.endsWith("/")) {
            return Optional.empty();
        }
        return Optional.of(new ContentId(physicalResource.namespace(), relative));
    }
}
