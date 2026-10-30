package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveRangeTelegraphService;
import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Regression coverage for world-space active range telegraphs. */
public final class ActiveRangeTelegraphGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 20)
    public void sustainedAreaAccelerationTracksExactRuntimeRadius(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String area = id("area_acceleration");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(area, 1)
                .withActiveSlots(List.of(area)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "area acceleration route failed");
        require(Math.abs(ActiveRangeTelegraphService.trackedRadius(player, area)
                        - AreaAccelerationService.radius()) < 0.001F,
                "area acceleration telegraph radius does not match runtime radius");
        require(ActiveRangeTelegraphService.remainingTicks(player, area) > 0,
                "sustained area acceleration telegraph did not keep a timed boundary");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void instantPullDoesNotBecomePersistentField(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String pull = id("pull");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(pull, 1)
                .withActiveSlots(List.of(pull)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "pull route failed");
        require(ActiveRangeTelegraphService.trackedRadius(player, pull) == 0.0F,
                "instant pull incorrectly left a persistent field telegraph");
        require(ActiveRangeTelegraphService.remainingTicks(player, pull) == 0,
                "instant pull incorrectly left persistent telegraph duration");
        GameTestPlayers.succeed(helper);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
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
