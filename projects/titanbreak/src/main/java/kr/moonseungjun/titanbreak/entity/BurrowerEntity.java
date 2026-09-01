package kr.moonseungjun.titanbreak.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BurrowerEntity extends Zombie implements TitanGeoEntity {
    private int burrowCooldown = 75;
    private int burrowTicks;
    private boolean burrowed;

    public BurrowerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 11;
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
            if (burrowed) emerge(null);
            return;
        }

        if (burrowed) {
            tickBurrow(target);
            return;
        }

        if (--burrowCooldown <= 0) {
            double distance = distanceTo(target);
            if (distance >= 6.0D && distance <= 18.0D) beginBurrow();
            else burrowCooldown = 20;
        }
    }

    private void beginBurrow() {
        burrowed = true;
        burrowTicks = 24;
        setInvisible(true);
        noPhysics = true;
        setNoGravity(true);
        getNavigation().stop();
        setDeltaMovement(0.0D, -0.32D, 0.0D);
    }

    private void tickBurrow(LivingEntity target) {
        burrowTicks--;
        Vec3 desired = target.position().subtract(position());
        Vec3 horizontal = new Vec3(desired.x, 0.0D, desired.z);
        if (horizontal.lengthSqr() > 1.0E-6D) horizontal = horizontal.normalize().scale(0.52D);
        double desiredY = target.getY() - 1.4D;
        double vertical = Math.max(-0.24D, Math.min(0.24D, (desiredY - getY()) * 0.20D));
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        hurtMarked = true;

        if (burrowTicks <= 0 || horizontalDistanceTo(target) <= 1.8D) emerge(target);
    }

    private void emerge(LivingEntity target) {
        burrowed = false;
        noPhysics = false;
        setNoGravity(false);
        setInvisible(false);
        burrowCooldown = 85 + getRandom().nextInt(45);
        if (target == null) return;

        setPos(getX(), target.getY() + 0.05D, getZ());
        setDeltaMovement(getDeltaMovement().x, 0.46D, getDeltaMovement().z);
        swing(InteractionHand.MAIN_HAND);
        if (level() instanceof ServerLevel level && target.isAlive() && distanceToSqr(target) <= 3.3D * 3.3D) {
            super.doHurtTarget(level, target);
        }
        hurtMarked = true;
    }

    private double horizontalDistanceTo(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
