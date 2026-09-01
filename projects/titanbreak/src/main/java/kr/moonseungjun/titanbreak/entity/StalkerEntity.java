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

/** Fast ambusher that cloaks while it stays outside the target's forward view. */
public final class StalkerEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private int revealTicks;

    public StalkerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 11;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 12;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        if (revealTicks > 0) revealTicks--;
        if (target == null || !target.isAlive()) {
            setInvisible(false);
            return;
        }

        double distance = distanceTo(target);
        boolean behind = isBehind(target);
        boolean cloak = revealTicks <= 0 && distance > 3.8D && behind;
        setInvisible(cloak);

        if (behind && distance >= 5.0D && distance <= 15.0D) {
            Vec3 toward = target.position().subtract(position());
            if (toward.lengthSqr() > 1.0E-6D) {
                toward = toward.normalize();
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x * 0.82D + toward.x * 0.045D,
                        motion.y,
                        motion.z * 0.82D + toward.z * 0.045D);
                hurtMarked = true;
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean backstab = target instanceof LivingEntity living && isBehind(living);
        boolean hit = super.doHurtTarget(level, target);
        if (!hit) return false;

        revealTicks = 36;
        setInvisible(false);
        if (backstab && target instanceof LivingEntity living && living.isAlive()) {
            living.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(8.0D));
        }
        return true;
    }

    private boolean isBehind(LivingEntity target) {
        Vec3 look = target.getLookAngle();
        look = new Vec3(look.x, 0.0D, look.z);
        Vec3 toStalker = position().subtract(target.position());
        toStalker = new Vec3(toStalker.x, 0.0D, toStalker.z);
        if (look.lengthSqr() <= 1.0E-6D || toStalker.lengthSqr() <= 1.0E-6D) return false;
        return look.normalize().dot(toStalker.normalize()) < -0.30D;
    }
}
