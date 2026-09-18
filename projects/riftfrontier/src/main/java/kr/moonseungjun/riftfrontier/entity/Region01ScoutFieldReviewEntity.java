package kr.moonseungjun.riftfrontier.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Development-only visual review actor for the exact Region 01 Scout Armabee candidate. */
public final class Region01ScoutFieldReviewEntity extends Monster {
    public Region01ScoutFieldReviewEntity(EntityType<? extends Region01ScoutFieldReviewEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // Stationary by design: source flight clips are presentation evidence, not flight-gameplay authorization.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
