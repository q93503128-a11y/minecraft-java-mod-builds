package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Temporal elite that repeatedly steps through short distances and phases incoming projectiles. */
public final class PhaseLurkerEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private int phaseCooldown = 24;
    private int projectilePhaseCooldown;

    public PhaseLurkerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 32;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 55;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Projectile && projectilePhaseCooldown <= 0) {
            projectilePhaseCooldown = 24;
            shortPhase(level, getTarget(), true);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (projectilePhaseCooldown > 0) projectilePhaseCooldown--;
        if (phaseCooldown > 0) {
            phaseCooldown--;
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double distance = distanceTo(target);
        if (distance < 3.0D || distance > 18.0D) return;

        shortPhase(level, target, false);
        phaseCooldown = 44 + getRandom().nextInt(28);
    }

    private void shortPhase(ServerLevel level, LivingEntity target, boolean evasive) {
        Vec3 baseDirection;
        if (target != null) {
            baseDirection = target.position().subtract(position());
            if (baseDirection.horizontalDistanceSqr() > 1.0E-6D) baseDirection = baseDirection.normalize();
            else baseDirection = getLookAngle();
        } else baseDirection = getLookAngle();

        Vec3 side = new Vec3(-baseDirection.z, 0.0D, baseDirection.x);
        double sideSign = getRandom().nextBoolean() ? 1.0D : -1.0D;
        double forward = evasive ? -2.6D : 2.4D + getRandom().nextDouble() * 2.2D;
        double lateral = sideSign * (2.2D + getRandom().nextDouble() * 2.4D);
        Vec3 delta = baseDirection.scale(forward).add(side.scale(lateral));

        for (int attempt = 0; attempt < 5; attempt++) {
            Vec3 candidate = position().add(delta.scale(1.0D - attempt * 0.14D));
            BlockPos feet = BlockPos.containing(candidate);
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
            setPos(candidate.x, candidate.y, candidate.z);
            getNavigation().stop();
            swing(InteractionHand.MAIN_HAND);
            return;
        }
    }
}
