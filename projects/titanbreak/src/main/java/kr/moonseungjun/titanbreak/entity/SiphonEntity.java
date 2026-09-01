package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Sustain-support predator: drains the victim and converts the stolen output into nearby recovery. */
public final class SiphonEntity extends Zombie implements TitanGeoEntity {
    public SiphonEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (!hit) return false;

        double stolenPower = 0.0D;
        if (target instanceof ServerPlayer player) {
            TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
            double before = AugmentationResourceService.currentPower(player, state);
            AugmentationResourceService.drainPower(player, state, 10.0D);
            double after = AugmentationResourceService.currentPower(player, state);
            stolenPower = Math.max(0.0D, before - after);
        }

        heal((float) CombatScale.toInternal(7.0D + Math.min(4.0D, stolenPower * 0.20D)));
        healNearbyAllies(level, 4.0D + Math.min(3.0D, stolenPower * 0.12D));
        return true;
    }

    private void healNearbyAllies(ServerLevel level, double visibleAmount) {
        AABB area = getBoundingBox().inflate(6.0D);
        int healed = 0;
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living != this && living.isAlive() && living instanceof TitanGeoEntity
                        && living.getHealth() < living.getMaxHealth())) {
            ally.heal((float) CombatScale.toInternal(visibleAmount));
            if (++healed >= 3) break;
        }
    }
}
