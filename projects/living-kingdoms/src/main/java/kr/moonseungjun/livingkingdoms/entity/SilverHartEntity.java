package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Northern Erden herd animal: wary of travellers and happiest close to its herd. */
public final class SilverHartEntity extends Goat {
    public SilverHartEntity(EntityType<? extends Goat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new AvoidEntityGoal<>(
                this, Player.class, 18.0F, 1.18D, 1.42D));
    }
    /** These are player-local ecology instances, not permanent settlement actors. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

}
