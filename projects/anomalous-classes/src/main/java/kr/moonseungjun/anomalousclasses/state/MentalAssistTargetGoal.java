package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Selects a target for native combat goals; this goal never adds an attack or damage behavior. */
final class MentalAssistTargetGoal extends Goal {
    private final Mob mob;
    private final MentalDominatorSavedData.ControlledRecord record;
    private LivingEntity proposedTarget;
    private int checkDelay;

    MentalAssistTargetGoal(Mob mob, MentalDominatorSavedData.ControlledRecord record) {
        this.mob = mob;
        this.record = record;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive() || mob.isNoAi() || record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) return false;
        ServerPlayer owner = MentalDominatorService.onlineOwner(mob.level().getServer(), record.ownerId());
        if (owner == null || !owner.isAlive() || owner.level() != mob.level()) return false;
        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive() && MentalDominatorService.isValidCombatTarget(
                owner, current, record.allowPlayerDamage())) return false;
        LivingEntity target = owner.getLastHurtByMob();
        if (!usable(owner, target)) target = owner.getLastHurtMob();
        if (!usable(owner, target)) return false;
        proposedTarget = target;
        return true;
    }

    private boolean usable(ServerPlayer owner, LivingEntity target) {
        return target != null && target.isAlive() && target.level() == mob.level()
                && mob.distanceToSqr(target) <= 32.0 * 32.0
                && MentalDominatorService.isValidCombatTarget(owner, target, record.allowPlayerDamage());
    }

    @Override
    public boolean canContinueToUse() {
        if (proposedTarget == null || !proposedTarget.isAlive() || proposedTarget.level() != mob.level()
                || mob.distanceToSqr(proposedTarget) > 40.0 * 40.0) return false;
        ServerPlayer owner = MentalDominatorService.onlineOwner(mob.level().getServer(), record.ownerId());
        return owner != null && owner.level() == mob.level()
                && MentalDominatorService.isValidCombatTarget(owner, proposedTarget, record.allowPlayerDamage());
    }

    @Override
    public void start() {
        mob.setTarget(proposedTarget);
        checkDelay = 10;
    }

    @Override
    public void tick() {
        if (--checkDelay <= 0) {
            checkDelay = 10;
            if (!canContinueToUse()) mob.setTarget(null);
        }
    }

    @Override
    public void stop() {
        if (mob.getTarget() == proposedTarget) mob.setTarget(null);
        proposedTarget = null;
    }
}
