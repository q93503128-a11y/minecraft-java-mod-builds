package dev.moonseungjun.openworldrpg.world.spatial;

import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules.RoomId;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Production-only access to R01 Quarry authored runtime geometry.
 *
 * <p>Pass-3 review shells remain inspectable as evidence through the raw spatial dataset, but
 * gameplay receives nothing here until separate final runtime volumes and spawn sockets are
 * explicitly promoted to production.</p>
 */
public final class R01QuarrySpatialBindingRegistry {
    private static volatile R01QuarrySpatialBindingData data;

    private R01QuarrySpatialBindingRegistry() {
    }

    public static synchronized void initialize(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (data != null) {
            return;
        }
        data = R01QuarrySpatialBindingLoader.loadBundled();
        logger.info(
                "Openworld RPG R01 Quarry runtime bindings loaded: rooms={}, productionReady={}.",
                data.rooms().size(),
                data.productionReady(R01SpatialBindingRegistry.data())
        );
    }

    public static R01QuarrySpatialBindingData data() {
        R01QuarrySpatialBindingData current = data;
        if (current == null) {
            current = R01QuarrySpatialBindingLoader.loadBundled();
            data = current;
        }
        return current;
    }

    public static Optional<R01QuarrySpatialBindingData.ProductionRoomBinding>
    productionRoom(RoomId room) {
        return data().productionRoom(room, R01SpatialBindingRegistry.data());
    }

    public static Optional<R01QuarrySpatialBindingData.ProductionStageBinding>
    productionRelay() {
        return data().productionRelay(R01SpatialBindingRegistry.data());
    }

    public static Optional<R01QuarrySpatialBindingData.ProductionStageBinding>
    productionEarthloongArena() {
        return data().productionEarthloongArena(
                R01SpatialBindingRegistry.data()
        );
    }

    public static boolean productionReady() {
        return data().productionReady(R01SpatialBindingRegistry.data());
    }
}
