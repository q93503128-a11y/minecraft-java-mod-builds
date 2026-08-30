package kr.moonseungjun.titanbreak.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RipperEntity extends Zombie implements TitanGeoEntity {
    private Entity comboTarget;
    private int comboHitsRemaining;
    private int comboDelay;
    private int flankCooldown;

    public RipperEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 7;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            comboTarget = target;
            comboHitsRemaining = 1;
            comboDelay = 10;
        }
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        tickCombo(level);
        tickFlank();
    }

    private void tickCombo(ServerLevel level) {
        if (comboHitsRemaining <= 0 || comboTarget == null) return;
        if (--comboDelay > 0) return;

        if (comboTarget.isAlive() && distanceToSqr(comboTarget) <= 9.0D) {
            swing(InteractionHand.MAIN_HAND);
            super.doHurtTarget(level, comboTarget);
        }
        comboHitsRemaining = 0;
        comboTarget = null;
    }

    private void tickFlank() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (flankCooldown-- > 0) return;
        flankCooldown = 28 + getRandom().nextInt(18);

        double distance = distanceTo(target);
        if (distance < 4.0D || distance > 13.0D) return;

        Vec3 toward = target.position().subtract(position());
        if (toward.lengthSqr() < 0.01D) return;
        toward = toward.normalize();
        double sideSign = getRandom().nextBoolean() ? 1.0D : -1.0D;
        Vec3 side = new Vec3(-toward.z, 0.0D, toward.x).scale(2.4D * sideSign);
        Vec3 flankPoint = target.position().add(side).subtract(toward.scale(1.4D));
        getNavigation().moveTo(flankPoint.x, flankPoint.y, flankPoint.z, 1.28D);
    }
}
