package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** Focused regressions for Rare-tier issues reported during the manual playtest pass. */
public final class RareTierPlaytestRegressionGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void nightVisionRefreshNeverEntersVanillaFlickerWindow(GameTestHelper helper) {
        SurvivalTuning.Snapshot tuning = SurvivalTuning.current();
        require(tuning.nightVisionRefreshThresholdTicks() > 200,
                "night vision refresh threshold re-entered vanilla <=200 tick flicker window");
        require(tuning.nightVisionRefreshDurationTicks() > tuning.nightVisionRefreshThresholdTicks(),
                "night vision refresh duration does not stay above its refresh threshold");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void reserveMealWorksThroughRealPlayerFoodAndHungerPaths(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        player.level().getServer().setDifficulty(Difficulty.NORMAL, true);
        String reserveMeal = id("reserve_meal");
        int level = AugmentCatalog.require(reserveMeal).maxLevel();
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(reserveMeal, level)
        );

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(0.0F);
        require(player.canEat(false),
                "reserve meal mixin did not allow food use at a full vanilla hunger bar");

        ItemStack bread = new ItemStack(Items.BREAD);
        FoodProperties food = bread.get(DataComponents.FOOD);
        Consumable consumable = bread.get(DataComponents.CONSUMABLE);
        require(food != null && consumable != null, "bread is missing food/consumable components");
        food.onConsume(player.level(), player, bread, consumable);

        int stored = ConsumptionAugmentService.reservePoints(AugmentationPlayerStateService.state(player));
        require(stored == food.nutrition(),
                "full-hunger food did not store its overflow nutrition; expected="
                        + food.nutrition() + ", actual=" + stored);

        player.getFoodData().setSaturation(0.0F);
        player.getFoodData().addExhaustion(5.0F);
        player.getFoodData().tick(player);
        require(player.getFoodData().getFoodLevel() == 20,
                "reserve meal did not refill the hunger point consumed by exhaustion");
        require(ConsumptionAugmentService.reservePoints(AugmentationPlayerStateService.state(player)) == stored - 1,
                "reserve meal did not spend exactly one reserve point after one hunger point was lost");

        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void sweepingHarvestReplantsOnlyWhenReplantIsOwned(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos swept = helper.absolutePos(new BlockPos(3, 2, 2));
        prepareMatureWheat(level, origin);
        prepareMatureWheat(level, swept);

        String sweeping = id("sweeping_harvest");
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(
                        sweeping,
                        AugmentCatalog.require(sweeping).maxLevel()
                )
        );
        require(player.gameMode.destroyBlock(origin), "failed to break root crop for sweeping-only regression");

        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            require(level.getBlockState(swept).isAir(),
                    "sweeping harvest replanted a derived crop without owning replant");

            prepareMatureWheat(level, origin);
            prepareMatureWheat(level, swept);
            level.addFreshEntity(new ItemEntity(
                    level,
                    origin.getX() + 0.5D,
                    origin.getY() + 0.5D,
                    origin.getZ() + 0.5D,
                    new ItemStack(Items.WHEAT_SEEDS, 2)
            ));
            level.addFreshEntity(new ItemEntity(
                    level,
                    swept.getX() + 0.5D,
                    swept.getY() + 0.5D,
                    swept.getZ() + 0.5D,
                    new ItemStack(Items.WHEAT_SEEDS, 2)
            ));

            String replant = id("replant");
            AugmentationPlayerStateService.replace(
                    player,
                    AugmentationPlayerState.initial()
                            .withAugmentLevel(sweeping, AugmentCatalog.require(sweeping).maxLevel())
                            .withAugmentLevel(replant, AugmentCatalog.require(replant).maxLevel())
            );
            require(player.gameMode.destroyBlock(origin),
                    "failed to break root crop for sweeping-plus-replant regression");

            GameTestPlayers.runAfterDelay(helper, 3, () -> {
                require(isFreshWheat(level, origin),
                        "replant did not restore the directly harvested crop");
                require(isFreshWheat(level, swept),
                        "replant did not extend to the crop harvested by sweeping harvest");
                GameTestPlayers.succeed(helper);
            });
        });
    }

    private static void prepareMatureWheat(ServerLevel level, BlockPos cropPos) {
        level.setBlockAndUpdate(cropPos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(
                cropPos,
                Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE)
        );
    }

    private static boolean isFreshWheat(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WHEAT)
                && level.getBlockState(pos).getValue(CropBlock.AGE) == 0;
    }

    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 2, z));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Override
    public void invokeTestMethod(java.lang.reflect.Method method, GameTestHelper helper) throws Exception {
        GameTestPlayers.invoke(this, helper, method);
    }
}
