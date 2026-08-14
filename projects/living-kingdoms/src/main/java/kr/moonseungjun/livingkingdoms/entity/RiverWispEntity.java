package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Ambient Silver River spirit. Players cannot turn it into an Allay-style item courier. */
public final class RiverWispEntity extends Allay {
    public RiverWispEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}
