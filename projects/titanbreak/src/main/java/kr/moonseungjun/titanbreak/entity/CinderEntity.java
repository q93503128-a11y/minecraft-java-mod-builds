package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CinderEntity extends Zombie implements TitanGeoEntity {
    private int burstCooldown = 55;

    public CinderEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof ServerPlayer player) addHeat(player, 7.0D);
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--burstCooldown > 0 || distanceToSqr(target) > 7.0D * 7.0D) return;
        burstCooldown = 70 + getRandom().nextInt(35);
        thermalBurst(level);
    }

    private void thermalBurst(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        AABB area = getBoundingBox().inflate(4.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area,
                candidate -> candidate.isAlive() && candidate.distanceToSqr(this) <= 16.0D)) {
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(22.0D));
            addHeat(player, 14.0D);
            Vec3 away = player.position().subtract(position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                player.push(away.x * 0.34D, 0.12D, away.z * 0.34D);
                player.hurtMarked = true;
            }
        }
    }

    private static void addHeat(ServerPlayer player, double rawHeat) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        double normalized = AugmentationResourceService.normalizedHeatGain(state, rawHeat);
        data.setHeat(player, state.heat() + normalized);
    }
}
