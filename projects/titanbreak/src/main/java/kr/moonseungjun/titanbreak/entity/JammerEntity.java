package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class JammerEntity extends Zombie implements TitanGeoEntity {
    private static final double JAM_RADIUS = 12.0D;

    public JammerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 9;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 10 != 0) return;
        double radiusSqr = JAM_RADIUS * JAM_RADIUS;
        for (var player : level.players()) {
            if (player.isAlive() && distanceToSqr(player) <= radiusSqr) {
                AnalysisJammingService.apply(player, 35);
            }
        }
    }
}
