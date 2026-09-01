package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** Regeneration elite with three independently cycling cores. Two must be disabled in the same window to halt repair. */
public final class RevenantEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private final int[] coreDisabledTicks = new int[3];
    private float observedHealth = Float.NaN;
    private int nextCore;

    public RevenantEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 30;
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
        for (int i = 0; i < coreDisabledTicks.length; i++) {
            if (coreDisabledTicks[i] > 0) coreDisabledTicks[i]--;
        }

        float health = getHealth();
        if (Float.isNaN(observedHealth)) {
            observedHealth = health;
            return;
        }

        float burstThreshold = (float) CombatScale.toInternal(18.0D);
        if (observedHealth - health >= burstThreshold) {
            disableNextCore();
        }

        int activeDisabled = disabledCoreCount();
        if (activeDisabled < 2 && health > 0.0F && health < getMaxHealth() && tickCount % 8 == 0) {
            // One surviving regeneration core is enough to aggressively rebuild the body.
            heal((float) CombatScale.toInternal(activeDisabled == 0 ? 10.0D : 6.0D));
            health = getHealth();
        }
        observedHealth = health;
    }

    public int disabledCoreCount() {
        int count = 0;
        for (int ticks : coreDisabledTicks) if (ticks > 0) count++;
        return count;
    }

    private void disableNextCore() {
        for (int offset = 0; offset < coreDisabledTicks.length; offset++) {
            int index = (nextCore + offset) % coreDisabledTicks.length;
            if (coreDisabledTicks[index] <= 0) {
                coreDisabledTicks[index] = 100;
                nextCore = (index + 1) % coreDisabledTicks.length;
                return;
            }
        }
        // All three are already down; refresh the oldest slot so the simultaneous-break window remains meaningful.
        coreDisabledTicks[nextCore] = 100;
        nextCore = (nextCore + 1) % coreDisabledTicks.length;
    }
}
