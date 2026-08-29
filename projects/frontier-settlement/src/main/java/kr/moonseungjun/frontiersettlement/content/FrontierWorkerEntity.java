package kr.moonseungjun.frontiersettlement.content;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Frontier civilian work body.
 *
 * This is deliberately NOT a Villager. Server behaviour is owned only by Frontier settlement
 * services issuing physical navigation/work orders. The client renderer reuses the villager model
 * and base texture as presentation only; there are no professions, trades, POIs, breeding rules,
 * gossip, village Brain activities, beds/jobsites, or vanilla villager schedules here.
 */
public final class FrontierWorkerEntity extends PathfinderMob {
    public FrontierWorkerEntity(EntityType<? extends FrontierWorkerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty. Frontier services are the single movement/work authority.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
