package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class NullEyeEntity extends Zombie implements TemporalRated {
    public NullEyeEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new JammingGoal());
    }

    @Override
    public int temporalRating() {
        return 20;
    }

    private final class JammingGoal extends Goal {
        private int timer = 80;

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            if (++timer < 160) return;
            timer = 0;
            if (!(level() instanceof ServerLevel serverLevel)) return;
            for (var player : serverLevel.players()) {
                if (player.isAlive() && distanceToSqr(player) <= 18.0D * 18.0D) {
                    AnalysisJammingService.apply(player, 240);
                }
            }
        }
    }
}
