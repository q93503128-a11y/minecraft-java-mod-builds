package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.content.EarthToStarsItems;
import kr.moonseungjun.earthtostars.ship.combat.SensorContact;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.gameplay.OrbitalRecoveryProgression;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.ShipSavedData;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsRuntime;
import kr.moonseungjun.earthtostars.space.SpaceLevels;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M1-D first-orbit gameplay coordinator.
 *
 * Encounters are authoritative logical state. Visible salvage/interceptor meshes are
 * model-backed ItemDisplays and may be replaced without changing hit/mission rules.
 */
public final class OrbitalMissionManager {
    private static final ModuleCatalog CATALOG = ShipBootstrapCatalog.create();
    private static final double SALVAGE_FORWARD_DISTANCE = 28.0D;
    private static final double SALVAGE_SIDE_DISTANCE = 8.0D;
    private static final double SALVAGE_RECOVERY_RADIUS = 3.5D;
    private static final double HOSTILE_SPAWN_DISTANCE = 24.0D;
    private static final double HOSTILE_STANDOFF_DISTANCE = 14.0D;
    private static final double HOSTILE_SPEED = 0.20D;
    private static final double HOSTILE_COLLISION_RADIUS = 1.0D;
    private static final double HOSTILE_MAX_HEALTH = 28.0D;
    private static final double HOSTILE_POWER_DRAIN = 5.0D;
    private static final double HOSTILE_ATTACK_RANGE = 22.0D;
    private static final long HOSTILE_ATTACK_INTERVAL_TICKS = 30L;
    private static final long HOSTILE_GRACE_TICKS = 40L;

    private static final Map<ShipId, SalvageEncounter> SALVAGE = new LinkedHashMap<>();
    private static final Map<ShipId, HostileEncounter> HOSTILES = new LinkedHashMap<>();
    private static final Set<ShipId> HOSTILE_CLEARED = new HashSet<>();

    private OrbitalMissionManager() {
    }

    public static void tick(MinecraftServer server) {
        List<ShipId> live = new ArrayList<>();
        for (ShipState ship : ShipRuntimeManager.liveShips()) {
            ShipId shipId = ship.shipId();
            live.add(shipId);
            ShipRuntimeManager.ExteriorAnchor anchor = ShipRuntimeManager.exteriorAnchor(shipId).orElse(null);
            if (anchor == null || !anchor.level().dimension().equals(SpaceLevels.ORBITAL_SPACE)) {
                HOSTILE_CLEARED.remove(shipId);
                removeEncounter(shipId);
                continue;
            }

            if (OrbitalRecoveryProgression.hasOrbitalScanner(ship)) {
                HOSTILE_CLEARED.remove(shipId);
                removeEncounter(shipId);
                continue;
            }

            if (!OrbitalRecoveryProgression.hasAutocannon(ship)) {
                HOSTILE_CLEARED.remove(shipId);
                ensureSalvage(ship, anchor);
                tickSalvage(server, ship, anchor);
            } else if (HOSTILE_CLEARED.contains(shipId)) {
                removeEncounter(shipId);
            } else {
                removeSalvage(shipId);
                ensureHostile(ship, anchor);
                tickHostile(server, ship, anchor);
            }
        }

        SALVAGE.keySet().stream().filter(shipId -> !live.contains(shipId)).toList()
                .forEach(OrbitalMissionManager::removeSalvage);
        HOSTILES.keySet().stream().filter(shipId -> !live.contains(shipId)).toList()
                .forEach(OrbitalMissionManager::removeHostile);
        HOSTILE_CLEARED.removeIf(shipId -> !live.contains(shipId));
    }

    static List<SensorContact> sensorContacts(ShipId shipId) {
        HostileEncounter hostile = HOSTILES.get(shipId);
        if (hostile == null) {
            return List.of();
        }
        return List.of(new SensorContact(hostile.targetId(), hostile.position(), true, 25.0D));
    }

