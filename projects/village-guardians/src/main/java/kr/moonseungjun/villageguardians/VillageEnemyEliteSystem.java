package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Qualitative elite mutations: infiltration, area denial, assassination, plague and shock cavalry. */
public final class VillageEnemyEliteSystem {
    private static final Map<UUID, EliteDoctrine> ACTIVE = new HashMap<>();
    private static final Map<UUID, GrappleMotion> GRAPPLE_MOTIONS = new HashMap<>();
    private static final Map<UUID, FirebrandCast> FIREBRAND_CASTS = new HashMap<>();
    private static final Map<UUID, PlagueCast> PLAGUE_CASTS = new HashMap<>();
    private static int ticks;
    private VillageEnemyEliteSystem() {}

    public static void reset() {
        clearRaidState();
    }

    public static void clearRaidState() {
        ACTIVE.clear();
        GRAPPLE_MOTIONS.clear();
        FIREBRAND_CASTS.clear();
        PLAGUE_CASTS.clear();
        ticks = 0;
    }

    public static void forget(UUID uuid) {
        if (uuid == null) return;
        ACTIVE.remove(uuid);
        GRAPPLE_MOTIONS.remove(uuid);
        FIREBRAND_CASTS.remove(uuid);
        PLAGUE_CASTS.remove(uuid);
    }

