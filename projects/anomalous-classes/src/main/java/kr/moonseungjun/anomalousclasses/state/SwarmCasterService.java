package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import kr.moonseungjun.anomalousclasses.entity.SwarmEntities;
import kr.moonseungjun.anomalousclasses.entity.SwarmNodeEntity;
import kr.moonseungjun.anomalousclasses.state.SwarmSavedData.SwarmNodeRole;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** First playable server collision slice: actual monster targeting, arrows and finite N. */
public final class SwarmCasterService {
    private static final Map<UUID, EnumMap<SwarmNodeRole, SwarmNodeEntity>> NODES = new HashMap<>();
    private static MinecraftServer activeServer;
    private SwarmCasterService() {}

    public static boolean isSwarm(ServerPlayer player) {
        return player.getServer() != null && ClassProfileSavedData.get(player.getServer())
                .profile(player.getUUID()).currentClass().orElse(null) == ClassId.SWARM_CASTER;
    }

    public static SwarmSavedData.State current(ServerPlayer player) {
        return SwarmSavedData.get(player.getServer()).player(player.getUUID());
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Mob mob)
                || (!(mob instanceof Zombie) && !(mob instanceof AbstractSkeleton))) return;
        // Only these verified vanilla AI families are adapted; no blanket mob rewrite.
        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                mob, SwarmNodeEntity.class, 10, true, false,
                candidate -> candidate instanceof SwarmNodeEntity node && canHunt(mob, node)));
    }

    private static boolean canHunt(Mob attacker, SwarmNodeEntity node) {
        if (!node.isAlive() || node.ownerId() == null || node.count() == 0
                || !(attacker.level() instanceof ServerLevel level)) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(node.ownerId());
        if (owner == null || owner.level() != level || !owner.isAlive() || !isSwarm(owner)
                || attacker.isAlliedTo(owner) || owner.distanceToSqr(node) > 25.0) return false;
        var controlled = MentalDominatorSavedData.get(level.getServer()).controlled(attacker.getUUID());
        return controlled == null || controlled.linkState() != MentalDominatorSavedData.LinkState.ACTIVE;
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            NODES.values().forEach(map -> map.values().forEach(Entity::discard));
            NODES.clear();
            activeServer = server;
        }
        if (server.overworld().getGameTime() % 10 != 0) return;
        SwarmSavedData data = SwarmSavedData.get(server);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            online.add(id);
            if (!player.isAlive() || !isSwarm(player)) {
                clear(id);
                continue;
            }
            int level = ClassProfileSavedData.get(server).profile(id)
                    .progress(ClassId.SWARM_CASTER).level();
            SwarmSavedData.State state = data.player(id);
            if (state.advance(server.overworld().getGameTime(), SwarmRules.maximum(level)))
                data.changed();
            Monster target = nearestHostile(player.serverLevel(), player, 12);
            updateRole(player, state, SwarmNodeRole.GUARD, target, level);
            updateRole(player, state, SwarmNodeRole.ATTACK, target, level);
            updateRole(player, state, SwarmNodeRole.REGROUP, target, level);
        }
        for (UUID id : Set.copyOf(NODES.keySet())) if (!online.contains(id)) clear(id);
    }

    private static Monster nearestHostile(ServerLevel world, ServerPlayer owner, double radius) {
        Monster closest = null;
        double nearest = radius * radius;
        for (Monster mob : world.getEntitiesOfClass(Monster.class,
                owner.getBoundingBox().inflate(radius), m -> m.isAlive() && !owner.isAlliedTo(m))) {
            var controlled = MentalDominatorSavedData.get(world.getServer()).controlled(mob.getUUID());
            if (controlled != null && controlled.linkState() == MentalDominatorSavedData.LinkState.ACTIVE) continue;
            double distance = owner.distanceToSqr(mob);
            if (distance < nearest) { closest = mob; nearest = distance; }
        }
        return closest;
    }

    private static void updateRole(ServerPlayer player, SwarmSavedData.State state,
                                   SwarmNodeRole role, Monster threat, int level) {
        UUID id = player.getUUID();
        EnumMap<SwarmNodeRole, SwarmNodeEntity> map =
                NODES.computeIfAbsent(id, ignored -> new EnumMap<>(SwarmNodeRole.class));
        SwarmNodeEntity node = map.get(role);
        int count = state.count(role);
        if (count == 0) {
            if (node != null) node.discard();
            map.remove(role);
            return;
        }
        if (node == null || node.isRemoved() || node.level() != player.level()
                || node.distanceToSqr(player) > 32.0 * 32.0) {
            if (node != null) node.discard();
            node = SwarmEntities.NODE.get().create(player.serverLevel());
            if (node == null) return;
            node.initialize(id, role, count);
            node.moveTo(player.getX(), player.getY() + (role == SwarmNodeRole.ATTACK ? 0 : 1.0),
                    player.getZ(), player.getYRot(), 0);
            if (!player.serverLevel().addFreshEntity(node)) return;
            map.put(role, node);
        }
        node.setCount(count);
        if (role == SwarmNodeRole.ATTACK && threat != null) {
            if (node.tickCount % 10 == 0) node.getNavigation().moveTo(threat, 1.15);
            if (node.distanceToSqr(threat) < 2.25 && node.getSensing().hasLineOfSight(threat)
                    && node.tickCount % 20 == 0) {
                float damage = (float) Math.min(12.0, .8 + Math.sqrt(count) * .65 + level * .03);
                threat.hurt(node.damageSources().mobAttack(node), damage);
            }
        } else {
            node.getNavigation().stop();
            Vec3 intended = player.position().add(0, 1.1, 0);
            if (role == SwarmNodeRole.GUARD && threat != null) {
                Vec3 direction = threat.position().subtract(player.position());
                if (direction.horizontalDistanceSqr() > .01)
                    intended = intended.add(direction.multiply(1, 0, 1).normalize().scale(1.15));
            } else {
                intended = intended.add(role == SwarmNodeRole.GUARD ? .7 : -.7, 0, .3);
            }
            Vec3 displacement = intended.subtract(node.position());
            if (displacement.lengthSqr() > .01)
                node.move(MoverType.SELF, displacement.scale(Math.min(.9, .25 / Math.sqrt(displacement.lengthSqr()))));
        }
    }

    public static boolean friendlyHit(SwarmNodeEntity node, DamageSource source) {
        if (!(node.level() instanceof ServerLevel level) || node.ownerId() == null) return true;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(node.ownerId());
        if (owner == null || !isSwarm(owner)) return true;
        Entity attacker = source.getEntity();
        if (attacker == null) return false;
        if (attacker.getUUID().equals(owner.getUUID())) return true;
        if (attacker instanceof SwarmNodeEntity other && other.ownerId() != null)
            return other.ownerId().equals(node.ownerId());
        if (attacker instanceof PuppetEntity puppet && node.ownerId().equals(puppet.puppetOwner()))
            return true;
        if (attacker instanceof TamableAnimal tame && node.ownerId().equals(tame.getOwnerUUID()))
            return true;
        var controlled = MentalDominatorSavedData.get(level.getServer()).controlled(attacker.getUUID());
        if (controlled != null && controlled.ownerId().equals(node.ownerId())
                && controlled.linkState() == MentalDominatorSavedData.LinkState.ACTIVE) return true;
        return owner.isAlliedTo(attacker);
    }

    public static void onProjectile(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof SwarmNodeEntity node)
                || !(event.getProjectile() instanceof Projectile projectile)) return;
        Entity shooter = projectile.getOwner();
        if (shooter != null && friendlyHit(node, projectile.damageSources().indirectMagic(projectile, shooter)))
            event.setCanceled(true);
    }

    public static void onNodeHit(SwarmNodeEntity node, float damage) {
        if (node.ownerId() == null || node.role() == null || node.level().getServer() == null) return;
        EnumMap<SwarmNodeRole, SwarmNodeEntity> map = NODES.get(node.ownerId());
        if (map == null || map.get(node.role()) != node) return;
        ServerPlayer owner = node.level().getServer().getPlayerList().getPlayer(node.ownerId());
        if (owner == null || !isSwarm(owner)) return;
        int level = ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID())
                .progress(ClassId.SWARM_CASTER).level();
        SwarmSavedData data = SwarmSavedData.get(owner.getServer());
        SwarmSavedData.State state = data.player(owner.getUUID());
        int casualties = SwarmRules.casualties(damage, level, state.count(node.role()));
        if (state.lose(node.role(), casualties) > 0) {
            data.changed();
            node.setCount(state.count(node.role()));
            if (state.count(node.role()) == 0) {
                node.discard();
                map.remove(node.role());
            }
        }
    }

    public static void clear(UUID owner) {
        EnumMap<SwarmNodeRole, SwarmNodeEntity> nodes = NODES.remove(owner);
        if (nodes != null) nodes.values().forEach(Entity::discard);
    }

    public static void prepareClassChange(ServerPlayer player, ClassId previous) {
        if (previous == ClassId.SWARM_CASTER) clear(player.getUUID());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity().getUUID());
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clear(event.getEntity().getUUID());
    }
}
