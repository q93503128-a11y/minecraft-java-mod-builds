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
import net.minecraft.world.phys.Vec3;

public final class LaunchCraftKitItem extends Item {
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
        if (!hasDeploymentClearance(level, base)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.blocked"));
            return InteractionResult.FAIL;
        }

        Vec3 spawn = Vec3.atBottomCenterOf(base).add(0.0D, 0.25D, 0.0D);
        ShipRuntimeManager.LaunchDeploymentResult result = ShipRuntimeManager.deployLaunchCraft(
                player,
                level,
                spawn,
                level.getGameTime()
        );

        return switch (result) {
            case DEPLOYED -> {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.deployed"));
                yield InteractionResult.SUCCESS_SERVER;
            }
            case ALREADY_OWNS_CRAFT -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.existing"));
                yield InteractionResult.FAIL;
            }
            case CONTROL_UNAVAILABLE -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.control_unavailable"));
                yield InteractionResult.FAIL;
            }
            case DEPLOYMENT_FAILED -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.failed"));
                yield InteractionResult.FAIL;
            }
        };
    }

    private static boolean hasDeploymentClearance(ServerLevel level, BlockPos base) {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!level.isEmptyBlock(base.offset(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
