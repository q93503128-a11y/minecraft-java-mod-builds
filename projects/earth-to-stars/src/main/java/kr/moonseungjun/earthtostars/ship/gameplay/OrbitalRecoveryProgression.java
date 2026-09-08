package kr.moonseungjun.earthtostars.ship.gameplay;

import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ModuleInstance;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;

import java.util.Objects;
import java.util.UUID;

public final class OrbitalRecoveryProgression {
    public static final String AUTOCANNON_MODULE_ID = "autocannon_mk1";
    public static final String SENSOR_MODULE_ID = "orbital_scanner_mk1";
    public static final String TURRET_SLOT_ID = "turret";
    public static final String SENSOR_SLOT_ID = "sensor";
    public static final double SENSOR_RANGE_MULTIPLIER = 1.5D;

    private OrbitalRecoveryProgression() {
    }

    public static boolean hasAutocannon(ShipState ship) {
        return hasModule(ship, AUTOCANNON_MODULE_ID);
    }

    public static boolean hasOrbitalScanner(ShipState ship) {
        return hasModule(ship, SENSOR_MODULE_ID);
    }

    public static boolean installRecoveredAutocannon(ShipState ship, ModuleCatalog catalog, UUID actorId) {
        return installIfFree(ship, catalog, actorId, AUTOCANNON_MODULE_ID, TURRET_SLOT_ID);
    }

    public static boolean installRecoveredScanner(ShipState ship, ModuleCatalog catalog, UUID actorId) {
        return installIfFree(ship, catalog, actorId, SENSOR_MODULE_ID, SENSOR_SLOT_ID);
    }

    public static double sensorRange(ShipState ship, double baseRange) {
        if (!Double.isFinite(baseRange) || baseRange <= 0.0D) {
            throw new IllegalArgumentException("baseRange must be finite and > 0");
        }
        return hasOrbitalScanner(ship) ? baseRange * SENSOR_RANGE_MULTIPLIER : baseRange;
    }

    private static boolean hasModule(ShipState ship, String definitionId) {
        Objects.requireNonNull(ship, "ship");
        return ship.modules().values().stream()
                .map(ModuleInstance::definitionId)
                .anyMatch(definitionId::equals);
    }

    private static boolean installIfFree(
            ShipState ship,
            ModuleCatalog catalog,
            UUID actorId,
            String definitionId,
            String slotId
    ) {
        Objects.requireNonNull(ship, "ship");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(actorId, "actorId");
        if (hasModule(ship, definitionId)) {
            return false;
        }
        boolean occupied = ship.modules().values().stream().anyMatch(module -> module.slotId().equals(slotId));
        if (occupied) {
            return false;
        }
        ship.installModule(
                actorId,
                catalog.require(definitionId),
                ModuleInstance.pristine(definitionId, slotId)
        );
        return true;
    }
}
