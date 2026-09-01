package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class RegrowerEntity extends Zombie implements TitanGeoEntity {
    private float observedHealth = Float.NaN;
    private int regenDelay = 80;

    public RegrowerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 11;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        float health = getHealth();
        if (Float.isNaN(observedHealth)) {
            observedHealth = health;
            return;
        }
        if (health + 0.01F < observedHealth) regenDelay = 80;
        else if (regenDelay > 0) regenDelay--;

        if (regenDelay <= 0 && health > 0.0F && health < getMaxHealth() && tickCount % 10 == 0) {
            heal((float) CombatScale.toInternal(7.0D));
            health = getHealth();
        }
        observedHealth = health;
    }
}
