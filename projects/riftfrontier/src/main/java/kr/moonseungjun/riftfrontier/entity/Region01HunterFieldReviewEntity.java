package kr.moonseungjun.riftfrontier.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Development-only visual review actor for the Region 01 Hunter Alien candidate. */
public final class Region01HunterFieldReviewEntity extends Monster {
    public Region01HunterFieldReviewEntity(EntityType<? extends Region01HunterFieldReviewEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // Stationary by design: the renderer cycles exact source clips for visual review.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
