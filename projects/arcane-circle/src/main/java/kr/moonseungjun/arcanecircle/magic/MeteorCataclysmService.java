package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** alpha.64 terminal ninth-circle cityfall catastrophe, invoked only for the delayed Crown Meteor. */
public final class MeteorCataclysmService {
    private MeteorCataclysmService() {}

    public static void crownImpact(ServerPlayer caster, Vec3 barrageCenter, double range, double power, long seed) {
        if (caster == null || barrageCenter == null) return;
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 center = crownCenter(barrageCenter, range, seed);
        entityCatastrophe(level, caster, center, range, power);
        terrainCatastrophe(caster, center, range, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 3.0F, .22F);
        int radius = (int) Math.round(NinthCircleMagnitude.meteorFieldRadius(range));
        ArcaneNoticeService.push(caster, Component.literal(
                "§4[메테오 스트라이크 · 도시 종말] §fCrown Meteor가 착탄했습니다. 약 "
                        + radius + "m 권역이 연쇄 붕괴합니다."), 135);
    }

    /** NPCs receive the same city-scale combat catastrophe; player-owned terrain mutation stays player-only. */
    public static void crownImpactNpc(ServerLevel level, Mob caster, Vec3 barrageCenter,
                                      double range, double power, long seed) {
        if (level == null || caster == null || barrageCenter == null || !caster.isAlive()) return;
        Vec3 center = crownCenter(barrageCenter, range, seed);
        entityCatastrophe(level, caster, center, range, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 3.0F, .22F);
    }

    private static Vec3 crownCenter(Vec3 barrageCenter, double range, long seed) {
        MeteorBarragePattern.Strike crown = MeteorBarragePattern.strike(seed, range,
                MeteorBarragePattern.crownIndex(range));
        return MeteorBarragePattern.position(barrageCenter, crown);
    }

    private static void entityCatastrophe(ServerLevel level, LivingEntity caster, Vec3 center,
                                          double range, double power) {
        double lethalRadius = NinthCircleMagnitude.crownLethalRadius(range);
        double shockRadius = NinthCircleMagnitude.crownShockRadius(range);
        double vertical = Math.max(42.0, shockRadius * .54);
        AABB box = new AABB(center, center).inflate(shockRadius, vertical, shockRadius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value != caster && value.isAlive() && !value.isRemoved() && !caster.isAlliedTo(value))) {
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > shockRadius + target.getBbWidth()) continue;
            double shock = Math.max(.12, 1.0 - distance / Math.max(1.0, shockRadius));
            double inner = distance <= lethalRadius ? 1.0 : Math.max(.0,
                    1.0 - (distance - lethalRadius) / Math.max(1.0, shockRadius - lethalRadius));
            double amount = power * (.58 + 1.62 * shock + .82 * inner)
                    + Math.min(power * 1.05, target.getMaxHealth() * (.10 + .30 * inner) * Math.max(.30, shock));
            ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, amount));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 320 + (int) (280 * shock)));
            Vec3 flat = new Vec3(dx, 0.0, dz);
            if (flat.lengthSqr() > 1.0E-8) flat = flat.normalize();
            double blast = .90 + shock * 3.25 + inner * 1.25;
            target.push(flat.x * blast, .65 + shock * 1.75 + inner * .80, flat.z * blast);
        }
    }

    private static void terrainCatastrophe(ServerPlayer caster, Vec3 center, double range, double power) {
        double field = NinthCircleMagnitude.meteorFieldRadius(range);
        // Each local crater remains inside DestructiveMagicService's per-cell safety cap.  The city
        // footprint is a lattice of bounded craters queued across later ticks, never one giant
        // synchronous sphere scan.
        DestructiveMagicService.impact(caster, "meteor_swarm", center.add(0, -1.4, 0),
                22.0, power * 2.75);

        double[] fractions = {.24, .47, .70, .92};
        int[] points = {8, 12, 16, 20};
        for (int ring = 0; ring < fractions.length; ring++) {
            int count = points[ring];
            double distance = field * fractions[ring];
            for (int i = 0; i < count; i++) {
                double a = i * Math.PI * 2.0 / count + ring * .31;
                double wobble = 1.0 + .055 * Math.sin(i * 2.17 + ring);
                Vec3 fracture = center.add(Math.cos(a) * distance * wobble,
                        -.55 - .18 * ((i + ring) % 3), Math.sin(a) * distance * wobble);
                double localRadius = 17.0 - ring * 1.8 + (i % 3) * .65;
                double localPower = power * (1.42 - ring * .15 + (i % 4 == 0 ? .10 : 0.0));
                DestructiveMagicService.impact(caster, "meteor_swarm", fracture, localRadius, localPower);
            }
        }
    }
}
