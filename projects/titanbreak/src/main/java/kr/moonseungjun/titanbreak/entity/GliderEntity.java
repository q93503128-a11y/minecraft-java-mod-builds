package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GliderEntity extends Zombie implements TitanGeoEntity {
    private int diveCooldown = 45;
    private int diveTicks;
    private int shotCooldown = 30;
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
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.84D).add(0.0D, 0.015D, 0.0D));
            return;
        }

        if (diveTicks > 0) {
            tickDive(level, target);
        } else {
            tickOrbit(target);
            if (--diveCooldown <= 0 && distanceTo(target) > 5.0F && distanceTo(target) < 18.0F) {
                diveCooldown = 65 + getRandom().nextInt(45);
                diveTicks = 22;
                diveHit = false;
            }
        }

        if (--shotCooldown <= 0 && distanceTo(target) >= 7.0F && distanceTo(target) <= 24.0F && hasLineOfSight(target)) {
            shotCooldown = 46 + getRandom().nextInt(30);
            fireDart(level, target);
        }

        Vec3 motion = getDeltaMovement();
        if (motion.length() > 1.05D) setDeltaMovement(motion.normalize().scale(1.05D));
        hurtMarked = true;
    }

    private void tickOrbit(LivingEntity target) {
        double angle = (tickCount * 0.055D) + (getId() * 0.37D);
        double radius = 5.5D;
        Vec3 desired = target.position().add(Math.cos(angle) * radius, 4.5D, Math.sin(angle) * radius);
        Vec3 correction = desired.subtract(position());
        if (correction.lengthSqr() > 0.01D) {
            correction = correction.normalize().scale(0.085D);
            setDeltaMovement(getDeltaMovement().scale(0.88D).add(correction));
        }
    }

    private void tickDive(ServerLevel level, LivingEntity target) {
        diveTicks--;
        Vec3 aim = target.getEyePosition().subtract(position());
        if (aim.lengthSqr() > 1.0E-6D) {
            setDeltaMovement(getDeltaMovement().scale(0.72D).add(aim.normalize().scale(0.18D)));
        }
        if (!diveHit && distanceToSqr(target) <= 2.8D * 2.8D) {
            diveHit = doHurtTarget(level, target);
        }
        if (diveTicks == 0) setDeltaMovement(getDeltaMovement().add(0.0D, 0.42D, 0.0D));
    }

    private void fireDart(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        double sx = getX();
        double sy = getEyeY() - 0.10D;
        double sz = getZ();
        Arrow arrow = new Arrow(level, sx, sy, sz, Items.ARROW.getDefaultInstance(), null);
        arrow.setOwner(this);
        arrow.setBaseDamage(CombatScale.toInternal(16.0D));
        Vec3 aim = target.getEyePosition().add(target.getDeltaMovement().scale(2.8D))
                .subtract(sx, sy, sz);
        arrow.shoot(aim.x, aim.y, aim.z, 1.65F, 3.5F);
        level.addFreshEntity(arrow);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return super.doHurtTarget(level, target);
    }
}
