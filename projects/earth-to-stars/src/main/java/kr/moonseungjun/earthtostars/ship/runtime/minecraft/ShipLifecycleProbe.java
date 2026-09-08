package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.interior.InteriorRef;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.InteriorSavedData;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.ShipSavedData;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.ShipSystemsSavedData;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsRuntime;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsSnapshot;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsTuning;
import kr.moonseungjun.earthtostars.space.SpaceLevels;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ShipLifecycleProbe {
    private static final String ENV = "EARTH_TO_STARS_LIFECYCLE_PROBE";
    private static final ShipId PROBE_SHIP_ID = new ShipId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    private static final UUID PROBE_OWNER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final double PROBE_POWER = 37.5D;
    private static final int PROBE_AMMO = 73;

    private ShipLifecycleProbe() {
    }

    static void onServerStarted(MinecraftServer server) {
        String mode = System.getenv(ENV);
        if (mode == null || mode.isBlank()) {
            return;
        }
        try {
            switch (mode) {
                case "seed" -> seed(server);
                case "verify" -> verify(server);
                default -> throw new IllegalArgumentException("unknown lifecycle probe mode: " + mode);
            }
        } finally {
            server.execute(() -> server.halt(false));
        }
    }

    private static void seed(MinecraftServer server) {
        requireSpaceLevels(server);

        ShipState ship = ShipState.create(PROBE_SHIP_ID, PROBE_OWNER_ID, probeSlots());
        ShipSavedData.get(server).put(ship);
        InteriorRef interior = InteriorSavedData.get(server).getOrAllocate(PROBE_SHIP_ID);

        String ammoType = ShipSystemsTuning.P0.primaryAmmoType();
        ShipSystemsSnapshot snapshot = new ShipSystemsSnapshot(
                PROBE_SHIP_ID,
                PROBE_POWER,
                Map.of(ammoType, PROBE_AMMO)
        );
        ShipSystemsSavedData.get(server).put(snapshot);

        EarthToStars.LOGGER.info(
                "EARTH_TO_STARS_P0G_SEED_PASS ship={} slot={} power={} ammo={}",
                PROBE_SHIP_ID,
                interior.slot(),
                PROBE_POWER,
                PROBE_AMMO
        );
    }

    private static void verify(MinecraftServer server) {
        requireSpaceLevels(server);

        ShipState ship = ShipSavedData.get(server)
                .decodeAll(ShipBootstrapCatalog.create())
                .stream()
                .filter(candidate -> candidate.shipId().equals(PROBE_SHIP_ID))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("probe ship was not restored from disk"));
        if (!ship.ownerId().equals(PROBE_OWNER_ID)) {
            throw new IllegalStateException("probe ship owner changed across restart");
        }
        if (ship.slots().size() != probeSlots().size()) {
            throw new IllegalStateException("probe ship slots changed across restart");
        }

        InteriorRef interior = InteriorSavedData.get(server)
                .find(PROBE_SHIP_ID)
                .orElseThrow(() -> new IllegalStateException("probe interior assignment was not restored from disk"));

        ShipSystemsSnapshot persisted = ShipSystemsSavedData.get(server)
                .find(PROBE_SHIP_ID)
                .orElseThrow(() -> new IllegalStateException("probe systems state was not restored from disk"));
        assertProbeResources(persisted);

        ShipSystemsRuntime liveSystems = ShipSystemsManager.find(PROBE_SHIP_ID)
                .orElseThrow(() -> new IllegalStateException("systems manager did not initialize restored ship state"));
        assertProbeResources(liveSystems.snapshot());

        EarthToStars.LOGGER.info(
                "EARTH_TO_STARS_P0G_VERIFY_PASS ship={} slot={} power={} ammo={}",
                PROBE_SHIP_ID,
                interior.slot(),
                persisted.powerStored(),
                persisted.ammoAmounts().get(ShipSystemsTuning.P0.primaryAmmoType())
        );
    }

    private static void assertProbeResources(ShipSystemsSnapshot snapshot) {
        if (Math.abs(snapshot.powerStored() - PROBE_POWER) > 1.0E-9D) {
            throw new IllegalStateException("probe power changed across restart: " + snapshot.powerStored());
        }
        int ammo = snapshot.ammoAmounts().getOrDefault(ShipSystemsTuning.P0.primaryAmmoType(), -1);
        if (ammo != PROBE_AMMO) {
            throw new IllegalStateException("probe ammo changed across restart: " + ammo);
        }
    }

    private static void requireSpaceLevels(MinecraftServer server) {
        if (server.getLevel(SpaceLevels.ORBITAL_SPACE) == null) {
            throw new IllegalStateException("orbital_space did not boot on dedicated server");
        }
        if (server.getLevel(SpaceLevels.SHIP_INTERIORS) == null) {
            throw new IllegalStateException("ship_interiors did not boot on dedicated server");
        }
    }

    private static List<ModuleSlot> probeSlots() {
        return List.of(
                new ModuleSlot("core", ModuleSlotType.CORE, 1),
                new ModuleSlot("engine", ModuleSlotType.PROPULSION, 1),
                new ModuleSlot("power", ModuleSlotType.POWER, 1),
                new ModuleSlot("cargo", ModuleSlotType.CARGO, 1),
                new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1)
        );
    }
}
