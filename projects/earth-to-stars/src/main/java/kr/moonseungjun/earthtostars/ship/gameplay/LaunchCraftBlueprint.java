package kr.moonseungjun.earthtostars.ship.gameplay;

import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ModuleInstance;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;

import java.util.List;
import java.util.UUID;

/**
 * Canonical M1 starter craft loadout. The first launch craft deliberately leaves
 * the weapon hardpoint empty so orbital salvage can create the first meaningful
 * combat upgrade instead of giving the player every system at construction time.
 */
public final class LaunchCraftBlueprint {
    private static final List<ModuleSlot> SLOTS = List.of(
            new ModuleSlot("core", ModuleSlotType.CORE, 1),
            new ModuleSlot("engine", ModuleSlotType.PROPULSION, 1),
            new ModuleSlot("power", ModuleSlotType.POWER, 1),
            new ModuleSlot("cargo", ModuleSlotType.CARGO, 1),
            new ModuleSlot("life_support", ModuleSlotType.UTILITY, 1),
            new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1)
    );

    private LaunchCraftBlueprint() {
    }

    public static List<ModuleSlot> slots() {
        return SLOTS;
    }

    public static void installStarterModules(ShipState ship, ModuleCatalog catalog) {
        UUID owner = ship.ownerId();
        install(ship, catalog, owner, "command_core_mk1", "core");
        install(ship, catalog, owner, "engine_mk1", "engine");
        install(ship, catalog, owner, "battery_mk1", "power");
        install(ship, catalog, owner, "cargo_mk1", "cargo");
        install(ship, catalog, owner, "life_support_mk1", "life_support");
    }

    private static void install(ShipState ship, ModuleCatalog catalog, UUID owner, String definitionId, String slotId) {
        ship.installModule(
                owner,
                catalog.require(definitionId),
                ModuleInstance.pristine(definitionId, slotId)
        );
    }
}
