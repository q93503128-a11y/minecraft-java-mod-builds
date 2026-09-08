package kr.moonseungjun.earthtostars.ship;

import kr.moonseungjun.earthtostars.ship.domain.CrewRole;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCategory;
import kr.moonseungjun.earthtostars.ship.domain.ModuleDefinition;
import kr.moonseungjun.earthtostars.ship.domain.ModuleInstance;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.persistence.ShipStateCodec;
import kr.moonseungjun.earthtostars.ship.runtime.ShipRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipKernelTest {
    @Test
    void installsOnlyCompatibleModulesAndRejectsDuplicateInstances() {
        UUID owner = UUID.randomUUID();
        ShipState ship = newShip(owner);
        ModuleCatalog catalog = catalog();
        ModuleInstance engine = ModuleInstance.pristine("engine_mk1", "engine");

        ship.installModule(owner, catalog.require("engine_mk1"), engine);
        assertEquals(engine, ship.module(engine.instanceId()).orElseThrow());

        assertThrows(IllegalArgumentException.class,
                () -> ship.installModule(owner, catalog.require("engine_mk1"), engine));
        assertThrows(IllegalArgumentException.class,
                () -> ship.installModule(owner, catalog.require("cargo_mk1"), ModuleInstance.pristine("cargo_mk1", "engine")));
    }

    @Test
    void removalFreesSlotForReplacement() {
        UUID owner = UUID.randomUUID();
        ShipState ship = newShip(owner);
        ModuleCatalog catalog = catalog();
        ModuleInstance first = ModuleInstance.pristine("battery_mk1", "power");

        ship.installModule(owner, catalog.require("battery_mk1"), first);
        ship.removeModule(owner, first.instanceId());
        ModuleInstance replacement = ModuleInstance.pristine("battery_mk1", "power");
        ship.installModule(owner, catalog.require("battery_mk1"), replacement);

        assertEquals(1, ship.modules().size());
        assertTrue(ship.module(replacement.instanceId()).isPresent());
    }

    @Test
    void ownerControlsCrewPolicyAndGuestCannotMutateShip() {
        UUID owner = UUID.randomUUID();
        UUID crew = UUID.randomUUID();
        UUID guest = UUID.randomUUID();
        ShipState ship = newShip(owner);
        ModuleCatalog catalog = catalog();

        ship.assignRole(owner, crew, CrewRole.CREW);
        assertTrue(ship.can(crew, ShipPermission.PILOT));
        assertTrue(ship.can(crew, ShipPermission.WEAPON_CONTROL));
        assertTrue(ship.can(guest, ShipPermission.INTERIOR_ACCESS));
        assertFalse(ship.can(guest, ShipPermission.MODULE_MANAGE));

        assertThrows(SecurityException.class,
                () -> ship.assignRole(crew, guest, CrewRole.CREW));
        assertThrows(SecurityException.class,
                () -> ship.installModule(guest, catalog.require("cargo_mk1"), ModuleInstance.pristine("cargo_mk1", "cargo")));
    }

    @Test
    void persistenceRoundTripPreservesStableShipIdentityCrewSlotsAndModules() {
        UUID owner = UUID.randomUUID();
        UUID crew = UUID.randomUUID();
        ModuleCatalog catalog = catalog();
        ShipState ship = newShip(owner);
        ship.assignRole(owner, crew, CrewRole.CREW);
        ship.installModule(owner, catalog.require("command_core_mk1"), ModuleInstance.pristine("command_core_mk1", "core"));
        ship.installModule(owner, catalog.require("engine_mk1"), ModuleInstance.pristine("engine_mk1", "engine"));

        byte[] encoded = ShipStateCodec.encode(ship);
        ShipState restored = ShipStateCodec.decode(encoded, catalog);

        assertEquals(ship.shipId(), restored.shipId());
        assertEquals(owner, restored.ownerId());
        assertEquals(CrewRole.CREW, restored.roleOf(crew));
        assertEquals(ship.slots(), restored.slots());
        assertEquals(ship.modules(), restored.modules());
    }

    @Test
    void unknownPersistenceSchemaIsRejectedInsteadOfReset() {
        ModuleCatalog catalog = catalog();
        byte[] encoded = ShipStateCodec.encode(newShip(UUID.randomUUID()));
        encoded[4] = 0;
        encoded[5] = 0;
        encoded[6] = 0;
        encoded[7] = 2;

        assertThrows(IllegalArgumentException.class, () -> ShipStateCodec.decode(encoded, catalog));
    }

    @Test
    void repositoryRejectsDuplicateShipIdentity() {
        UUID owner = UUID.randomUUID();
        ShipState ship = newShip(owner);
        ShipRepository repository = new ShipRepository();
        repository.add(ship);

        assertThrows(IllegalArgumentException.class, () -> repository.add(ship));
        assertEquals(ship, repository.find(ship.shipId()).orElseThrow());
    }

    private static ShipState newShip(UUID owner) {
        return ShipState.create(new ShipId(UUID.randomUUID()), owner, List.of(
                new ModuleSlot("core", ModuleSlotType.CORE, 1),
                new ModuleSlot("engine", ModuleSlotType.PROPULSION, 1),
                new ModuleSlot("power", ModuleSlotType.POWER, 1),
                new ModuleSlot("cargo", ModuleSlotType.CARGO, 1),
                new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1)
        ));
    }

    private static ModuleCatalog catalog() {
        ModuleCatalog catalog = new ModuleCatalog();
        catalog.register(new ModuleDefinition("command_core_mk1", ModuleCategory.COMMAND, ModuleSlotType.CORE, 1, 2.0, 1.0, 0.0, 0.0));
        catalog.register(new ModuleDefinition("engine_mk1", ModuleCategory.PROPULSION, ModuleSlotType.PROPULSION, 1, 4.0, 3.0, 0.0, 0.0));
        catalog.register(new ModuleDefinition("battery_mk1", ModuleCategory.POWER, ModuleSlotType.POWER, 1, 3.0, 0.0, 4.0, 100.0));
        catalog.register(new ModuleDefinition("cargo_mk1", ModuleCategory.CARGO, ModuleSlotType.CARGO, 1, 3.0, 0.2, 0.0, 64.0));
        catalog.register(new ModuleDefinition("autocannon_mk1", ModuleCategory.WEAPON, ModuleSlotType.WEAPON_HARDPOINT, 1, 5.0, 2.0, 0.0, 0.0));
        return catalog;
    }
}
