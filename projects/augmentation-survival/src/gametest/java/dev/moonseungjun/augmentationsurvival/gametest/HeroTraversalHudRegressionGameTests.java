package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.TemporaryPlatformBlocks;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
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
import net.minecraft.world.phys.Vec3;

/** End-to-end regressions for the post-playtest aerial/HUD/platform closure bundle. */
public final class HeroTraversalHudRegressionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 40)
    public void aerialComboCanRechargeConsumeAndRechargeAgainWithoutDoubleJump(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("aerial_combo"), 1)
        );
        require(AugmentationPlayerStateService.state(player).level(id("double_jump")) == 0,
                "repeat aerial-combo regression accidentally owns double jump");

        BlockPos air = helper.absolutePos(new BlockPos(3, 7, 3));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.08D, 0.0D);
        MovementAugmentService.setJumpHeld(player, true);

        LivingEntity first = helper.spawnMob(EntityTypes.ZOMBIE, 3, 7, 4).withNoFreeWill().spawn();
        LivingEntity second = helper.spawnMob(EntityTypes.ZOMBIE, 4, 7, 3).withNoFreeWill().spawn();

        player.attack(first);
        require(player.getDeltaMovement().y > 0.0D,
                "first airborne ROOT melee hit did not consume the first aerial-combo recharge");

        player.setDeltaMovement(0.0D, -0.08D, 0.0D);
        player.setOnGround(false);
        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            player.attack(second);
            require(player.getDeltaMovement().y > 0.0D,
                    "second airborne ROOT melee hit did not recharge and consume another aerial jump");
            MovementAugmentService.setJumpHeld(player, false);
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 60)
    public void temporaryPlatformThroughActiveRouterCancelsStoredFallDamage(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("temporary_platform"), 1)
        );
        require(ActiveAbilityRouter.resolvedSlots(AugmentationPlayerStateService.state(player)).get(0)
                        .equals(id("temporary_platform")),
                "temporary platform did not resolve into the first empty active slot");

        BlockPos air = helper.absolutePos(new BlockPos(6, 9, 6));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(new Vec3(0.0D, -0.35D, 0.0D));
        player.fallDistance = 10.0F;
        player.setHealth(player.getMaxHealth());
        float before = player.getHealth();

        require(ActiveAbilityRouter.tryUseSlot(player, 0),
                "temporary platform failed through the actual active-slot router");
        BlockPos expectedPlatform = BlockPos.containing(
                player.getX(), Math.floor(player.getBoundingBox().minY) - 1.0D, player.getZ()
        );
        require(player.level().getBlockState(expectedPlatform).is(TemporaryPlatformBlocks.TEMPORARY_PLATFORM),
                "active router reported success but did not create the rescue platform beneath the player");

        GameTestPlayers.runAfterDelay(helper, 20, () -> {
            require(player.onGround(), "player did not land on the routed temporary platform");
            require(Math.abs(player.getHealth() - before) <= 1.0E-4F,
                    "routed temporary platform landing leaked stored fall damage to HP");
            GameTestPlayers.succeed(helper);
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
