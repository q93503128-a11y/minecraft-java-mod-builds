package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.combat.SensorContact;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsRuntime;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsTuning;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShipSystemsManager {
    private static final Map<ShipId, ShipSystemsRuntime> SYSTEMS = new LinkedHashMap<>();

    private ShipSystemsManager() {
    }

    public static void tick(MinecraftServer server) {
        for (ShipState ship : ShipRuntimeManager.liveShips()) {
            ShipRuntimeManager.ExteriorAnchor anchor = ShipRuntimeManager.exteriorAnchor(ship.shipId()).orElse(null);
            if (anchor == null) {
                continue;
            }
            long tick = anchor.level().getGameTime();
            ShipSystemsRuntime systems = systems(ship);
            systems.beginTick(tick);
            systems.sensorGrid().expireOlderThan(tick, systems.tuning().sensorStaleTicks());
            if (shouldScan(ship.shipId(), tick, systems.tuning()) && systems.tryPowerSensorScan()) {
                systems.sensorGrid().update(scanContacts(anchor, systems.tuning().sensorRange()), tick);
            }
        }
    }

    static boolean allowPropulsion(ShipState ship, ShipControlInput input) {
        return systems(ship).tryPowerPropulsion(input);
    }

    static ShipSystemsRuntime systems(ShipState ship) {
        return SYSTEMS.computeIfAbsent(ship.shipId(), ShipSystemsRuntime::p0);
    }

    public static SystemStatus status(ServerPlayer player) {
        ShipState ship = ShipRuntimeManager.accessibleShip(player, ShipPermission.INTERIOR_ACCESS).orElse(null);
        if (ship == null) {
            return SystemStatus.unavailable();
        }
        ShipSystemsRuntime systems = systems(ship);
        String ammoType = systems.tuning().primaryAmmoType();
        return new SystemStatus(
                true,
                systems.powerStored(),
                systems.powerCapacity(),
                systems.generationPerTick(),
                systems.ammoAmount(ammoType),
                systems.ammoCapacity(ammoType),
                systems.sensorGrid().contactCount()
        );
    }

    public static void removeShip(ShipId shipId) {
        SYSTEMS.remove(shipId);
    }

    public static void clear() {
        SYSTEMS.clear();
    }

    private static boolean shouldScan(ShipId shipId, long tick, ShipSystemsTuning tuning) {
        int interval = tuning.sensorIntervalTicks();
        int phase = Math.floorMod(shipId.value().hashCode(), interval);
        return Math.floorMod(tick, interval) == phase;
    }

    private static List<SensorContact> scanContacts(ShipRuntimeManager.ExteriorAnchor anchor, double range) {
        ShipVec3 position = anchor.transform().position();
        AABB box = new AABB(
                position.x() - range,
                position.y() - range,
                position.z() - range,
                position.x() + range,
                position.y() + range,
                position.z() + range
        );
        List<Entity> entities = anchor.level().getEntities(
                (Entity) null,
                box,
                entity -> entity instanceof LivingEntity && !(entity instanceof ArmorStand) && entity.isAlive()
        );
        List<SensorContact> contacts = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            Vec3 pos = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            boolean hostile = entity instanceof Enemy;
            contacts.add(new SensorContact(
                    entity.getUUID(),
                    new ShipVec3(pos.x, pos.y, pos.z),
                    hostile,
                    hostile ? 10.0D : 0.0D
            ));
        }
        return contacts;
    }

    public record SystemStatus(
            boolean available,
            double powerStored,
            double powerCapacity,
            double generationPerTick,
            int ammo,
            int ammoCapacity,
            int contacts
    ) {
        static SystemStatus unavailable() {
            return new SystemStatus(false, 0.0D, 0.0D, 0.0D, 0, 0, 0);
        }
    }
}
