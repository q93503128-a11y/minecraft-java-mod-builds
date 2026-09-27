package dev.moonseungjun.openworldrpg.world.spatial;

import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Validated bundled R01 spatial dataset.
 *
 * <p>Candidate data is intentionally loadable before final spatial acceptance, but normal gameplay
 * can only obtain production anchors/areas through the production-prefixed accessors. This keeps a
 * raw-world candidate from silently becoming spawn/quest authority.</p>
 */
public final class R01SpatialBindingRegistry {
    private static volatile R01SpatialBindingData data;

    private R01SpatialBindingRegistry() {
    }

    public static synchronized void initialize(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (data != null) {
            return;
        }
        data = R01SpatialBindingLoader.loadBundled();
        logger.info(
                "Openworld RPG R01 Azari spatial candidates loaded: mapBuild={}, anchors={}, areas={}, volumes={}, routes={}, productionReady={}.",
                data.mapBuild(),
                data.anchors().size(),
                data.areas().size(),
                data.volumes().size(),
                data.routes().size(),
                data.productionReady()
        );
    }

    public static R01SpatialBindingData data() {
        R01SpatialBindingData current = data;
        if (current == null) {
            current = R01SpatialBindingLoader.loadBundled();
            data = current;
        }
        return current;
    }

    public static Optional<R01SpatialBindingData.Anchor> productionAnchor(
            String anchorId
    ) {
        return data().productionAnchor(anchorId);
    }

    public static Optional<R01SpatialBindingData.Area> productionArea(
            String areaId
    ) {
        return data().productionArea(areaId);
    }

    public static Optional<R01SpatialBindingData.Volume> productionVolume(
            String volumeId
    ) {
        return data().productionVolume(volumeId);
    }

    public static boolean productionReady() {
        return data().productionReady();
    }
}
