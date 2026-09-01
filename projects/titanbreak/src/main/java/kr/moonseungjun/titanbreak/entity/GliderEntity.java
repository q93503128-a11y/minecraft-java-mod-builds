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

public final class GliderEntity extends Zombie implements TitanGeoEntity {
    private int harassCooldown = 25;
    private int diveCooldown = 70;
    private int diveTicks;
    private boolean diveHit;

    public GliderEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 9;
        setNoGravity(true);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        setNoGravity(true);
        fallDistance = 0.0F;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.82D));
            return;
        }
        getNavigation().stop();

        double distance = distanceTo(target);
        if (diveTicks > 0) {
            Vec3 toTarget = target.getEyePosition().subtract(position());
            if (toTarget.lengthSqr() > 0.01D) {
                Vec3 thrust = toTarget.normalize().scale(0.30D);
                setDeltaMovement(getDeltaMovement().scale(0.56D).add(thrust));
            }
            if (!diveHit && distance <= 2.35D) {
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(level, target);
                diveHit = true;
            }
            if (--diveTicks <= 0) {
                diveCooldown = 75 + getRandom().nextInt(35);
            }
        } else {
            if (diveCooldown > 0) diveCooldown--;

            double orbit = (tickCount * 0.075D) + (getId() * 0.37D);
            Vec3 hoverPoint = target.position().add(
                    Math.cos(orbit) * 5.5D,
                    4.5D + Math.sin(orbit * 0.55D) * 1.2D,
                    Math.sin(orbit) * 5.5D);
            Vec3 correction = hoverPoint.subtract(position());
            if (correction.lengthSqr() > 0.01D) {
                double strength = Math.min(0.16D, 0.045D + correction.length() * 0.008D);
                setDeltaMovement(getDeltaMovement().scale(0.78D).add(correction.normalize().scale(strength)));
            }

            if (diveCooldown <= 0 && distance >= 4.0D && distance <= 13.0D) {
                diveTicks = 22;
                diveHit = false;
            }
        }

        if (harassCooldown > 0) harassCooldown--;
        if (harassCooldown <= 0 && distance >= 7.0D && distance <= 24.0D && hasLineOfSight(target)) {
            swing(InteractionHand.MAIN_HAND);
            fireHarassBolt(level, target);
            harassCooldown = 52 + getRandom().nextInt(25);
        }

        setDeltaMovement(clampVelocity(getDeltaMovement(), 1.05D));
        hurtMarked = true;
    }

    private void fireHarassBolt(ServerLevel level, LivingEntity target) {
        Arrow arrow = EntityType.ARROW.create(level, EntitySpawnReason.EVENT);
        if (arrow == null) return;
        arrow.setOwner(this);
        arrow.setPos(getX(), getEyeY() - 0.05D, getZ());

        Vec3 aim = target.getEyePosition().subtract(arrow.position())
                .add(target.getDeltaMovement().scale(5.0D));
        arrow.shoot(aim.x, aim.y, aim.z, 1.65F, 1.5F);
        arrow.setBaseDamage(CombatScale.toInternal(16.0D));
        level.addFreshEntity(arrow);
    }

    private static Vec3 clampVelocity(Vec3 velocity, double max) {
        double length = velocity.length();
        return length > max && length > 1.0E-6D ? velocity.scale(max / length) : velocity;
    }
}
