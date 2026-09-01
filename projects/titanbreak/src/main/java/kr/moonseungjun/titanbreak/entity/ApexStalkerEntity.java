package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** High-end ambush elite that uses cloak and intercepts the player's actual retreat vector. */
public final class ApexStalkerEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private int revealedTicks;
    private int interceptCooldown;

    public ApexStalkerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 32;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 35;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            revealedTicks = 50;
            setInvisible(false);
            if (target instanceof LivingEntity living && isBehind(living)) {
                living.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(10.0D));
            }
        }
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (revealedTicks > 0) revealedTicks--;
        if (interceptCooldown > 0) interceptCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setInvisible(false);
            return;
        }

        double distance = distanceTo(target);
        setInvisible(revealedTicks <= 0 && distance > 4.2D);
        Vec3 movement = target.getDeltaMovement();
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        Vec3 awayFromMe = target.position().subtract(position());
        awayFromMe = new Vec3(awayFromMe.x, 0.0D, awayFromMe.z);

        boolean retreating = horizontal.lengthSqr() > 0.012D && awayFromMe.lengthSqr() > 0.01D
                && horizontal.normalize().dot(awayFromMe.normalize()) > 0.50D;
        if (retreating && interceptCooldown <= 0 && distance >= 6.0D && distance <= 22.0D) {
            Vec3 route = target.position().add(horizontal.normalize().scale(4.5D));
            getNavigation().moveTo(route.x, route.y, route.z, 1.55D);
            interceptCooldown = 24;
        }
    }

    private boolean isBehind(LivingEntity target) {
        Vec3 toMe = position().subtract(target.position());
        toMe = new Vec3(toMe.x, 0.0D, toMe.z);
        Vec3 look = target.getLookAngle();
        look = new Vec3(look.x, 0.0D, look.z);
        return toMe.lengthSqr() > 1.0E-6D && look.lengthSqr() > 1.0E-6D
                && toMe.normalize().dot(look.normalize()) < -0.45D;
    }
}
