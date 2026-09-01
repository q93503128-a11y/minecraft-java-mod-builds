package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Low-health proximity bomber with a visible fuse window before detonation. */
public final class BurstlingEntity extends Zombie implements TitanGeoEntity {
    private static final int FUSE_TICKS = 24;
    private int fuseTicks = -1;

    public BurstlingEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 7;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        if (fuseTicks >= 0) {
            tickFuse(level);
            return;
        }
        if (target == null || !target.isAlive()) {
            setGlowingTag(false);
            return;
        }
        if (distanceToSqr(target) <= 3.5D * 3.5D) {
            fuseTicks = FUSE_TICKS;
            setGlowingTag(true);
            getNavigation().stop();
        }
    }

    private void tickFuse(ServerLevel level) {
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.42D, motion.y, motion.z * 0.42D);
        setGlowingTag(true);
        if (--fuseTicks <= 0) detonate(level);
    }

    private void detonate(ServerLevel level) {
        double radius = 3.4D;
        AABB area = getBoundingBox().inflate(radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area,
                candidate -> candidate.isAlive() && candidate.distanceToSqr(this) <= radius * radius)) {
            double distance = Math.max(0.0D, distanceTo(player));
            double falloff = 1.0D - Math.min(0.55D, distance / (radius * 1.7D));
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(26.0D * falloff));
            Vec3 away = player.position().subtract(position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                player.push(away.x * 0.82D, 0.28D, away.z * 0.82D);
                player.hurtMarked = true;
            }
        }
        discard();
    }
}
