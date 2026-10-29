package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/** Canonical actual-melee/wall-contact regression for Wall Slam. */
public final class WallSlamGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 40)
    public void knockedTargetCollidingWithWallTakesBonusDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        player.setYRot(0.0F);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("wall_slam"), 3)
        );

        helper.setBlock(new BlockPos(1, 2, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.STONE);
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 5).withNoFreeWill().spawn();

        // Let pack-wide threat scaling settle before taking the damage snapshot.
        GameTestPlayers.runAfterDelay(helper, 5, () -> {
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            float before = target.getHealth();
            player.attack(target);
            float afterRoot = target.getHealth();
            require(afterRoot < before, "qualifying actual melee damage did not land");

            GameTestPlayers.runAfterDelay(helper, 2, () -> {
                require(target.getHealth() < afterRoot,
                        "canonical Wall Slam did not add derived damage at the nearby impact wall");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
