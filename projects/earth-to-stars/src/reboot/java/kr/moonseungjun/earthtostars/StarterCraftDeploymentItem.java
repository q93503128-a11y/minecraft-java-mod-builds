package kr.moonseungjun.earthtostars;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class StarterCraftDeploymentItem extends Item {
    public StarterCraftDeploymentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (context.getClickedFace().getAxis().isHorizontal()) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.earth_to_stars.deploy_top_surface"), true);
            }
            return InteractionResult.CONSUME;
        }

        BlockPos floorCenter = context.getClickedPos().relative(context.getClickedFace());
        try {
            StarterCraftDeploymentService.deploy(level, floorCenter);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.earth_to_stars.deploy_success"), true);
            }
            return InteractionResult.CONSUME;
        } catch (RuntimeException error) {
            EarthToStars.LOGGER.warn("Starter craft deployment rejected at {}", floorCenter, error);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("message.earth_to_stars.deploy_failed", error.getMessage()),
                        true
                );
            }
            return InteractionResult.FAIL;
        }
    }
}
