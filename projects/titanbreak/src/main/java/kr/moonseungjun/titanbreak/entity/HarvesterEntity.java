package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Regeneration elite that harvests combat debris as biomass and converts it into repair and small reinforcements. */
public final class HarvesterEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private int biomass;
    private int summonCooldown;

    public HarvesterEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 35;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 10;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (summonCooldown > 0) summonCooldown--;
        if (tickCount % 20 != 0) return;

        AABB area = getBoundingBox().inflate(8.0D);
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && !item.getItem().isEmpty());
        int consumed = 0;
        for (ItemEntity item : debris) {
            if (consumed >= 3) break;
            int value = Math.max(1, Math.min(4, item.getItem().getCount()));
            biomass += value;
            item.discard();
            consumed++;
        }

        if (consumed > 0 && getHealth() < getMaxHealth()) {
            heal((float) CombatScale.toInternal(Math.min(24.0D, 6.0D + biomass * 1.2D)));
        }

        if (biomass >= 6 && summonCooldown <= 0) {
            summonBurstling(level);
            biomass -= 6;
            summonCooldown = 90;
        }
    }

    private void summonBurstling(ServerLevel level) {
        var created = ModEntities.BURSTLING.get().create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof BurstlingEntity burstling)) return;
        double angle = getRandom().nextDouble() * Math.PI * 2.0D;
        double range = 1.8D + getRandom().nextDouble() * 1.6D;
        burstling.setPos(getX() + Math.cos(angle) * range, getY(), getZ() + Math.sin(angle) * range);
        if (!level.noCollision(burstling)) return;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) burstling.setTarget(target);
        level.addFreshEntity(burstling);
    }
}
