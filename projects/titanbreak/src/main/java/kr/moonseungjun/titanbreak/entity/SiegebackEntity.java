package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.BreachService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Heavy siege elite: frontal armor and a telegraphed dorsal cannon that breaches ordinary structures. */
public final class SiegebackEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private static final int CANNON_COOLDOWN = 110;
    private int cannonCooldown = 55;
    private int cannonCharge;
    private Vec3 cannonTarget;

    public SiegebackEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 36;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null) {
            Vec3 toAttacker = attacker.position().subtract(position());
            if (toAttacker.horizontalDistanceSqr() > 1.0E-6D) {
                Vec3 direction = toAttacker.normalize();
                Vec3 facing = getLookAngle();
                double dot = facing.x * direction.x + facing.z * direction.z;
                if (dot > 0.20D) amount *= 0.16F;
                else if (dot > -0.25D) amount *= 0.62F;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (cannonCharge > 0) {
            cannonCharge--;
            setDeltaMovement(getDeltaMovement().scale(0.45D));
            if (cannonCharge == 0 && cannonTarget != null) {
                fireCannon(level, cannonTarget);
                cannonTarget = null;
                cannonCooldown = CANNON_COOLDOWN;
            }
            return;
        }

        if (cannonCooldown > 0) {
            cannonCooldown--;
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double distance = distanceTo(target);
        if (distance < 7.0D || distance > 34.0D || !hasLineOfSight(target)) return;

        cannonTarget = target.position().add(target.getDeltaMovement().scale(14.0D));
        cannonCharge = 28;
        swing(InteractionHand.MAIN_HAND);
    }

    private void fireCannon(ServerLevel level, Vec3 impact) {
        AABB blast = new AABB(impact, impact).inflate(3.4D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blast,
                living -> living.isAlive() && living != this && !(living instanceof TitanGeoEntity))) {
            double distance = Math.sqrt(victim.distanceToSqr(impact));
            double scale = Math.max(0.25D, 1.0D - distance / 4.2D);
            victim.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(46.0D * scale));
            Vec3 push = victim.position().subtract(impact);
            if (push.lengthSqr() > 1.0E-6D) {
                push = push.normalize().scale(0.75D * scale);
                victim.push(push.x, 0.22D + 0.18D * scale, push.z);
            }
        }
        breachImpact(level, BlockPos.containing(impact));
    }

    private void breachImpact(ServerLevel level, BlockPos center) {
        int broken = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            if (broken >= 18) break;
            BlockPos pos = cursor.immutable();
            if (pos.distSqr(center) > 7.0D) continue;
            var state = level.getBlockState(pos);
            if (BreachService.requiredPower(level, pos, state) > 4) continue;
            if (level.destroyBlock(pos, false, this)) broken++;
        }
    }
}
