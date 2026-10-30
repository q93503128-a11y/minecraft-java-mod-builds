package dev.moonseungjun.augmentationsurvival.gametest;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformBlocks;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformService;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformWorldState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Physical traversal regressions for Temporary Platform and Water Walk surface rules. */
public final class TraversalRescueGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void temporaryPlatformExpiryRecordSurvivesCodecRoundTrip(GameTestHelper helper) {
        var record = new TemporaryPlatformWorldState.PlatformRecord(
                "minecraft:overworld", 123456789L, "00000000-0000-0000-0000-000000000001", 6000L, 12000L
        );
        var original = TemporaryPlatformWorldState.initial().withPlatform("test", record);
        var encoded = TemporaryPlatformWorldState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        var restored = TemporaryPlatformWorldState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        require(restored.equals(original), "temporary platform cleanup state did not survive serialization");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
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
        player.fallDistance = 10.0F;
        player.causeFallDamage(10.0D, 1.0F, player.damageSources().fall());
        require(close(player.getHealth(), before, 1.0E-4F),
                "temporary platform landing leaked fall damage through LivingEntity.causeFallDamage");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void ordinaryBlockLandingStillDealsFallDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos floor = new BlockPos(4, 3, 4);
        helper.setBlock(floor, Blocks.STONE);
        BlockPos absoluteFloor = helper.absolutePos(floor);

        player.setPos(
                absoluteFloor.getX() + 0.5D,
                absoluteFloor.getY() + 1.0D,
                absoluteFloor.getZ() + 0.5D
        );
        player.setHealth(player.getMaxHealth());
        float before = player.getHealth();
        player.fallDistance = 10.0F;
        player.causeFallDamage(10.0D, 1.0F, player.damageSources().fall());
        require(player.getHealth() < before,
                "ordinary block landing incorrectly received temporary-platform fall immunity");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 40)
    public void waterWalkUsesOneWaySurfaceCrouchDiveAndSafeLanding(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos relativeWater = new BlockPos(4, 2, 4);
        helper.setBlock(relativeWater, Blocks.WATER);
        BlockPos waterPos = helper.absolutePos(relativeWater);
        BlockState water = level.getBlockState(waterPos);

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("water_run"), 1)
        );
        player.setPos(waterPos.getX() + 0.5D, waterPos.getY() + 1.0D, waterPos.getZ() + 0.5D);
        player.setShiftKeyDown(false);
        player.fallDistance = 0.0F;

        VoxelShape standing = water.getCollisionShape(level, waterPos, CollisionContext.of(player));
        require(!standing.isEmpty(), "Water Walk did not expose a water-surface collision while above water");

        player.setShiftKeyDown(true);
        require(water.getCollisionShape(level, waterPos, CollisionContext.of(player)).isEmpty(),
                "Water Walk surface remained solid while crouching");
        player.setShiftKeyDown(false);

        player.fallDistance = 5.0F;
        require(water.getCollisionShape(level, waterPos, CollisionContext.of(player)).isEmpty(),
                "five-block dive did not bypass the Water Walk surface");

        player.fallDistance = 0.0F;
        player.setPos(waterPos.getX() + 0.5D, waterPos.getY() + 0.20D, waterPos.getZ() + 0.5D);
        require(water.getCollisionShape(level, waterPos, CollisionContext.of(player)).isEmpty(),
                "Water Walk blocked a player rising from inside the water");

        player.setPos(waterPos.getX() + 0.5D, waterPos.getY() + 1.0D, waterPos.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        float before = player.getHealth();
        player.fallDistance = 4.5F;
        player.causeFallDamage(4.5D, 1.0F, player.damageSources().fall());
        require(close(player.getHealth(), before, 1.0E-4F),
                "Water Walk surface landing leaked fall damage");

        require(Blocks.LAVA.defaultBlockState()
                        .getCollisionShape(level, waterPos, CollisionContext.of(player)).isEmpty(),
                "Water Walk incorrectly exposed a collision surface for lava");
        GameTestPlayers.succeed(helper);
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
