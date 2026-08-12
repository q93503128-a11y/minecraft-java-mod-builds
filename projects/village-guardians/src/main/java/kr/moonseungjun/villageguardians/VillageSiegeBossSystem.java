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

/** Three boss combat structures layered over the existing boss roster: breach, command ritual and duel. */
public final class VillageSiegeBossSystem {
    private static final Map<UUID, BossDoctrine> ACTIVE = new HashMap<>();
    private static final HashSet<UUID> PHASE_TWO = new HashSet<>();
    private static int ticks;
    private VillageSiegeBossSystem() {}

    public static void reset() { ACTIVE.clear(); PHASE_TWO.clear(); ticks = 0; }

    public static void tick(MinecraftServer server) {
        if (server == null || !VillageRaidSystem.isActive()) return;
        ticks++;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        if (ticks % 20 == 1) discover(level, center);
        for (UUID id : new HashSet<>(ACTIVE.keySet())) {
            var entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) { ACTIVE.remove(id); PHASE_TWO.remove(id); continue; }
            if (!PHASE_TWO.contains(id) && mob.getHealth() <= mob.getMaxHealth() * 0.50f) {
                PHASE_TWO.add(id);
                enterPhaseTwo(server, mob, ACTIVE.get(id));
            }
            switch (ACTIVE.get(id)) {
                case BREACH_COLOSSUS -> tickBreach(server, mob);
                case BONE_HIEROPHANT -> tickRitual(level, mob);
                case BLACK_MARSHAL -> tickDuel(server, mob);
            }
        }
    }

    public static String previewBossMechanic(int day) {
        BossDoctrine doctrine = BossDoctrine.values()[Math.floorMod(day, BossDoctrine.values().length)];
        return doctrine.displayName() + " · " + doctrine.description();
    }

    private static void discover(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, value -> value.isAlive())) {
            VillageEnemyArchetypeSystem.Archetype type = VillageRaidSystem.archetypeOf(mob);
            if (type == null || !VillageEnemyArchetypeSystem.isBoss(type) || ACTIVE.containsKey(mob.getUUID())) continue;
            BossDoctrine doctrine = BossDoctrine.values()[Math.floorMod(VillageCouncilState.currentDay(), BossDoctrine.values().length)];
            ACTIVE.put(mob.getUUID(), doctrine);
            Component old = mob.getCustomName();
            mob.setCustomName(Component.literal("§4[" + doctrine.displayName() + "] §f"
                    + (old == null ? type.displayName() : old.getString())));
            mob.setCustomNameVisible(true);
            if (doctrine == BossDoctrine.BREACH_COLOSSUS) {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60 * 30, 1));
            } else if (doctrine == BossDoctrine.BLACK_MARSHAL) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 30, 1));
            }
        }
    }

    private static void enterPhaseTwo(MinecraftServer server, Mob mob, BossDoctrine doctrine) {
        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60 * 30, 1));
        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 30, 1));
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "§4[보스 2페이즈] §f" + doctrine.displayName() + "의 전투 방식이 격화됩니다. · "
                        + doctrine.phaseTwo()), false);
    }

    private static void tickBreach(MinecraftServer server, Mob mob) {
        VillageAttackPlanSystem.Front front = VillageAttackPlanSystem.frontOf(mob.getUUID());
        VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.primarySideFor(front);
        if (segment == VillageSiegeSegmentSystem.Segment.NORTH_GATE && front == VillageAttackPlanSystem.Front.NORTH) return;
        BlockPos target = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());
        if (!VillageSiegeSegmentSystem.breached(segment)) {
            mob.setTarget(null);
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.18);
            if (ticks % 45 == 0 && VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) {
                int damage = PHASE_TWO.contains(mob.getUUID()) ? 72 : 48;
                VillageSiegeSegmentSystem.damage(server, segment, damage, mob.blockPosition());
            }
        }
    }

    private static void tickRitual(ServerLevel level, Mob boss) {
        if (ticks % 120 != Math.floorMod(boss.getUUID().hashCode(), 120)) return;
        for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, boss.position(), 15.0, 20, boss.getUUID())) {
            ally.heal(PHASE_TWO.contains(boss.getUUID()) ? 10.0f : 6.0f);
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100,
                    PHASE_TWO.contains(boss.getUUID()) ? 2 : 1));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                boss.getX(), boss.getY() + 1.2, boss.getZ(), 30, 2.0, 0.8, 2.0, 0.04);
    }

    private static void tickDuel(MinecraftServer server, Mob boss) {
        if (ticks % 35 != 0) return;
        ServerPlayer target = server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == boss.level() && player.isAlive() && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player) && player.distanceToSqr(boss) <= 42.0 * 42.0)
                .min(java.util.Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if (target == null) return;
        boss.setTarget(target);
        boss.getNavigation().moveTo(target, PHASE_TWO.contains(boss.getUUID()) ? 1.58 : 1.34);
        if (ticks % 105 == 0) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 1));
    }

    public enum BossDoctrine {
        BREACH_COLOSSUS("파성 거신", "성벽 구역을 직접 목표로 삼고 50% 이하에서 파쇄 주기가 빨라집니다.",
                "성벽 파쇄 피해 50% 증가"),
        BONE_HIEROPHANT("사령 결속자", "주변 공성 병력에 반복 보호막·회복 의식을 걸어 우선 처치가 필요합니다.",
                "의식 회복량과 보호막 단계 증가"),
        BLACK_MARSHAL("검은 결투원수", "시설보다 살아 있는 수호자를 추격하는 결투형 지휘관입니다.",
                "추격 속도 상승과 약화 베기 추가");
        private final String displayName, description, phaseTwo;
        BossDoctrine(String displayName, String description, String phaseTwo) {
            this.displayName = displayName; this.description = description; this.phaseTwo = phaseTwo;
        }
        public String displayName() { return displayName; }
        public String description() { return description; }
        public String phaseTwo() { return phaseTwo; }
    }
}
