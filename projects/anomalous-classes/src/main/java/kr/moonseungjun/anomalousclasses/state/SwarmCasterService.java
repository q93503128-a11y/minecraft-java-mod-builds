package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    private static final Map<UUID, UUID> PRIORITY_TARGETS = new HashMap<>();
    private static final Map<UUID, RushOrder> RUSH_ORDERS = new HashMap<>();
    private static final double TARGET_RANGE = 48.0;
    private static final long RUSH_COOLDOWN = 160L;
    private static final long SACRIFICE_COOLDOWN = 240L;
    private static MinecraftServer activeServer;
    private record RushOrder(UUID target, long untilTick) {}
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
            PRIORITY_TARGETS.clear();
            RUSH_ORDERS.clear();
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
            Monster target = selectedEnemy(player);
            if (target == null) target = nearestHostile(player.serverLevel(), player, 12);
            updateRole(player, state, SwarmNodeRole.GUARD, target, level);
            updateRole(player, state, SwarmNodeRole.ATTACK, target, level);
            updateRole(player, state, SwarmNodeRole.REGROUP, target, level);
        }
        for (UUID id : Set.copyOf(NODES.keySet())) if (!online.contains(id)) clear(id);
    }

    /** R/Z/X carry an intent only. All aiming, ownership and combat validation is server-side. */
    public static void input(ServerPlayer player, int action) {
        if (!player.isAlive() || !isSwarm(player) || player.isSpectator()) return;
        switch (action) {
            case 0 -> designateTarget(player);
            case 1 -> chargeRush(player);
            case 2 -> sacrifice(player);
            default -> { } // Unknown packets never mutate state.
        }
    }

    private static boolean validEnemy(ServerPlayer owner, Monster enemy) {
        if (enemy == null || !enemy.isAlive() || owner.level() != enemy.level()
                || owner.distanceToSqr(enemy) > TARGET_RANGE * TARGET_RANGE
                || owner.isAlliedTo(enemy) || enemy.isAlliedTo(owner)) return false;
        var controlled = MentalDominatorSavedData.get(owner.getServer()).controlled(enemy.getUUID());
        return controlled == null || controlled.linkState() != MentalDominatorSavedData.LinkState.ACTIVE;
    }

    private static Monster selectedEnemy(ServerPlayer player) {
        UUID selected = PRIORITY_TARGETS.get(player.getUUID());
        if (selected == null) return null;
        Entity candidate = player.serverLevel().getEntity(selected);
        if (candidate instanceof Monster enemy && validEnemy(player, enemy)) return enemy;
        PRIORITY_TARGETS.remove(player.getUUID());
        RUSH_ORDERS.remove(player.getUUID());
        return null;
    }

    private static Monster aimedEnemy(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle().scale(TARGET_RANGE);
        Vec3 end = eye.add(direction);
        var hit = ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(direction).inflate(1.0),
                entity -> entity instanceof Monster mob && validEnemy(player, mob),
                TARGET_RANGE * TARGET_RANGE);
        if (hit == null || !(hit.getEntity() instanceof Monster target)) return null;
        BlockHitResult wall = player.serverLevel().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS
                && wall.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye))
            return null;
        return target;
    }

    private static void designateTarget(ServerPlayer player) {
        Monster target = aimedEnemy(player);
        if (target == null) {
            PRIORITY_TARGETS.remove(player.getUUID());
            RUSH_ORDERS.remove(player.getUUID());
            say(player, "message.anomalousclasses.swarm.target_cleared");
            return;
        }
        PRIORITY_TARGETS.put(player.getUUID(), target.getUUID());
        RUSH_ORDERS.remove(player.getUUID());
        say(player, "message.anomalousclasses.swarm.target_set", target.getDisplayName().getString());
    }

    private static SwarmNodeEntity attackers(ServerPlayer player) {
        var nodes = NODES.get(player.getUUID());
        if (nodes == null) return null;
        SwarmNodeEntity node = nodes.get(SwarmNodeRole.ATTACK);
        return node != null && !node.isRemoved() && node.isAlive()
                && node.level() == player.level() && node.count() > 0 ? node : null;
    }

    private static long tickTime(ServerPlayer player) {
        return player.getServer().overworld().getGameTime();
    }

    private static void chargeRush(ServerPlayer player) {
        SwarmSavedData.State state = current(player);
        long now = tickTime(player);
        if (state.cooldownRemaining(1, now) > 0) {
            cooldownMessage(player, state.cooldownRemaining(1, now));
            return;
        }
        SwarmNodeEntity node = attackers(player);
        if (node == null || state.attack() == 0) {
            say(player, "message.anomalousclasses.swarm.no_attackers");
            return;
        }
        Monster target = selectedEnemy(player);
        if (target == null) target = nearestHostile(player.serverLevel(), player, 12.0);
        if (target == null) {
            say(player, "message.anomalousclasses.swarm.no_target");
            return;
        }
        if (!state.commitCooldown(1, now, RUSH_COOLDOWN)) return;
        SwarmSavedData.get(player.getServer()).changed();
        RUSH_ORDERS.put(player.getUUID(), new RushOrder(target.getUUID(), now + 120L));
        say(player, "message.anomalousclasses.swarm.rush_started");
    }

    private static void sacrifice(ServerPlayer player) {
        SwarmSavedData.State state = current(player);
        long now = tickTime(player);
        if (state.cooldownRemaining(2, now) > 0) {
            cooldownMessage(player, state.cooldownRemaining(2, now));
            return;
        }
        SwarmNodeEntity node = attackers(player);
        int cost = SwarmRules.sacrificeCost(state.attack());
        if (node == null || cost == 0) {
            say(player, "message.anomalousclasses.swarm.no_attackers");
            return;
        }
        Monster target = selectedEnemy(player);
        if (target == null) target = nearestHostile(player.serverLevel(), player, 12);
        // Sacrifice may only occur at a *real* deployed attack position.
        if (target == null || node.distanceToSqr(target) > 9.0
                || !node.getSensing().hasLineOfSight(target)) {
            say(player, "message.anomalousclasses.swarm.not_in_range");
            return;
        }
        int level = ClassProfileSavedData.get(player.getServer())
                .profile(player.getUUID()).progress(ClassId.SWARM_CASTER).level();
        float damage = SwarmRules.sacrificeDamage(cost, level);
        if (!target.hurt(node.damageSources().mobAttack(node), damage)) {
            say(player, "message.anomalousclasses.swarm.no_hit");
            return;
        }
        // The actual, successful hit is the commit point: pay exactly once.
        if (!state.commitCooldown(2, now, SACRIFICE_COOLDOWN))
            throw new IllegalStateException("swarm sacrifice cooldown raced a server tick");
        int lost = state.lose(SwarmNodeRole.ATTACK, cost);
        if (lost != cost) throw new IllegalStateException("swarm sacrifice consumed mismatched units");
        node.setCount(state.attack());
        SwarmSavedData.get(player.getServer()).changed();
        if (state.attack() == 0) {
            node.discard();
            var nodes = NODES.get(player.getUUID());
            if (nodes != null) nodes.remove(SwarmNodeRole.ATTACK);
        }
        for (Monster nearby : player.serverLevel().getEntitiesOfClass(Monster.class,
                target.getBoundingBox().inflate(2.0),
                candidate -> candidate != target && validEnemy(player, candidate)
                        && node.distanceToSqr(candidate) < 10.0
                        && node.getSensing().hasLineOfSight(candidate))) {
            nearby.hurt(node.damageSources().mobAttack(node), damage * .48F);
        }
        say(player, "message.anomalousclasses.swarm.sacrifice_success", cost);
    }

    private static void cooldownMessage(ServerPlayer player, long remaining) {
        say(player, "message.anomalousclasses.swarm.cooldown", Math.max(1L, (remaining + 19) / 20));
    }

    private static void say(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    private static Monster nearestHostile(ServerLevel world, ServerPlayer owner, double radius) {
        Monster closest = null;
        double nearest = radius * radius;
        for (Monster mob : world.getEntitiesOfClass(Monster.class,
                owner.getBoundingBox().inflate(radius), m -> validEnemy(owner, m))) {
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
                || node.distanceToSqr(player) >
                    (role == SwarmNodeRole.ATTACK ? 64.0 * 64.0 : 32.0 * 32.0)) {
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
            long now = tickTime(player);
            RushOrder rush = RUSH_ORDERS.get(id);
            if (rush != null && (!rush.target().equals(threat.getUUID())
                    || now > rush.untilTick() || !validEnemy(player, threat))) {
                RUSH_ORDERS.remove(id);
                rush = null;
            }
            if (node.tickCount % 10 == 0)
                node.getNavigation().moveTo(threat, rush == null ? 1.15 : 1.45);
            if (rush != null && node.distanceToSqr(threat) < 3.24
                    && node.getSensing().hasLineOfSight(threat)) {
                RUSH_ORDERS.remove(id); // Never replay this cast on another tick.
                float burst = SwarmRules.rushDamage(count, level);
                if (threat.hurt(node.damageSources().mobAttack(node), burst)) {
                    int hits = 1;
                    for (Monster nearby : player.serverLevel().getEntitiesOfClass(Monster.class,
                            threat.getBoundingBox().inflate(1.6),
                            candidate -> candidate != threat && validEnemy(player, candidate)
                                    && node.distanceToSqr(candidate) < 9.0
                                    && node.getSensing().hasLineOfSight(candidate))) {
                        if (hits++ >= 3) break;
                        nearby.hurt(node.damageSources().mobAttack(node), burst * .55F);
                    }
                }
                say(player, "message.anomalousclasses.swarm.rush_landed");
            }
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
            // No hidden preset is saved. Until the player writes a tactic, the
            // regrouping colony still fights back against an immediate attacker.
            if (role == SwarmNodeRole.REGROUP && threat != null
                    && node.distanceToSqr(threat) < 2.25
                    && (threat.getTarget() == player || threat.getTarget() == node)
                    && node.tickCount % 30 == 0)
                threat.hurt(node.damageSources().mobAttack(node), .65F);
        }
    }

    public static boolean friendlyHit(SwarmNodeEntity node, DamageSource source) {
        return friendlyAttacker(node, source.getEntity());
    }

    private static boolean friendlyAttacker(SwarmNodeEntity node, Entity attacker) {
        if (!(node.level() instanceof ServerLevel level) || node.ownerId() == null) return true;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(node.ownerId());
        if (owner == null || !isSwarm(owner)) return true;
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
                || !(hit.getEntity() instanceof SwarmNodeEntity node)) return;
        // The event already exposes Projectile. Do not pattern-match its own type
        // or fabricate a DamageSource with ambiguous attacker ordering.
        Projectile projectile = event.getProjectile();
        if (friendlyAttacker(node, projectile.getOwner())) event.setCanceled(true);
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
        PRIORITY_TARGETS.remove(owner);
        RUSH_ORDERS.remove(owner);
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
