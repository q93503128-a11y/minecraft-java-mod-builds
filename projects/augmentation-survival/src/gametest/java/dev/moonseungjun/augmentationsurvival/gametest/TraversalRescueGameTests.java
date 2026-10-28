package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformBlocks;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/** Physical traversal regressions for Temporary Platform landing and Water Run entry rules. */
public final class TraversalRescueGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 100)
    public void temporaryPlatformLandingCancelsActualFallDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos center = new BlockPos(4, 4, 4);
        BlockPos absoluteCenter = helper.absolutePos(center);

        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }

        player.setPos(
                absoluteCenter.getX() + 0.5D,
                absoluteCenter.getY(),
                absoluteCenter.getZ() + 0.5D
        );
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("temporary_platform"), 1)
        );
        require(TemporaryPlatformService.execute(player, 1.0D),
                "temporary platform failed to place beneath the player");
        require(helper.getBlockState(new BlockPos(4, 3, 4)).is(TemporaryPlatformBlocks.TEMPORARY_PLATFORM),
                "temporary platform center block was not placed");

        player.setHealth(player.getMaxHealth());
        float before = player.getHealth();
        player.resetFallDistance();
        player.setPos(
                absoluteCenter.getX() + 0.5D,
                absoluteCenter.getY() + 6.0D,
                absoluteCenter.getZ() + 0.5D
        );
        player.setDeltaMovement(0.0D, -0.20D, 0.0D);

        GameTestPlayers.runAfterDelay(helper, 60, () -> {
            require(player.onGround(), "player did not actually land on the temporary platform");
            require(close(player.getHealth(), before, 1.0E-4F),
                    "temporary platform landing leaked fall damage to player health");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 100)
    public void ordinaryBlockLandingStillDealsFallDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos floor = new BlockPos(4, 3, 4);
        helper.setBlock(floor, Blocks.STONE);
        BlockPos absoluteFloor = helper.absolutePos(floor);

        player.setHealth(player.getMaxHealth());
        float before = player.getHealth();
        player.resetFallDistance();
        player.setPos(
                absoluteFloor.getX() + 0.5D,
                absoluteFloor.getY() + 7.0D,
                absoluteFloor.getZ() + 0.5D
        );
        player.setDeltaMovement(0.0D, -0.20D, 0.0D);

        GameTestPlayers.runAfterDelay(helper, 60, () -> {
            require(player.onGround(), "control player did not land on the ordinary block");
            require(player.getHealth() < before,
                    "ordinary block landing incorrectly received temporary-platform fall immunity");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 40)
    public void waterRunBridgesGroundSprintEdgeButRejectsStoppedSlowCrouchedAndLava(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("water_run"), 1)
        );
        player.setSprinting(true);
        player.setDeltaMovement(0.10D, 0.0D, 0.0D);

        GameTestPlayers.runAfterDelay(helper, 1, () -> {
            require(player.onGround(), "water-run entry setup was not grounded");

            // Simulate the vanilla sprint flag dropping exactly as the player reaches the fluid edge.
            player.setSprinting(false);
            player.setDeltaMovement(0.20D, 0.0D, 0.0D);
            require(player.canStandOnFluid(Fluids.WATER.defaultFluidState()),
                    "recent ground sprint did not bridge the water-entry sprint edge");
            require(!player.canStandOnFluid(Fluids.LAVA.defaultFluidState()),
                    "Water Run incorrectly allowed standing on lava");

            GameTestPlayers.runAfterDelay(helper, 3, () -> {
                player.setSprinting(false);
                player.setDeltaMovement(0.20D, 0.0D, 0.0D);
                require(!player.canStandOnFluid(Fluids.WATER.defaultFluidState()),
                        "Water Run remained active after sprint intent expired");

                player.setSprinting(true);
                player.setDeltaMovement(0.02D, 0.0D, 0.0D);
                require(!player.canStandOnFluid(Fluids.WATER.defaultFluidState()),
                        "Water Run ignored its minimum horizontal speed");

                player.setDeltaMovement(0.20D, 0.0D, 0.0D);
                player.setShiftKeyDown(true);
                require(!player.canStandOnFluid(Fluids.WATER.defaultFluidState()),
                        "Water Run remained active while crouching");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static boolean close(float actual, float expected, float tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
