package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure client-resource-pack path policy for selected boss presentation resources.
 *
 * <p>The asset manifest stores stable selected resource IDs without loader/library file suffixes. This policy is the
 * only place that turns those IDs into exact CLIENT_RESOURCES paths. It intentionally does not touch server datapack
 * resources and does not invent a fallback when a kind/path combination is invalid.</p>
 */
public final class BossPresentationClientResourcePath {
    private BossPresentationClientResourcePath() { }

    public static Optional<ContentId> resolve(
        BossPresentationAssetManifest.Kind kind,
        ContentId selectedResource
    ) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(selectedResource, "selectedResource");

        String path = selectedResource.path();
        String physicalPath = switch (kind) {
            case MODEL -> modelPath(path);
            case ANIMATION -> animationPath(path);
            case VFX -> vfxPath(path);
            case SOUND -> soundPath(path);
        };
        return physicalPath == null
            ? Optional.empty()
            : Optional.of(new ContentId(selectedResource.namespace(), physicalPath));
    }

    private static String modelPath(String path) {
        if (path.startsWith("models/")) return appendIfMissing(path, ".json");
        if (path.startsWith("geo/")) return appendIfMissing(path, ".geo.json");
        return null;
    }

    private static String animationPath(String path) {
        if (!path.startsWith("animations/")) return null;
        return appendIfMissing(path, ".animation.json");
    }

    private static String vfxPath(String path) {
        if (!path.startsWith("vfx/") && !path.startsWith("particles/")) return null;
        return appendIfMissing(path, ".json");
    }

    private static String soundPath(String path) {
        if (!path.startsWith("sounds/")) return null;
        return appendIfMissing(path, ".ogg");
    }

    private static String appendIfMissing(String path, String suffix) {
        return path.endsWith(suffix) ? path : path + suffix;
    }
}
