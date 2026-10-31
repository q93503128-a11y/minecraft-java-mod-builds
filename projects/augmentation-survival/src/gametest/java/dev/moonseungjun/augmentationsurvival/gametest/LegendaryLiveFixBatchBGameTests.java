package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveChargePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.AreaAccelerationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Runtime regressions for the second Legendary live-play repair bundle. */
public final class LegendaryLiveFixBatchBGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest(maxTicks = 20)
    public void doubleChargeSnapshotShowsOneThenZeroReadyCharges(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String shield = id("shield");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(shield, 1)
                .withAugmentLevel(id("double_charge"), 1)
                .withActiveSlots(List.of(shield)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "first double-charge shield cast failed");
        long now = player.level().getGameTime();
        ActiveChargePresentationService.Snapshot afterFirst = ActiveChargePresentationService.snapshot(
                AugmentationPlayerStateService.state(player), shield, now
        );
        require(afterFirst.maxCharges() == 2, "double charge was not exposed as a two-charge active");
        require(afterFirst.readyCharges() == 1, "first cast did not leave exactly one ready HUD charge");
        require(afterFirst.nextRechargeTicks() > 0, "first spent charge has no visible recharge timer");

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "second double-charge shield cast failed");
        ActiveChargePresentationService.Snapshot afterSecond = ActiveChargePresentationService.snapshot(
                AugmentationPlayerStateService.state(player), shield, player.level().getGameTime()
        );
        require(afterSecond.readyCharges() == 0, "second cast did not consume the remaining HUD charge");
        require(afterSecond.nextRechargeTicks() > 0, "sequential recharge timer vanished after both charges were spent");
        require(afterSecond.charge0Ticks() != afterSecond.charge1Ticks(),
                "both charges were scheduled to recover at once instead of one-by-one");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 120)
    public void areaAccelerationRoutesIntoRealFurnaceTicks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        String area = id("area_acceleration");
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(area, 1)
                .withActiveSlots(List.of(area)));

        ServerLevel level = (ServerLevel) player.level();
        BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 2, 1));
        level.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
        require(level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity,
                "test furnace block entity was not created");
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "area acceleration route failed");
        require(AreaAccelerationService.multiplierAt(level, furnacePos) == 15,
                "level-one area acceleration did not expose its x15 runtime multiplier");

        GameTestPlayers.runAfterDelay(helper, 80, () -> {
            ItemStack output = furnace.getItem(2);
            require(output.is(Items.IRON_INGOT) && output.getCount() >= 1,
                    "area acceleration did not accelerate the real furnace server tick path");
            GameTestPlayers.succeed(helper);
        });
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
