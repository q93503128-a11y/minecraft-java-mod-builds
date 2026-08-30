package kr.moonseungjun.titanbreak.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

public final class SkitterEntity extends Spider implements TitanGeoEntity {
    private Entity comboTarget;
    private int comboHitsRemaining;
    private int comboDelay;

    public SkitterEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            comboTarget = target;
            comboHitsRemaining = 2;
            comboDelay = 10;
        }
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (comboHitsRemaining <= 0 || comboTarget == null) return;
        if (--comboDelay > 0) return;

        if (comboTarget.isAlive() && distanceToSqr(comboTarget) <= 10.0D) {
            swing(InteractionHand.MAIN_HAND);
            super.doHurtTarget(level, comboTarget);
            comboHitsRemaining--;
            comboDelay = 10;
        } else {
            comboHitsRemaining = 0;
            comboTarget = null;
        }

        if (comboHitsRemaining <= 0) comboTarget = null;
    }
}
