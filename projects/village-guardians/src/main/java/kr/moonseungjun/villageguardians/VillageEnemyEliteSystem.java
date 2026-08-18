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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Qualitative elite mutations: infiltration, area denial, assassination, plague and shock cavalry. */
public final class VillageEnemyEliteSystem {
    private static final Map<UUID, EliteDoctrine> ACTIVE = new HashMap<>();
    private static int ticks;
    private VillageEnemyEliteSystem() {}

    public static void reset() { ACTIVE.clear(); ticks = 0; }

    public static void tick(MinecraftServer server) {
        if (server == null || !VillageRaidSystem.isActive()) return;
        ticks++;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        if (ticks % 20 == 1) discover(level, center);
        for (UUID id : new HashSet<>(ACTIVE.keySet())) {
            var entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) { ACTIVE.remove(id); continue; }
            EliteDoctrine doctrine = ACTIVE.get(id);
            switch (doctrine) {
                case GRAPPLER -> grappler(level, mob);
                case FIREBRAND -> firebrand(level, server, mob);
                case ASSASSIN -> assassin(server, mob);
                case PLAGUE_WEAVER -> plague(server, mob);
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
            if (doctrine == EliteDoctrine.GRAPPLER) mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60 * 30, 0));
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
        if (phase == 88) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    wall.getX() + 0.5, wall.getY() + 1.0, wall.getZ() + 0.5, 12, 0.7, 0.6, 0.7, 0.03);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 8, 0.35, 0.5, 0.35, 0.02);
            return;
        }
        if (phase != 0) return;
        BlockPos inside = VillageSiegeSegmentSystem.insideApproach(segment);
        if (level.getBlockState(inside).isAir() && level.getBlockState(inside.above()).isAir()) {
            mob.snapTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 18, 0.5, 0.7, 0.5, 0.04);
        }
    }

    private static void firebrand(ServerLevel level, MinecraftServer server, Mob mob) {
        int offset = Math.floorMod(mob.getUUID().hashCode(), 100);
        int phase = Math.floorMod(ticks - offset, 100);
        if (phase == 82) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 16, 0.8, 0.5, 0.8, 0.03);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + 0.8, mob.getZ(), 10, 0.6, 0.4, 0.6, 0.02);
            return;
        }
        if (phase != 0) return;
        for (ServerPlayer player : nearbyPlayers(server, mob, 10.0)) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 70));
            player.hurtServer(level, level.damageSources().magic(), 2.5f + VillageCouncilState.currentDay() * 0.12f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    player.getX(), player.getY() + 0.8, player.getZ(), 10, 0.35, 0.55, 0.35, 0.03);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                mob.getX(), mob.getY() + 1.0, mob.getZ(), 3, 0.8, 0.4, 0.8, 0.02);
    }

    private static void assassin(MinecraftServer server, Mob mob) {
        if (ticks % 40 != 0) return;
        ServerPlayer target = nearbyPlayers(server, mob, 28.0).stream()
                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if (target != null) {
            mob.setTarget(target);
            mob.getNavigation().moveTo(target, 1.48);
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 45, 0));
            if (mob.level() instanceof ServerLevel level) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        mob.getX(), mob.getY() + 0.8, mob.getZ(), 10, 0.35, 0.5, 0.35, 0.03);
            }
        }
    }

    private static void plague(MinecraftServer server, Mob mob) {
        int offset = Math.floorMod(mob.getUUID().hashCode(), 120);
        int phase = Math.floorMod(ticks - offset, 120);
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (phase == 100) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 20, 1.4, 0.6, 1.4, 0.04);
            return;
        }
        if (phase != 0) return;
        for (ServerPlayer player : nearbyPlayers(server, mob, 9.0)) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.7, player.getZ(), 9, 0.4, 0.5, 0.4, 0.03);
        }
    }

    private static void shock(MinecraftServer server, Mob mob) {
        if (ticks % 70 != Math.floorMod(mob.getUUID().hashCode(), 70)) return;
        ServerPlayer target = nearbyPlayers(server, mob, 24.0).stream()
                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if (target != null) {
            mob.setTarget(target);
            mob.getNavigation().moveTo(target, 1.62);
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 55, 1));
            if (mob.level() instanceof ServerLevel level) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        mob.getX(), mob.getY() + 0.9, mob.getZ(), 12, 0.45, 0.55, 0.45, 0.04);
            }
        }
    }

    private static java.util.List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == mob.level() && player.isAlive() && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player) && player.distanceToSqr(mob) <= squared).toList();
    }

    public enum EliteDoctrine {
        GRAPPLER("갈고리병"), FIREBRAND("화염 투척병"), ASSASSIN("침투 암살자"),
        PLAGUE_WEAVER("역병술사"), SHOCK_RIDER("돌파 기병");
        private final String displayName;
        EliteDoctrine(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
