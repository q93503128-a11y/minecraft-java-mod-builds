package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Pins the final server block-break progress boundary used by instant mining. */
@SuppressWarnings({"removal", "deprecation"})
public final class InstantMiningBoundaryGameTests implements CustomTestMethodInvoker {
    private static final String INSTANT_MINING_ID = AugmentationSurvival.MOD_ID + ":instant_mining";

    @GameTest
    public void instantMiningStatePredicateAndFinalProgressStayBound(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());

        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(INSTANT_MINING_ID, 1));
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition().offset(2, 0, 0);
        var stone = Blocks.STONE.defaultBlockState();
        level.setBlock(pos, stone, Block.UPDATE_ALL);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(Items.DIAMOND_PICKAXE));

        require(AugmentationPlayerStateService.state(player).level(INSTANT_MINING_ID) == 1,
                "instant-mining attachment state was lost before break-progress calculation");
        require(player.hasCorrectToolForDrops(stone),
                "instant-mining precondition rejected the diamond pickaxe");
        require(UtilityAugmentService.shouldInstantMine(player, stone, 1.0F),
                "instant-mining service predicate rejected valid state/tool input");

        float progress = stone.getDestroyProgress(player, level, pos);
        require(progress >= 1.0F,
                "final BlockState#getDestroyProgress boundary did not apply instant mining; progress=" + progress);
        player.discard();
        helper.succeed();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