    static boolean tryHitHostile(
            ServerLevel level,
            ShipId firingShip,
            ShipVec3 projectilePosition,
            double projectileRadius,
            float damage
    ) {
        HostileEncounter hostile = HOSTILES.get(firingShip);
        if (hostile == null || hostile.level() != level) {
            return false;
        }
        double hitRadius = HOSTILE_COLLISION_RADIUS + projectileRadius;
        if (subtract(hostile.position(), projectilePosition).lengthSquared() > hitRadius * hitRadius) {
            return false;
        }

        hostile.damage(damage);
        if (hostile.health() <= 0.0D) {
            ShipState ship = ShipRuntimeManager.liveShips().stream()
                    .filter(candidate -> candidate.shipId().equals(firingShip))
                    .findFirst()
                    .orElse(null);
            ShipRuntimeManager.ExteriorAnchor anchor = ShipRuntimeManager.exteriorAnchor(firingShip).orElse(null);
            HOSTILE_CLEARED.add(firingShip);
            removeHostile(firingShip);
            if (ship != null && anchor != null) {
                dropSensorCore(anchor);
                notifyActiveCrew(level.getServer(), ship,
                        Component.translatable("message.earth_to_stars.orbit.hostile_destroyed"));
            }
        }
        return true;
    }

    public static ScannerInstallResult installRecoveredScanner(ServerPlayer player) {
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            return ScannerInstallResult.EARTH_ONLY;
        }
        ShipState ship = ShipRuntimeManager.accessibleShip(player, ShipPermission.MODULE_MANAGE).orElse(null);
        if (ship == null) {
            return ScannerInstallResult.NO_ACCESSIBLE_SHIP;
        }
        if (OrbitalRecoveryProgression.hasOrbitalScanner(ship)) {
            return ScannerInstallResult.ALREADY_INSTALLED;
        }
        if (!OrbitalRecoveryProgression.installRecoveredScanner(ship, CATALOG, player.getUUID())) {
            return ScannerInstallResult.SLOT_UNAVAILABLE;
        }
        HOSTILE_CLEARED.remove(ship.shipId());
        ShipSavedData.get(player.level().getServer()).put(ship);
        ShipSystemsManager.flush(player.level().getServer());
        return ScannerInstallResult.INSTALLED;
    }

    public static void removeShip(ShipId shipId) {
        HOSTILE_CLEARED.remove(shipId);
        removeEncounter(shipId);
    }

    public static void clear() {
        for (SalvageEncounter encounter : SALVAGE.values()) {
            SpaceVisualFactory.discard(encounter.visual());
        }
        for (HostileEncounter encounter : HOSTILES.values()) {
            SpaceVisualFactory.discard(encounter.visual());
        }
        SALVAGE.clear();
        HOSTILES.clear();
        HOSTILE_CLEARED.clear();
    }

    private static void ensureSalvage(ShipState ship, ShipRuntimeManager.ExteriorAnchor anchor) {
        SalvageEncounter existing = SALVAGE.get(ship.shipId());
        if (existing != null && existing.level() == anchor.level() && !existing.visual().isRemoved()) {
            return;
        }
        removeSalvage(ship.shipId());

        ShipTransform transform = anchor.transform();
        ShipVec3 position = transform.position()
                .add(transform.forward().scale(SALVAGE_FORWARD_DISTANCE))
                .add(transform.right().scale(SALVAGE_SIDE_DISTANCE))
                .add(transform.up().scale(2.0D));
        Display.ItemDisplay visual = SpaceVisualFactory.create(
                anchor.level(),
                EarthToStarsItems.ORBITAL_SALVAGE_VISUAL.get(),
                position,
                (float) transform.yawDegrees() + 35.0F,
                20.0F
        );
        if (anchor.level().addFreshEntity(visual)) {
            SALVAGE.put(ship.shipId(), new SalvageEncounter(anchor.level(), position, visual));
            notifyActiveCrew(anchor.level().getServer(), ship,
                    Component.translatable("message.earth_to_stars.orbit.salvage_detected"));
        }
    }

    private static void tickSalvage(MinecraftServer server, ShipState ship, ShipRuntimeManager.ExteriorAnchor anchor) {
        SalvageEncounter encounter = SALVAGE.get(ship.shipId());
        if (encounter == null || encounter.level() != anchor.level()) {
            return;
        }
        double distanceSquared = subtract(encounter.position(), anchor.transform().position()).lengthSquared();
        if (distanceSquared > SALVAGE_RECOVERY_RADIUS * SALVAGE_RECOVERY_RADIUS) {
            return;
        }

        if (!OrbitalRecoveryProgression.installRecoveredAutocannon(ship, CATALOG, ship.ownerId())) {
            return;
        }
        ShipSavedData.get(server).put(ship);
        removeSalvage(ship.shipId());
        ShipTurretManager.activateRecoveredAutocannon(ship);
        notifyActiveCrew(server, ship,
                Component.translatable("message.earth_to_stars.orbit.autocannon_recovered"));
        ensureHostile(ship, anchor);
    }

    private static void ensureHostile(ShipState ship, ShipRuntimeManager.ExteriorAnchor anchor) {
        if (OrbitalRecoveryProgression.hasOrbitalScanner(ship) || HOSTILE_CLEARED.contains(ship.shipId())) {
            return;
        }
        HostileEncounter existing = HOSTILES.get(ship.shipId());
        if (existing != null && existing.level() == anchor.level() && !existing.visual().isRemoved()) {
            return;
        }
        removeHostile(ship.shipId());

        ShipTransform transform = anchor.transform();
        ShipVec3 position = transform.position()
                .add(transform.right().scale(HOSTILE_SPAWN_DISTANCE))
                .add(transform.forward().scale(8.0D))
                .add(transform.up().scale(4.0D));
        Display.ItemDisplay visual = SpaceVisualFactory.create(
                anchor.level(),
                EarthToStarsItems.ORBITAL_INTERCEPTOR_VISUAL.get(),
                position,
                (float) transform.yawDegrees() - 90.0F,
                0.0F
        );
        if (anchor.level().addFreshEntity(visual)) {
            long tick = anchor.level().getGameTime();
            HOSTILES.put(ship.shipId(),
                    new HostileEncounter(anchor.level(), visual.getUUID(), position,
                            HOSTILE_MAX_HEALTH, tick, tick, visual));
            notifyActiveCrew(anchor.level().getServer(), ship,
                    Component.translatable("message.earth_to_stars.orbit.hostile_detected"));
        }
    }

    private static void tickHostile(MinecraftServer server, ShipState ship, ShipRuntimeManager.ExteriorAnchor anchor) {
        HostileEncounter hostile = HOSTILES.get(ship.shipId());
        if (hostile == null || hostile.level() != anchor.level()) {
            return;
        }
        if (hostile.visual().isRemoved()) {
            HOSTILES.remove(ship.shipId());
            ensureHostile(ship, anchor);
            return;
        }

        ShipVec3 toShip = subtract(anchor.transform().position(), hostile.position());
        double distance = toShip.length();
        ShipVec3 movement;
        if (distance > HOSTILE_STANDOFF_DISTANCE + 2.0D) {
            movement = toShip.normalized().scale(HOSTILE_SPEED);
        } else if (distance < HOSTILE_STANDOFF_DISTANCE - 2.0D) {
            movement = toShip.normalized().scale(-HOSTILE_SPEED * 0.75D);
        } else {
            ShipVec3 tangent = new ShipVec3(-toShip.z(), 0.0D, toShip.x()).normalized();
            movement = tangent.scale(HOSTILE_SPEED * 0.45D);
        }
        hostile.move(movement);
        float yaw = (float) Math.toDegrees(Math.atan2(-toShip.x(), toShip.z()));
        SpaceVisualFactory.apply(hostile.visual(), hostile.position(), yaw, 0.0F);

        long tick = anchor.level().getGameTime();
        if (tick - hostile.spawnTick() < HOSTILE_GRACE_TICKS
                || tick - hostile.lastAttackTick() < HOSTILE_ATTACK_INTERVAL_TICKS
                || distance > HOSTILE_ATTACK_RANGE) {
            return;
        }

        hostile.markAttack(tick);
        ShipSystemsRuntime systems = ShipSystemsManager.systems(ship);
        double drained = systems.drainPowerFromHostile(HOSTILE_POWER_DRAIN);
        if (drained > 0.0D) {
            ShipSystemsManager.flush(server);
        }
    }

    private static void dropSensorCore(ShipRuntimeManager.ExteriorAnchor anchor) {
        ShipVec3 position = anchor.transform().position().add(anchor.transform().up().scale(1.5D));
        ItemEntity reward = new ItemEntity(
                anchor.level(),
                position.x(), position.y(), position.z(),
                new ItemStack(EarthToStarsItems.RECOVERED_SENSOR_CORE.get())
        );
        reward.setNoGravity(true);
        anchor.level().addFreshEntity(reward);
    }

    private static void notifyActiveCrew(MinecraftServer server, ShipState ship, Component message) {
        List<ServerPlayer> active = ShipRuntimeManager.activeCrewPlayers(server, ship.shipId());
        if (active.isEmpty()) {
            Optional.ofNullable(server.getPlayerList().getPlayer(ship.ownerId()))
                    .ifPresent(player -> player.sendSystemMessage(message));
            return;
        }
        active.forEach(player -> player.sendSystemMessage(message));
    }

    private static void removeEncounter(ShipId shipId) {
        removeSalvage(shipId);
        removeHostile(shipId);
    }

    private static void removeSalvage(ShipId shipId) {
        SalvageEncounter removed = SALVAGE.remove(shipId);
        if (removed != null) {
            SpaceVisualFactory.discard(removed.visual());
        }
    }

    private static void removeHostile(ShipId shipId) {
        HostileEncounter removed = HOSTILES.remove(shipId);
        if (removed != null) {
            SpaceVisualFactory.discard(removed.visual());
        }
    }

    private static ShipVec3 subtract(ShipVec3 left, ShipVec3 right) {
        return new ShipVec3(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }

    public enum ScannerInstallResult {
        INSTALLED,
        EARTH_ONLY,
        NO_ACCESSIBLE_SHIP,
        ALREADY_INSTALLED,
        SLOT_UNAVAILABLE
    }

    private record SalvageEncounter(ServerLevel level, ShipVec3 position, Display.ItemDisplay visual) {
    }

    private static final class HostileEncounter {
        private final ServerLevel level;
        private final java.util.UUID targetId;
        private ShipVec3 position;
        private double health;
        private final long spawnTick;
        private long lastAttackTick;
        private final Display.ItemDisplay visual;

        private HostileEncounter(
                ServerLevel level,
                java.util.UUID targetId,
                ShipVec3 position,
                double health,
                long spawnTick,
                long lastAttackTick,
                Display.ItemDisplay visual
        ) {
            this.level = level;
            this.targetId = targetId;
            this.position = position;
            this.health = health;
            this.spawnTick = spawnTick;
            this.lastAttackTick = lastAttackTick;
            this.visual = visual;
        }

        ServerLevel level() { return level; }
        java.util.UUID targetId() { return targetId; }
        ShipVec3 position() { return position; }
        double health() { return health; }
        long spawnTick() { return spawnTick; }
        long lastAttackTick() { return lastAttackTick; }
        Display.ItemDisplay visual() { return visual; }
        void move(ShipVec3 delta) { position = position.add(delta); }
        void damage(double amount) { health = Math.max(0.0D, health - amount); }
        void markAttack(long tick) { lastAttackTick = tick; }
    }
}
