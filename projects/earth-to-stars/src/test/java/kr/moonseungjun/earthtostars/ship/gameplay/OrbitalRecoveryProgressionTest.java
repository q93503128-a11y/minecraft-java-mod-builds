package kr.moonseungjun.earthtostars.ship.gameplay;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitalRecoveryProgressionTest {
    @Test
    void firstOrbitRecoveryUnlocksWeaponThenScannerCapability() {
        UUID owner = UUID.randomUUID();
        ShipState ship = ShipState.create(ShipId.random(), owner, LaunchCraftBlueprint.slots());
        var catalog = ShipBootstrapCatalog.create();
        LaunchCraftBlueprint.installStarterModules(ship, catalog);

        assertFalse(OrbitalRecoveryProgression.hasAutocannon(ship));
        assertFalse(OrbitalRecoveryProgression.hasOrbitalScanner(ship));
        assertEquals(64.0D, OrbitalRecoveryProgression.sensorRange(ship, 64.0D), 1.0E-9D);

        assertTrue(OrbitalRecoveryProgression.installRecoveredAutocannon(ship, catalog, owner));
        assertTrue(OrbitalRecoveryProgression.hasAutocannon(ship));
        assertFalse(OrbitalRecoveryProgression.installRecoveredAutocannon(ship, catalog, owner));

        assertTrue(OrbitalRecoveryProgression.installRecoveredScanner(ship, catalog, owner));
        assertTrue(OrbitalRecoveryProgression.hasOrbitalScanner(ship));
        assertEquals(96.0D, OrbitalRecoveryProgression.sensorRange(ship, 64.0D), 1.0E-9D);
        assertFalse(OrbitalRecoveryProgression.installRecoveredScanner(ship, catalog, owner));
    }
}
