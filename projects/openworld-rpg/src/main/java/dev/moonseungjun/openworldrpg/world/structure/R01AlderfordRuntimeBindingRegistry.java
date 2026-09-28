package dev.moonseungjun.openworldrpg.world.structure;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingRegistry;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;

/** Production-only Alderford service/property runtime geometry access. */
public final class R01AlderfordRuntimeBindingRegistry {
    private static volatile R01AlderfordRuntimeBindingData data;
    private static volatile R01StructureBindingData structures;

    private R01AlderfordRuntimeBindingRegistry() {
    }

    public static synchronized void initialize(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (data != null) {
            return;
        }
        structures = R01StructureBindingLoader.loadBundled();
        data = R01AlderfordRuntimeBindingLoader.loadBundled();
        logger.info(
                "Openworld RPG Alderford runtime bindings loaded: services={}, properties={}, productionReady={}.",
                data.services().size(),
                data.properties().size(),
                productionReady()
        );
    }

    public static R01AlderfordRuntimeBindingData data() {
        R01AlderfordRuntimeBindingData current = data;
        if (current == null) {
            current = R01AlderfordRuntimeBindingLoader.loadBundled();
            data = current;
        }
        return current;
    }

    public static R01StructureBindingData structures() {
        R01StructureBindingData current = structures;
        if (current == null) {
            current = R01StructureBindingLoader.loadBundled();
            structures = current;
        }
        return current;
    }

    public static Optional<R01AlderfordRuntimeBindingData.ProductionService>
    productionService(String serviceId) {
        return data().productionService(
                serviceId,
                structures(),
                R01SpatialBindingRegistry.data()
        );
    }

    public static Optional<R01AlderfordRuntimeBindingData.ProductionProperty>
    productionProperty(String propertyId) {
        return data().productionProperty(
                propertyId,
                structures(),
                R01SpatialBindingRegistry.data()
        );
    }

    public static boolean productionReady() {
        return data().productionReady(
                structures(),
                R01SpatialBindingRegistry.data()
        );
    }
}
