package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpitterEntity extends Zombie implements TitanGeoEntity {
    public SpitterEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new CorrosiveSpitGoal());
    }

    private final class CorrosiveSpitGoal extends Goal {
        private int cooldown = 35;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) <= 19.0D * 19.0D;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) return;
            double distance = distanceTo(target);
            if (distance < 4.0D) {
                Vec3 away = position().subtract(target.position());
                if (away.lengthSqr() > 1.0E-6D) {
                    away = away.normalize();
                    getNavigation().moveTo(getX() + away.x * 5.0D, getY(), getZ() + away.z * 5.0D, 1.15D);
                }
            }
            if (--cooldown > 0 || distance > 19.0D || !hasLineOfSight(target)) return;
            cooldown = 42 + getRandom().nextInt(22);
            spit(target);
        }
    }

    private void spit(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) return;
        swing(InteractionHand.MAIN_HAND);
        double sx = getX();
        double sy = getEyeY() - 0.18D;
        double sz = getZ();
        Arrow arrow = new Arrow(level, sx, sy, sz, Items.ARROW.getDefaultInstance(), null);
        arrow.setOwner(this);
        arrow.setBaseDamage(CombatScale.toInternal(18.0D));
        Vec3 aim = target.getEyePosition().add(target.getDeltaMovement().scale(4.0D))
                .subtract(sx, sy, sz);
        arrow.shoot(aim.x, aim.y, aim.z, 1.42F, 2.0F);
        level.addFreshEntity(arrow);

        double angle = getRandom().nextDouble() * Math.PI * 2.0D;
        double strafe = 3.0D + getRandom().nextDouble() * 3.0D;
        getNavigation().moveTo(getX() + Math.cos(angle) * strafe, getY(), getZ() + Math.sin(angle) * strafe, 1.08D);
    }
}
