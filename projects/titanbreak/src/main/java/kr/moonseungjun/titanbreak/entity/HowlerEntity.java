package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class HowlerEntity extends Zombie implements TitanGeoEntity {
    public HowlerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new HowlGoal());
    }

    private final class HowlGoal extends Goal {
        private int timer;

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            if (++timer < 80) return;
            timer = 0;
            swing(InteractionHand.MAIN_HAND);
            if (!(level() instanceof ServerLevel serverLevel)) return;
            for (var player : serverLevel.players()) {
                if (player.isAlive() && distanceToSqr(player) <= 18.0D * 18.0D) {
                    TitanPlayerData data = TitanPlayerData.get(serverLevel.getServer());
                    data.setSanity(player, data.state(player).sanity() - 4.0D);
                }
            }
        }
    }
}
