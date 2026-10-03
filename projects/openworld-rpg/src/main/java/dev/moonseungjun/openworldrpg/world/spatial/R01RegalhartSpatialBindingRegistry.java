package dev.moonseungjun.openworldrpg.world.spatial;

import java.util.Objects;
import org.slf4j.Logger;

/** Validated bundled R01 Regalhart spatial binding. */
public final class R01RegalhartSpatialBindingRegistry {
    private static volatile R01RegalhartSpatialBindingData data;

    private R01RegalhartSpatialBindingRegistry() {
    }

    public static synchronized void initialize(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (data != null) {
            return;
        }
        data = R01RegalhartSpatialBindingLoader.loadBundled();
        logger.info(
                "Openworld RPG Regalhart spatial binding loaded: mapBuild={}, territory={}, coreArena={}, startAnchors={}.",
                data.mapBuild(),
                data.territory().id(),
                data.coreArena().id(),
                data.startAnchors().size()
        );
    }

    public static R01RegalhartSpatialBindingData data() {
        R01RegalhartSpatialBindingData current = data;
        if (current == null) {
            current = R01RegalhartSpatialBindingLoader.loadBundled();
            data = current;
        }
        return current;
    }
}
