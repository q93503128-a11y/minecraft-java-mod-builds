package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ShieldThrowFlightService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Real-path regression for physical Shield Throw flight, ricochet and item return. */
public final class ShieldThrowFlightGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 160)
    public void shieldLeavesHandHitsMultipleTargetsAndReturns(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("shield_throw"), 3)
        );

        LivingEntity first = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 5).withNoFreeWill().spawn();
        LivingEntity second = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 6).withNoFreeWill().spawn();
        float firstBefore = first.getHealth();
        float secondBefore = second.getHealth();

        require(ShieldThrowFlightService.tryUse(player, 1.0D, true),
                "shield throw failed to launch with a valid shield, weapon and targets");
        require(player.getOffhandItem().isEmpty(),
                "physical shield remained equipped while its display was in flight");
        require(ShieldThrowFlightService.activeFlightCount(player) == 1,
                "physical shield flight was not registered");

        GameTestPlayers.runAfterDelay(helper, 60, () -> {
            require(first.getHealth() < firstBefore,
                    "physical shield did not damage the primary target");
            require(second.getHealth() < secondBefore,
                    "physical shield did not ricochet to the next target");
            require(player.getOffhandItem().is(Items.SHIELD),
                    "physical shield did not return to its original hand");
            require(ShieldThrowFlightService.activeFlightCount(player) == 0,
                    "shield flight remained active after returning to the owner");
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
