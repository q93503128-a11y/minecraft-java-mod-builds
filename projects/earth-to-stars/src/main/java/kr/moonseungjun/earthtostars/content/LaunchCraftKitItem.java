package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipRuntimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class LaunchCraftKitItem extends Item {
    private static final int DEPLOY_RADIUS = 2;
    private static final int DEPLOY_HEIGHT = 3;

    public LaunchCraftKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.FAIL;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.earth_only"));
            return InteractionResult.FAIL;
        }

        BlockPos base = context.getClickedPos().relative(context.getClickedFace());
        if (!prepareDeploymentSite(level, base)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.blocked"), true);
            return InteractionResult.FAIL;
        }

        Vec3 spawn = Vec3.atBottomCenterOf(base).add(0.0D, 0.05D, 0.0D);
        ShipRuntimeManager.LaunchDeploymentResult result = ShipRuntimeManager.deployLaunchCraft(
                player, level, spawn, level.getGameTime());

        return switch (result) {
            case DEPLOYED -> {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.deployed"), true);
                yield InteractionResult.SUCCESS_SERVER;
            }
            case ALREADY_OWNS_CRAFT -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.existing"), true);
                yield InteractionResult.FAIL;
            }
            case CONTROL_UNAVAILABLE -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.control_unavailable"), true);
                yield InteractionResult.FAIL;
            }
            case DEPLOYMENT_FAILED -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.failed"), true);
                yield InteractionResult.FAIL;
            }
        };
    }

    private static boolean prepareDeploymentSite(ServerLevel level, BlockPos base) {
        for (int y = 0; y < DEPLOY_HEIGHT; y++) {
            for (int x = -DEPLOY_RADIUS; x <= DEPLOY_RADIUS; x++) {
                for (int z = -DEPLOY_RADIUS; z <= DEPLOY_RADIUS; z++) {
                    BlockState state = level.getBlockState(base.offset(x, y, z));
                    if (!state.isAir() && (!state.canBeReplaced() || !state.getFluidState().isEmpty())) {
                        return false;
                    }
                }
            }
        }
        for (int y = 0; y < DEPLOY_HEIGHT; y++) {
            for (int x = -DEPLOY_RADIUS; x <= DEPLOY_RADIUS; x++) {
                for (int z = -DEPLOY_RADIUS; z <= DEPLOY_RADIUS; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        return true;
    }
}
