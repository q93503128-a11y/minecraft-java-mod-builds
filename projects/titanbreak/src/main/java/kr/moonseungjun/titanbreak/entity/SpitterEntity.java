package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpitterEntity extends Zombie implements TitanGeoEntity {
    private int spitCooldown = 20;

    public SpitterEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (spitCooldown-- > 0) return;

        double distance = distanceTo(target);
        if (distance < 4.0D || distance > 19.0D || !hasLineOfSight(target)) {
            spitCooldown = 12;
            return;
        }

        swing(InteractionHand.MAIN_HAND);
        fireCorrosiveBolt(level, target);
        spitCooldown = 46 + getRandom().nextInt(24);

        Vec3 toward = target.position().subtract(position());
        if (toward.lengthSqr() > 0.01D) {
            toward = toward.normalize();
            double side = getRandom().nextBoolean() ? 1.0D : -1.0D;
            Vec3 strafe = new Vec3(-toward.z, 0.0D, toward.x).scale(3.0D * side);
            Vec3 retreat = target.position().subtract(toward.scale(8.0D)).add(strafe);
            getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.05D);
        }
    }

    private void fireCorrosiveBolt(ServerLevel level, LivingEntity target) {
        Arrow arrow = EntityType.ARROW.create(level, EntitySpawnReason.EVENT);
        if (arrow == null) return;
        arrow.setOwner(this);
        arrow.setPos(getX(), getEyeY() - 0.15D, getZ());

        Vec3 aim = target.getEyePosition().subtract(arrow.position())
                .add(target.getDeltaMovement().scale(4.0D));
        arrow.shoot(aim.x, aim.y, aim.z, 1.45F, 2.0F);
        arrow.setBaseDamage(CombatScale.toInternal(18.0D));
        level.addFreshEntity(arrow);
    }
}
