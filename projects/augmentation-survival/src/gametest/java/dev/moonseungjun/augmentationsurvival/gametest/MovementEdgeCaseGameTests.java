package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Focused regressions for movement effects that are hard to judge by feel in a manual playtest. */
public final class MovementEdgeCaseGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 40)
    public void mantleActuallyRaisesOwnedAirbornePlayerAtClearLedge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos wall = helper.absolutePos(new BlockPos(4, 5, 4));

        for (int dy = -2; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setBlock(wall.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(wall.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("mantle"), 1)
        );
        double startX = wall.getX() - 0.42D;
        double startY = wall.getY() + 0.05D;
        double startZ = wall.getZ() + 0.5D;
        player.setPos(startX, startY, startZ);
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        player.setDeltaMovement(new Vec3(0.28D, -0.04D, 0.0D));

        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            require(player.getY() >= startY + 0.70D,
                    "owned mantle did not raise the player over a clear one-block ledge; startY="
                            + startY + ", actualY=" + player.getY());
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 40)
    public void terrainResistanceMitigatesActualCobwebVerticalSlowdown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer baseline = GameTestPlayers.survivalPlayer(helper);
        ServerPlayer resistant = GameTestPlayers.survivalPlayer(helper);
        BlockPos baseColumn = helper.absolutePos(new BlockPos(2, 7, 2));
        BlockPos resistantColumn = helper.absolutePos(new BlockPos(8, 7, 2));

        prepareCobwebColumn(level, baseColumn);
        prepareCobwebColumn(level, resistantColumn);

        AugmentationPlayerStateService.replace(baseline, AugmentationPlayerState.initial());
        AugmentationPlayerStateService.replace(
                resistant,
                AugmentationPlayerState.initial().withAugmentLevel(id("terrain_resistance"), 3)
        );
        requireClose(baseline.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), 0.0D, 1.0E-9D,
                "baseline movement efficiency was not zero");
        requireClose(resistant.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), 0.45D, 1.0E-9D,
                "terrain resistance level 3 did not apply 45% movement efficiency");

        double baselineStartY = baseColumn.getY() + 2.70D;
        double resistantStartY = resistantColumn.getY() + 2.70D;
        baseline.setPos(baseColumn.getX() + 0.5D, baselineStartY, baseColumn.getZ() + 0.5D);
        resistant.setPos(resistantColumn.getX() + 0.5D, resistantStartY, resistantColumn.getZ() + 0.5D);
        baseline.setDeltaMovement(0.0D, -0.45D, 0.0D);
        resistant.setDeltaMovement(0.0D, -0.45D, 0.0D);

        GameTestPlayers.runAfterDelay(helper, 8, () -> {
            double baselineDrop = baselineStartY - baseline.getY();
            double resistantDrop = resistantStartY - resistant.getY();
            require(baselineDrop > 0.0D, "baseline player did not descend through the cobweb column");
            require(resistantDrop > baselineDrop + 0.05D,
                    "terrain resistance did not mitigate cobweb vertical slowdown; baselineDrop="
                            + baselineDrop + ", resistantDrop=" + resistantDrop);
            GameTestPlayers.succeed(helper);
        });
    }

    private static void prepareCobwebColumn(ServerLevel level, BlockPos base) {
        for (int dy = -2; dy <= 5; dy++) {
            level.setBlock(base.above(dy), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = -2; dy <= 5; dy++) {
                    level.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireClose(double actual, double expected, double tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
