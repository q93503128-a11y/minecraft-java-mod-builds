package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Western Erden pack predator. It is wildlife, not a reskinned tameable wolf. */
public final class AshHoundEntity extends Wolf {
    public AshHoundEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.20D, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }
}
