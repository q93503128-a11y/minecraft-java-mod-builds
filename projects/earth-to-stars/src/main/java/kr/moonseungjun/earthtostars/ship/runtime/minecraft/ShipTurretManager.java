package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.combat.TurretControlMode;
import kr.moonseungjun.earthtostars.ship.combat.TurretFireSolution;
import kr.moonseungjun.earthtostars.ship.combat.TurretProfile;
import kr.moonseungjun.earthtostars.ship.combat.TurretRuntime;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShipTurretManager {
    private static final double PROJECTILE_COLLISION_RADIUS = 0.55D;
    private static final Map<ShipId, TurretRuntime> TURRETS = new LinkedHashMap<>();
    private static final Map<UUID, ManualSession> MANUAL_SESSIONS = new HashMap<>();
    private static final List<ActiveShot> SHOTS = new ArrayList<>();

    private ShipTurretManager() {
    }

    public static boolean setMode(ServerPlayer player, TurretControlMode mode) {
        ShipState ship = resolveAccessibleShip(player).orElse(null);
        if (ship == null) return false;
        TurretRuntime turret = turret(ship);
        turret.setMode(player.getUUID(), mode);
        if (mode != TurretControlMode.MANUAL) MANUAL_SESSIONS.remove(player.getUUID());
        return true;
    }

    public static boolean requestManualControl(ServerPlayer player, long tick) {
        ShipState ship = resolveAccessibleShip(player).orElse(null);
        if (ship == null) return false;
        TurretRuntime turret = turret(ship);
        if (turret.mode() != TurretControlMode.MANUAL) return false;
        Optional<UUID> sessionId = turret.requestManualControl(player.getUUID(), tick);
        if (sessionId.isEmpty()) return false;
        MANUAL_SESSIONS.put(player.getUUID(), new ManualSession(ship.shipId(), sessionId.orElseThrow()));
        return true;
    }

    public static boolean releaseManualControl(UUID playerId) {
        ManualSession session = MANUAL_SESSIONS.remove(playerId);
        if (session == null) return false;
        TurretRuntime turret = TURRETS.get(session.shipId());
        return turret != null && turret.releaseManualControl(playerId);
    }

    public static boolean fireManual(ServerPlayer player, long tick) {
        ManualSession session = MANUAL_SESSIONS.get(player.getUUID());
        if (session == null) return false;
        TurretRuntime turret = TURRETS.get(session.shipId());
        ShipRuntimeManager.ExteriorAnchor anchor = ShipRuntimeManager.exteriorAnchor(session.shipId()).orElse(null);
        if (turret == null || anchor == null || !turret.ship().can(player.getUUID(), ShipPermission.WEAPON_CONTROL)) return false;

        Vec3 look = player.getLookAngle();
        ShipVec3 aim = new ShipVec3(look.x, look.y, look.z);
        if (!turret.acceptManualAim(player.getUUID(), session.sessionId(), tick, aim, tick)) return false;
        ShipVec3 muzzle = muzzle(anchor.transform());
        ShipSystemsRuntime systems = ShipSystemsManager.systems(turret.ship());
        Optional<TurretFireSolution> shot = turret.fireManual(
                player.getUUID(),
                session.sessionId(),
                systems,
                muzzle,
                anchor.transform().forward(),
                tick
        );
        shot.ifPresent(solution -> spawnShot(anchor.level(), session.shipId(), solution));
        return shot.isPresent();
    }

    public static TurretStatus status(ServerPlayer player) {
        ShipState ship = resolveAccessibleShip(player).orElse(null);
        if (ship == null) return TurretStatus.unavailable();
        TurretRuntime turret = turret(ship);
        ShipSystemsRuntime systems = ShipSystemsManager.systems(ship);
        return new TurretStatus(
                true,
                turret.mode(),
                systems.ammoAmount(turret.profile().ammoType()),
                systems.sensorGrid().contactCount(),
                turret.lease().isPresent()
        );
    }

    public static void tick(MinecraftServer server) {
        List<ShipId> stale = new ArrayList<>();
        for (Map.Entry<ShipId, TurretRuntime> mapEntry : TURRETS.entrySet()) {
            ShipId shipId = mapEntry.getKey();
            TurretRuntime turret = mapEntry.getValue();
            ShipRuntimeManager.ExteriorAnchor anchor = ShipRuntimeManager.exteriorAnchor(shipId).orElse(null);
            if (anchor == null) {
                stale.add(shipId);
                continue;
            }
            long tick = anchor.level().getGameTime();
            turret.expireLease(tick);
            ShipSystemsRuntime systems = ShipSystemsManager.systems(turret.ship());
            Optional<TurretFireSolution> autoShot = turret.tickAuto(
                    systems,
                    muzzle(anchor.transform()),
                    anchor.transform().forward(),
                    tick
            );
            autoShot.ifPresent(solution -> spawnShot(anchor.level(), shipId, solution));
        }
        stale.forEach(ShipTurretManager::removeShip);
        tickShots();
        MANUAL_SESSIONS.entrySet().removeIf(entry -> {
            TurretRuntime turret = TURRETS.get(entry.getValue().shipId());
            return turret == null || turret.lease().isEmpty() || !turret.lease().orElseThrow().controllerId().equals(entry.getKey());
        });
    }

    public static void removeShip(ShipId shipId) {
        TURRETS.remove(shipId);
        MANUAL_SESSIONS.entrySet().removeIf(entry -> entry.getValue().shipId().equals(shipId));
        SHOTS.removeIf(shot -> shot.shipId().equals(shipId));
    }

    public static void clear() {
        TURRETS.clear();
        MANUAL_SESSIONS.clear();
        SHOTS.clear();
    }

    private static Optional<ShipState> resolveAccessibleShip(ServerPlayer player) {
        return ShipRuntimeManager.accessibleShip(player, ShipPermission.WEAPON_CONTROL);
    }

    private static TurretRuntime turret(ShipState ship) {
        return TURRETS.computeIfAbsent(ship.shipId(), ignored -> new TurretRuntime(ship, TurretProfile.P0_AUTOCANNON));
    }

    private static ShipVec3 muzzle(ShipTransform transform) {
        return transform.position().add(transform.forward().scale(2.0D)).add(transform.up().scale(0.75D));
    }

    private static void spawnShot(ServerLevel level, ShipId shipId, TurretFireSolution solution) {
        SHOTS.add(new ActiveShot(
                level,
                shipId,
                solution.origin(),
                solution.direction().normalized().scale(solution.projectileSpeed()),
                solution.projectileLifetimeTicks(),
                solution.damage()
        ));
    }

    private static void tickShots() {
        Iterator<ActiveShot> iterator = SHOTS.iterator();
        while (iterator.hasNext()) {
            ActiveShot shot = iterator.next();
            if (shot.remainingTicks() <= 0) {
                iterator.remove();
                continue;
            }
            ShipVec3 next = shot.position().add(shot.velocity());
            LivingEntity hit = firstHostileCollision(shot.level(), next);
            if (hit != null) {
                hit.hurtServer(shot.level(), shot.level().damageSources().generic(), shot.damage());
                iterator.remove();
                continue;
            }
            shot.advance(next);
        }
    }

    private static LivingEntity firstHostileCollision(ServerLevel level, ShipVec3 position) {
        AABB hitBox = new AABB(
                position.x() - PROJECTILE_COLLISION_RADIUS,
                position.y() - PROJECTILE_COLLISION_RADIUS,
                position.z() - PROJECTILE_COLLISION_RADIUS,
                position.x() + PROJECTILE_COLLISION_RADIUS,
                position.y() + PROJECTILE_COLLISION_RADIUS,
                position.z() + PROJECTILE_COLLISION_RADIUS
        );
        return level.getEntities(
                        (Entity) null,
                        hitBox,
                        entity -> entity instanceof LivingEntity && entity instanceof Enemy && entity.isAlive()
                ).stream()
                .map(entity -> (LivingEntity) entity)
                .findFirst()
                .orElse(null);
    }

    public record TurretStatus(boolean available, TurretControlMode mode, int ammo, int contacts, boolean controlled) {
        static TurretStatus unavailable() {
            return new TurretStatus(false, TurretControlMode.OFF, 0, 0, false);
        }
    }

    private record ManualSession(ShipId shipId, UUID sessionId) {
    }

    private static final class ActiveShot {
        private final ServerLevel level;
        private final ShipId shipId;
        private ShipVec3 position;
        private final ShipVec3 velocity;
        private int remainingTicks;
        private final float damage;

        private ActiveShot(ServerLevel level, ShipId shipId, ShipVec3 position, ShipVec3 velocity, int remainingTicks, float damage) {
            this.level = level;
            this.shipId = shipId;
            this.position = position;
            this.velocity = velocity;
            this.remainingTicks = remainingTicks;
            this.damage = damage;
        }

        ServerLevel level() { return level; }
        ShipId shipId() { return shipId; }
        ShipVec3 position() { return position; }
        ShipVec3 velocity() { return velocity; }
        int remainingTicks() { return remainingTicks; }
        float damage() { return damage; }
        void advance(ShipVec3 next) { position = next; remainingTicks--; }
    }
}
