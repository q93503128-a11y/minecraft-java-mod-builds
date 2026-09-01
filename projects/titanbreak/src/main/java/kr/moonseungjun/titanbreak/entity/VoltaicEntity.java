package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class VoltaicEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    public VoltaicEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 5;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (!hit) return false;

        if (target instanceof ServerPlayer player) {
            drainPower(level, player, 12.0D);
        }

        int chained = 0;
        for (ServerPlayer other : level.players()) {
            if (other == target || !other.isAlive() || distanceToSqr(other) > 6.5D * 6.5D) continue;
            other.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(10.0D));
            drainPower(level, other, 6.0D);
            if (++chained >= 2) break;
        }
        return true;
    }

    private static void drainPower(ServerLevel level, ServerPlayer player, double amount) {
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        AugmentationResourceService.drainPower(player, state, amount);
    }
}
