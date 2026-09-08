package kr.moonseungjun.earthtostars.ship.persistence;

import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCategory;
import kr.moonseungjun.earthtostars.ship.domain.ModuleDefinition;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;

public final class ShipBootstrapCatalog {
    private ShipBootstrapCatalog() {
    }

    public static ModuleCatalog create() {
        ModuleCatalog catalog = new ModuleCatalog();
        catalog.register(new ModuleDefinition("command_core_mk1", ModuleCategory.COMMAND, ModuleSlotType.CORE, 1, 2.0D, 1.0D, 0.0D, 0.0D));
        catalog.register(new ModuleDefinition("engine_mk1", ModuleCategory.PROPULSION, ModuleSlotType.PROPULSION, 1, 4.0D, 3.0D, 0.0D, 0.0D));
        catalog.register(new ModuleDefinition("battery_mk1", ModuleCategory.POWER, ModuleSlotType.POWER, 1, 3.0D, 0.0D, 4.0D, 100.0D));
        catalog.register(new ModuleDefinition("cargo_mk1", ModuleCategory.CARGO, ModuleSlotType.CARGO, 1, 3.0D, 0.2D, 0.0D, 64.0D));
        catalog.register(new ModuleDefinition("autocannon_mk1", ModuleCategory.WEAPON, ModuleSlotType.WEAPON_HARDPOINT, 1, 5.0D, 2.0D, 0.0D, 0.0D));
        return catalog;
    }
}
