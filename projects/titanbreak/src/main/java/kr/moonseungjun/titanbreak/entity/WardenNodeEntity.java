package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Command elite that synchronizes nearby hostiles and drives them into a loose formation around its target. */
public final class WardenNodeEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    public WardenNodeEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 33;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 25;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 16 != 0) return;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        AABB area = getBoundingBox().inflate(18.0D);
        List<Mob> allies = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob.isAlive() && mob != this && mob instanceof TitanGeoEntity);

        int index = 0;
        for (Mob ally : allies) {
            ally.setTarget(target);
            ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 35, 0, false, false));
            ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 35, 0, false, false));

            double angle = (Math.PI * 2.0D * index) / Math.max(3, allies.size());
            double radius = 3.5D + (index % 2) * 1.5D;
            Vec3 slot = target.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            if (ally.distanceToSqr(slot) > 2.0D * 2.0D) {
                ally.getNavigation().moveTo(slot.x, slot.y, slot.z, 1.18D);
            }
            index++;
        }
    }
}
