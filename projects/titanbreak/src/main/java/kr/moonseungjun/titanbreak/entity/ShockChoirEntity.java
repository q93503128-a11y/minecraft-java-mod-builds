package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Electrical conductor elite that turns nearby Voltaics into a shared overload network. */
public final class ShockChoirEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private int pulseCooldown = 45;

    public ShockChoirEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 30;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 15;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (--pulseCooldown > 0) return;
        pulseCooldown = 44 + getRandom().nextInt(24);
        pulseNetwork(level);
    }

    private void pulseNetwork(ServerLevel level) {
        AABB networkArea = getBoundingBox().inflate(16.0D);
        List<VoltaicEntity> linked = level.getEntitiesOfClass(VoltaicEntity.class, networkArea,
                voltaic -> voltaic.isAlive());
        int linkCount = Math.min(4, linked.size());
        if (linkCount > 0) {
            swing(InteractionHand.MAIN_HAND);
            for (VoltaicEntity voltaic : linked) {
                voltaic.heal((float) CombatScale.toInternal(4.0D));
                if (getTarget() != null && getTarget().isAlive()) voltaic.setTarget(getTarget());
            }
        }

        double radius = 8.0D + linkCount * 1.5D;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(radius),
                candidate -> candidate.isAlive())) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            TitanPlayerData.State state = data.state(player);
            double drained = AugmentationResourceService.drainPower(player, state, 8.0D + linkCount * 4.0D);
            if (drained > 0.0D) {
                double heat = AugmentationResourceService.normalizedHeatGain(state, 3.0D + linkCount * 1.5D);
                data.setHeat(player, state.heat() + heat);
            }
            if (linkCount >= 3) {
                player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(14.0D));
            }
        }
    }
}
