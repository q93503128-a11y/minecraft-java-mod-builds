package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Alpha.62 terminal ninth-circle Meteor Swarm catastrophe, invoked only for the crown strike. */
public final class MeteorCataclysmService {
    private MeteorCataclysmService() {}

    public static void crownImpact(ServerPlayer caster, Vec3 barrageCenter, double power, long seed) {
        if (caster == null || barrageCenter == null) return;
        ServerLevel level = (ServerLevel) caster.level();
        MeteorBarragePattern.Strike crown = MeteorBarragePattern.strike(seed, MeteorBarragePattern.crownIndex());
        Vec3 center = MeteorBarragePattern.position(barrageCenter, crown);

        // Entity catastrophe: the final impact owns a far wider pressure wave than ordinary meteors.
        double killRadius = 19.0;
        AABB box = new AABB(center, center).inflate(killRadius, 12.0, killRadius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value != caster && value.isAlive() && !value.isRemoved() && !caster.isAlliedTo(value))) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            if (distance > killRadius + target.getBbWidth()) continue;
            double falloff = Math.max(.32, 1.0 - distance / killRadius * .68);
            double amount = power * (1.42 * falloff) + Math.min(power * .70, target.getMaxHealth() * .18 * falloff);
            ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, amount));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260));
            Vec3 flat = target.position().subtract(center);
            flat = new Vec3(flat.x, 0.0, flat.z);
            if (flat.lengthSqr() > 1.0E-8) flat = flat.normalize();
            target.push(flat.x * (1.15 + falloff * 1.35), .82 + falloff * 1.15,
                    flat.z * (1.15 + falloff * 1.35));
        }

        // Terrain catastrophe: one dense basin plus two staggered fracture rings. DestructiveMagicService
        // tiles the oversized footprint across later ticks, preserving hard block-edit budgets.
        DestructiveMagicService.impact(caster, "meteor_swarm", center.add(0, -1.0, 0),
                22.0, power * 2.30);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0 / 12.0 + .17;
            double distance = 10.5 + (i % 3) * 2.2;
            Vec3 fracture = center.add(Math.cos(a) * distance, -.65 - .22 * (i % 2), Math.sin(a) * distance);
            DestructiveMagicService.impact(caster, "meteor_swarm", fracture,
                    9.0 + (i % 2) * 1.6, power * (1.35 - .04 * (i % 3)));
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0 + .39;
            Vec3 outer = center.add(Math.cos(a) * 22.0, -.32, Math.sin(a) * 22.0);
            DestructiveMagicService.impact(caster, "meteor_swarm", outer, 6.8, power * .88);
        }

        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 2.0F, .30F);
        ArcaneNoticeService.push(caster, Component.literal(
                "§4[메테오 스트라이크 · 종말 낙하] §fCrown Meteor가 착탄해 중심 지역을 소멸 분지로 붕괴시켰습니다."), 110);
    }
}