    public static void tick(MinecraftServer server) {
        if (server == null || !VillageRaidSystem.isActive()) return;
        ticks++;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        if (ticks % 20 == 1) discover(level, center);
        for (UUID id : new HashSet<>(ACTIVE.keySet())) {
            var entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                ACTIVE.remove(id);
                GRAPPLE_MOTIONS.remove(id);
                FIREBRAND_CASTS.remove(id);
                PLAGUE_CASTS.remove(id);
                continue;
            }
            GrappleMotion motion = GRAPPLE_MOTIONS.get(id);
            if (motion != null) {
                tickGrapple(level, mob, motion);
                continue;
            }
            EliteDoctrine doctrine = ACTIVE.get(id);
            switch (doctrine) {
                case GRAPPLER -> grappler(level, mob);
                case FIREBRAND -> firebrand(level, server, mob);
                case ASSASSIN -> assassin(server, mob);
                case PLAGUE_WEAVER -> plague(level, server, mob);
                case SHOCK_RIDER -> shock(server, mob);
            }
        }
    }

    public static int expectedEliteCount(int day, int count) {
        if (day < 6) return 0;
        int divisor = day >= 12 ? 5 : day >= 9 ? 6 : 8;
        return Math.max(1, count / divisor);
    }

    public static String scoutSummary(int day, int count) {
        int elites = expectedEliteCount(day, count);
        if (elites <= 0) return "정예 변종 없음";
        return "정예 약 " + elites + "명 · 갈고리 침투/화염 투척/암살/역병/충격 기병 중 혼성";
    }

    private static void discover(ServerLevel level, BlockPos center) {
        if (VillageCouncilState.currentDay() < 6) return;
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                value -> VillageRaidSystem.isRaidEnemy(value) && value.isAlive())) {
            VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);
            if (archetype == null || VillageEnemyArchetypeSystem.isBoss(archetype) || ACTIVE.containsKey(mob.getUUID())) continue;
            int divisor = VillageCouncilState.currentDay() >= 12 ? 5 : VillageCouncilState.currentDay() >= 9 ? 6 : 8;
            if (Math.floorMod(mob.getUUID().hashCode(), divisor) != 0) continue;
            EliteDoctrine doctrine = EliteDoctrine.values()[Math.floorMod(mob.getUUID().hashCode() / 7, EliteDoctrine.values().length)];
            ACTIVE.put(mob.getUUID(), doctrine);
            Component old = mob.getCustomName();
            mob.setCustomName(Component.literal("§6[정예 · " + doctrine.displayName() + "] §f"
                    + (old == null ? "침공병" : old.getString())));
            mob.setCustomNameVisible(true);
            if (doctrine == EliteDoctrine.SHOCK_RIDER || doctrine == EliteDoctrine.ASSASSIN) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 30, 1));
            }
            if (doctrine == EliteDoctrine.GRAPPLER) {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60 * 30, 0));
            }
            VillageEnemyEffectSystem.eliteAura(level, mob, doctrine);
        }
    }

    private static void grappler(ServerLevel level, Mob mob) {
        int offset = Math.floorMod(mob.getUUID().hashCode(), 100);
        int phase = Math.floorMod(ticks - offset, 100);
        VillageAttackPlanSystem.Front front = VillageAttackPlanSystem.frontOf(mob.getUUID());
        if (front == VillageAttackPlanSystem.Front.NORTH) return;
        VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.primarySideFor(front);
        BlockPos wall = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());
        if (mob.blockPosition().distSqr(wall) > 144.0) return;
        BlockPos inside = VillageSiegeSegmentSystem.insideApproach(segment);
        Vec3 end = new Vec3(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        if (mob.position().distanceToSqr(end) <= 36.0) return;
        if (!level.getBlockState(inside).isAir() || !level.getBlockState(inside.above()).isAir()) return;
        if (phase == 88) {
            VillageEnemyEffectSystem.grappleLine(level, mob, mob.position().add(0.0, 1.0, 0.0),
                    end.add(0.0, 1.0, 0.0), 30);
            return;
        }
        if (phase != 0) return;
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoGravity(true);
        GRAPPLE_MOTIONS.put(mob.getUUID(), new GrappleMotion(mob.position(), end, ticks, 18));
    }

    private static void tickGrapple(ServerLevel level, Mob mob, GrappleMotion motion) {
        double progress = (ticks - motion.startTick()) / (double) Math.max(1, motion.duration());
        if (progress >= 1.0) {
            mob.snapTo(motion.end().x, motion.end().y, motion.end().z);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.setNoGravity(false);
            GRAPPLE_MOTIONS.remove(mob.getUUID());
            return;
        }
        double t = Math.max(0.0, Math.min(1.0, progress));
        Vec3 control = motion.start().lerp(motion.end(), 0.5).add(0.0,
                Math.max(4.0, motion.start().distanceTo(motion.end()) * 0.18), 0.0);
        Vec3 point = bezier(motion.start(), control, motion.end(), t);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
        mob.snapTo(point.x, point.y, point.z);
    }

    private static Vec3 bezier(Vec3 start, Vec3 control, Vec3 end, double t) {
        double u = 1.0 - t;
        return start.scale(u * u).add(control.scale(2.0 * u * t)).add(end.scale(t * t));
    }

    private static void firebrand(ServerLevel level, MinecraftServer server, Mob mob) {
        int offset = Math.floorMod(mob.getUUID().hashCode(), 100);
        int phase = Math.floorMod(ticks - offset, 100);
        if (phase == 82) {
            ServerPlayer target = nearbyPlayers(server, mob, 14.0).stream()
                    .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if (target == null) return;
            Vec3 impact = target.position();
            FIREBRAND_CASTS.put(mob.getUUID(), new FirebrandCast(impact, ticks + 18));
            VillageEnemyEffectSystem.firebrandThrow(level, mob, impact, 18);
            return;
        }
        if (phase != 0) return;
        FirebrandCast cast = FIREBRAND_CASTS.remove(mob.getUUID());
        if (cast == null || cast.dueTick() > ticks) return;
        double radius = 3.6;
        for (ServerPlayer player : nearbyPlayersAt(server, level, cast.impact(), radius)) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 70));
            player.hurtServer(level, level.damageSources().magic(), 2.5f + VillageCouncilState.currentDay() * 0.12f);
        }
        VillageEnemyEffectSystem.firebrandImpact(level, cast.impact(), radius);
    }

    private static void assassin(MinecraftServer server, Mob mob) {
        if (ticks % 40 != 0) return;
        ServerPlayer target = nearbyPlayers(server, mob, 28.0).stream()
                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if (target != null) {
            mob.setTarget(target);
            mob.getNavigation().moveTo(target, 1.48);
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 45, 0));
        }
    }

    private static void plague(ServerLevel level, MinecraftServer server, Mob mob) {
        int offset = Math.floorMod(mob.getUUID().hashCode(), 120);
        int phase = Math.floorMod(ticks - offset, 120);
        double radius = 9.0;
        if (phase == 100) {
            Vec3 center = mob.position();
            PLAGUE_CASTS.put(mob.getUUID(), new PlagueCast(center, ticks + 20));
            VillageEnemyEffectSystem.plagueWarning(level, mob, center, radius, 20);
            return;
        }
        if (phase != 0) return;
        PlagueCast cast = PLAGUE_CASTS.remove(mob.getUUID());
        if (cast == null || cast.dueTick() > ticks) return;
        for (ServerPlayer player : nearbyPlayersAt(server, level, cast.center(), radius)) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
        VillageEnemyEffectSystem.plagueImpact(level, cast.center(), radius);
    }

    private static void shock(MinecraftServer server, Mob mob) {
        if (ticks % 70 != Math.floorMod(mob.getUUID().hashCode(), 70)) return;
        ServerPlayer target = nearbyPlayers(server, mob, 24.0).stream()
                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if (target != null) {
            mob.setTarget(target);
            mob.getNavigation().moveTo(target, 1.62);
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 55, 1));
        }
    }

    private static java.util.List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        return nearbyPlayersAt(server, (ServerLevel) mob.level(), mob.position(), radius);
    }

    private static java.util.List<ServerPlayer> nearbyPlayersAt(
            MinecraftServer server, ServerLevel level, Vec3 center, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == level && player.isAlive() && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player)
                        && player.position().distanceToSqr(center) <= squared).toList();
    }

    private record GrappleMotion(Vec3 start, Vec3 end, int startTick, int duration) {}
    private record FirebrandCast(Vec3 impact, int dueTick) {}
    private record PlagueCast(Vec3 center, int dueTick) {}

    public enum EliteDoctrine {
        GRAPPLER("갈고리병"), FIREBRAND("화염 투척병"), ASSASSIN("침투 암살자"),
        PLAGUE_WEAVER("역병술사"), SHOCK_RIDER("돌파 기병");
        private final String displayName;
        EliteDoctrine(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
