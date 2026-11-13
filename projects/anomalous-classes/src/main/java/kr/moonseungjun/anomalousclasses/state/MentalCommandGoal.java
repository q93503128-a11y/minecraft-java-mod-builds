package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

/** Adds command behavior without replacing or serializing over the creature's native goal set. */
final class MentalCommandGoal extends Goal {
    private final Mob mob;
    private final MentalDominatorSavedData.ControlledRecord record;
    private final PathNavigation navigation;
    private int repathDelay;

    MentalCommandGoal(Mob mob, MentalDominatorSavedData.ControlledRecord record) {
        this.mob = mob;
        this.record = record;
        this.navigation = mob.getNavigation();
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive() || mob.isNoAi() || record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE
                || mob.getTarget() != null) return false;
        ServerPlayer owner = MentalDominatorService.onlineOwner(mob.level().getServer(), record.ownerId());
        if (owner == null || !owner.isAlive()) return false;
        // A remote owner is not a reason to take over the mob's MOVE selector.
        // The entity keeps its native local behavior without cross-dimension pathing.
        return MentalDominatorRules.shouldTakeFollowMovement(record.command(), owner.level() == mob.level(),
                mob.distanceToSqr(owner));
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repathDelay = 0;
    }

    @Override
    public void tick() {
        ServerPlayer owner = MentalDominatorService.onlineOwner(mob.level().getServer(), record.ownerId());
        if (owner == null || record.command() == MentalDominatorSavedData.CommandMode.STAY
                || owner.level() != mob.level()) {
            navigation.stop();
            return;
        }
        if (--repathDelay <= 0) {
            repathDelay = 10;
            navigation.moveTo(owner, MentalDominatorConfig.value(MentalDominatorConfig.VALUES.followSpeed));
        }
    }

    @Override
    public void stop() {
        navigation.stop();
    }
}
