package kr.moonseungjun.titanbreak.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

public final class NeedlerEntity extends Skeleton {
    public NeedlerEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        super.performRangedAttack(target, distanceFactor);
        double angle = getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = 4.0D + getRandom().nextDouble() * 4.0D;
        getNavigation().moveTo(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance, 1.15D);
    }
}
