package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.BreachService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CrusherEntity extends Zombie implements TitanGeoEntity {
    private LivingEntity slamTarget;
    private int windupTicks;
    private int slamCooldown;

    public CrusherEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 16;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || slamCooldown > 0 || windupTicks > 0) return false;
        slamTarget = living;
        windupTicks = 20;
        slamCooldown = 55;
        getNavigation().stop();
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (slamCooldown > 0) slamCooldown--;
        if (windupTicks <= 0) return;

        windupTicks--;
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.30D, motion.y, motion.z * 0.30D);
        if (slamTarget != null && slamTarget.isAlive()) getLookControl().setLookAt(slamTarget, 30.0F, 30.0F);
        if (windupTicks == 0) releaseSlam(level);
    }

    private void releaseSlam(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        LivingEntity primary = slamTarget;
        slamTarget = null;
        AABB area = getBoundingBox().inflate(3.3D, 1.5D, 3.3D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && living != this && !(living instanceof TitanGeoEntity))) {
            double distance = distanceTo(victim);
            if (distance > 3.6D) continue;
            double visibleDamage = victim == primary ? 42.0D : 24.0D;
            victim.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(visibleDamage));
            Vec3 away = victim.position().subtract(position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                victim.push(away.x * 0.95D, 0.34D, away.z * 0.95D);
                victim.hurtMarked = true;
            }
        }
        breakForwardWall(level);
    }

    private void breakForwardWall(ServerLevel level) {
        Vec3 facing = getLookAngle();
        facing = new Vec3(facing.x, 0.0D, facing.z);
        if (facing.lengthSqr() <= 1.0E-6D) return;
        facing = facing.normalize();
        BlockPos center = BlockPos.containing(position().add(facing.scale(1.6D)).add(0.0D, 0.7D, 0.0D));
        int broken = 0;
        for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1))) {
            if (broken >= 8) break;
            BlockPos pos = mutable.immutable();
            var state = level.getBlockState(pos);
            if (BreachService.requiredPower(level, pos, state) <= 3 && level.destroyBlock(pos, false, this)) broken++;
        }
    }
}
