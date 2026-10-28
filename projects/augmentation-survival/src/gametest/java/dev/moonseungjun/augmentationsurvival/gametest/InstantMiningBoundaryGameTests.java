package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Pins the final server block-break progress boundary used by the dedicated Instant Mining toggle. */
public final class InstantMiningBoundaryGameTests implements CustomTestMethodInvoker {
    private static final String INSTANT_MINING_ID = AugmentationSurvival.MOD_ID + ":instant_mining";

    @GameTest
    public void instantMiningStatePredicateAndFinalProgressStayBound(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
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

        require(!UtilityAugmentService.isInstantMiningEnabled(player),
                "instant mining became active from ownership alone");
        require(!UtilityAugmentService.shouldInstantMine(player, stone, 1.0F),
                "instant mining service bypassed its explicit toggle while off");
        require(!ActiveAbilityRouter.ownedActiveIds(AugmentationPlayerStateService.state(player)).contains(INSTANT_MINING_ID),
                "instant mining is still exposed in the player-facing active loadout");
        require(UtilityAugmentService.toggleInstantMining(player),
                "dedicated instant mining toggle did not turn on");
        require(UtilityAugmentService.shouldInstantMine(player, stone, 1.0F),
                "instant-mining service predicate rejected enabled valid state/tool input");

        float progress = stone.getDestroyProgress(player, level, pos);
        require(progress >= 1.0F,
                "final BlockState#getDestroyProgress boundary did not apply enabled instant mining; progress=" + progress);
        require(UtilityAugmentService.toggleInstantMining(player),
                "dedicated instant mining toggle did not turn off");
        require(!UtilityAugmentService.isInstantMiningEnabled(player),
                "instant mining remained enabled after dedicated toggle off");
        GameTestPlayers.succeed(helper);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
