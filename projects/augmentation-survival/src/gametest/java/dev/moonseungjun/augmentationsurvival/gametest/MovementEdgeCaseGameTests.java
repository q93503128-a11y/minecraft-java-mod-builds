package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.movement.MantleAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.TerrainResistanceService;
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

    @GameTest
    public void mantleRuntimeRaisesOnlyOwnedPlayerWhenDestinationIsClear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos wall = helper.absolutePos(new BlockPos(4, 5, 4));
        clearAround(level, wall);
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        double startX = wall.getX() - 0.31D;
        double startY = wall.getY();
        double startZ = wall.getZ() + 0.5D;

        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
        prepareMantleAttempt(player, startX, startY, startZ);
        require(!MantleAugmentService.tryMantle(player), "unowned mantle moved the player");
        requireClose(player.getY(), startY, 1.0E-9D, "unowned mantle changed player Y");

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("mantle"), 1)
        );
        prepareMantleAttempt(player, startX, startY, startZ);
        require(MantleAugmentService.tryMantle(player), "owned mantle rejected a clear ledge");
        requireClose(player.getY(), startY + 1.0D, 1.0E-9D,
                "owned mantle did not raise the player exactly one block");
        require(player.getX() > startX + 0.30D,
                "owned mantle did not move the player toward the ledge");
        require(player.getDeltaMovement().y >= 0.0D,
                "mantle retained downward velocity after climbing");

        level.setBlock(wall.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        prepareMantleAttempt(player, startX, startY, startZ);
        require(!MantleAugmentService.tryMantle(player),
                "mantle entered a blocked destination above the ledge");
        requireClose(player.getY(), startY, 1.0E-9D,
                "blocked mantle changed player Y");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void terrainResistanceMitigatesCobwebHorizontalAndVerticalStuckMultiplier(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        Vec3 vanillaCobweb = new Vec3(0.25D, 0.05D, 0.25D);
        Vec3 weavingCobweb = new Vec3(0.50D, 0.25D, 0.50D);

        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial());
        requireClose(player.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), 0.0D, 1.0E-9D,
                "baseline movement efficiency was not zero");
        requireVec(TerrainResistanceService.adjustCobwebMultiplier(player, vanillaCobweb), vanillaCobweb, 1.0E-9D,
                "unowned terrain resistance changed vanilla cobweb slowdown");

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("terrain_resistance"), 3)
        );
        requireClose(player.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), 0.45D, 1.0E-9D,
                "terrain resistance level 3 did not apply 45% movement efficiency");

        Vec3 adjusted = TerrainResistanceService.adjustCobwebMultiplier(player, vanillaCobweb);
        requireClose(adjusted.x, 0.5875D, 1.0E-9D,
                "terrain resistance level 3 produced the wrong cobweb X multiplier");
        requireClose(adjusted.y, 0.4775D, 1.0E-9D,
                "terrain resistance level 3 did not mitigate cobweb vertical slowdown");
        requireClose(adjusted.z, 0.5875D, 1.0E-9D,
                "terrain resistance level 3 produced the wrong cobweb Z multiplier");

        Vec3 weavingAdjusted = TerrainResistanceService.adjustCobwebMultiplier(player, weavingCobweb);
        requireClose(weavingAdjusted.x, 0.725D, 1.0E-9D,
                "terrain resistance diverged for Weaving cobweb X multiplier");
        requireClose(weavingAdjusted.y, 0.5875D, 1.0E-9D,
                "terrain resistance diverged for Weaving cobweb vertical multiplier");
        requireClose(weavingAdjusted.z, 0.725D, 1.0E-9D,
                "terrain resistance diverged for Weaving cobweb Z multiplier");

        GameTestPlayers.succeed(helper);
    }

    private static void prepareMantleAttempt(ServerPlayer player, double x, double y, double z) {
        player.setPos(x, y, z);
        player.setOnGround(false);
        player.horizontalCollision = true;
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        player.setDeltaMovement(new Vec3(0.28D, -0.04D, 0.0D));
    }

    private static void clearAround(ServerLevel level, BlockPos center) {
        for (int dy = -2; dy <= 4; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
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

    private static void requireVec(Vec3 actual, Vec3 expected, double tolerance, String message) {
        requireClose(actual.x, expected.x, tolerance, message + " (x)");
        requireClose(actual.y, expected.y, tolerance, message + " (y)");
        requireClose(actual.z, expected.z, tolerance, message + " (z)");
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
