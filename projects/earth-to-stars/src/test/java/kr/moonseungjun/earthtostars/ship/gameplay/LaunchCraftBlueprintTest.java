package kr.moonseungjun.earthtostars.ship.gameplay;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchCraftBlueprintTest {
    @Test
    void starterCraftHasCoreSystemsButLeavesFirstOrbitUpgradeSlotsOpen() {
        UUID owner = UUID.randomUUID();
        ShipState ship = ShipState.create(ShipId.random(), owner, LaunchCraftBlueprint.slots());

        LaunchCraftBlueprint.installStarterModules(ship, ShipBootstrapCatalog.create());

        assertEquals(7, ship.slots().size());
        assertEquals(5, ship.modules().size());
        Set<String> definitions = ship.modules().values().stream()
                .map(module -> module.definitionId())
                .collect(Collectors.toSet());
        assertTrue(definitions.contains("command_core_mk1"));
        assertTrue(definitions.contains("engine_mk1"));
        assertTrue(definitions.contains("battery_mk1"));
        assertTrue(definitions.contains("cargo_mk1"));
        assertTrue(definitions.contains("life_support_mk1"));
        assertFalse(definitions.contains("autocannon_mk1"));
        assertFalse(definitions.contains("orbital_scanner_mk1"));
        assertTrue(ship.modules().values().stream().noneMatch(module -> module.slotId().equals("turret")));
        assertTrue(ship.modules().values().stream().noneMatch(module -> module.slotId().equals("sensor")));
    }
}
