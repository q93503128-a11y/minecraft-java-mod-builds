package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementTuning;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Deterministic server-side behavior matrix for survival, movement and consumption families. */
public final class SurvivalResourceMatrixGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";
    private static final String RESERVE_POINTS_KEY = "reserve_meal_points";

    @GameTest
    public void miningAndMetabolismScalarsRespectOwnershipAndMaxClamp(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        AugmentationPlayerStateService.replace(player, empty);

        float baseStone = SurvivalAugmentService.modifyBlockBreakSpeed(
                player, Blocks.STONE.defaultBlockState(), 1.0F);
        float baseLog = SurvivalAugmentService.modifyBlockBreakSpeed(
                player, Blocks.OAK_LOG.defaultBlockState(), 1.0F);
        float baseExhaustion = SurvivalAugmentService.modifyFoodExhaustion(player, 4.0F);
        requireClose(baseStone, 1.0D, 1.0E-6D, "unowned mining modifier changed stone speed");
        requireClose(baseLog, 1.0D, 1.0E-6D, "unowned mining modifier changed log speed");
        requireClose(baseExhaustion, 4.0D, 1.0E-6D, "unowned metabolism changed exhaustion");

        String minersHand = id("miners_hand");
        AugmentDefinition minersDefinition = AugmentCatalog.require(minersHand);
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(minersHand, minersDefinition.maxLevel()));
        float minersMax = SurvivalAugmentService.modifyBlockBreakSpeed(
                player, Blocks.STONE.defaultBlockState(), 1.0F);
        require(minersMax > 1.0F, "miners hand did not increase ordinary block break speed");
        AugmentationPlayerStateService.replace(
                player, empty.withAugmentLevel(minersHand, minersDefinition.maxLevel() + 50));
        requireClose(
                SurvivalAugmentService.modifyBlockBreakSpeed(player, Blocks.STONE.defaultBlockState(), 1.0F),
                minersMax,
                1.0E-6D,
                "miners hand exceeded canonical max-level clamp"
        );

        String logging = id("logging_knack");
        AugmentDefinition loggingDefinition = AugmentCatalog.require(logging);
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(logging, loggingDefinition.maxLevel()));
        requireClose(
                SurvivalAugmentService.modifyBlockBreakSpeed(player, Blocks.STONE.defaultBlockState(), 1.0F),
                1.0D,
                1.0E-6D,
                "logging knack incorrectly affected non-log blocks"
        );
        require(SurvivalAugmentService.modifyBlockBreakSpeed(
                        player, Blocks.OAK_LOG.defaultBlockState(), 1.0F) > 1.0F,
                "logging knack did not increase log break speed");

        String metabolism = id("efficient_metabolism");
        AugmentDefinition metabolismDefinition = AugmentCatalog.require(metabolism);
        AugmentationPlayerStateService.replace(
                player, empty.withAugmentLevel(metabolism, metabolismDefinition.maxLevel()));
        float metabolismMax = SurvivalAugmentService.modifyFoodExhaustion(player, 4.0F);
        require(metabolismMax >= 0.0F && metabolismMax < 4.0F,
                "efficient metabolism did not reduce positive exhaustion");
        AugmentationPlayerStateService.replace(
                player, empty.withAugmentLevel(metabolism, metabolismDefinition.maxLevel() + 50));
        requireClose(
                SurvivalAugmentService.modifyFoodExhaustion(player, 4.0F),
                metabolismMax,
                1.0E-6D,
                "efficient metabolism exceeded canonical max-level clamp"
        );
        requireClose(SurvivalAugmentService.modifyFoodExhaustion(player, 0.0F), 0.0D, 1.0E-6D,
                "zero exhaustion was modified");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void oxygenAndStepHeightReconcileThenClear(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        AugmentationPlayerStateService.replace(player, empty);
        double oxygenBase = player.getAttributeValue(Attributes.OXYGEN_BONUS);
        double stepBase = player.getAttributeValue(Attributes.STEP_HEIGHT);

        String longBreath = id("long_breath");
        String stepUp = id("step_up");
        AugmentationPlayerState owned = empty
                .withAugmentLevel(longBreath, AugmentCatalog.require(longBreath).maxLevel())
                .withAugmentLevel(stepUp, AugmentCatalog.require(stepUp).maxLevel());
        AugmentationPlayerStateService.replace(player, owned);
        require(player.getAttributeValue(Attributes.OXYGEN_BONUS) > oxygenBase,
                "long breath did not increase oxygen bonus attribute");
        require(player.getAttributeValue(Attributes.STEP_HEIGHT) > stepBase,
                "step up did not increase step height attribute");

        AugmentationPlayerStateService.replace(player, empty);
        requireClose(player.getAttributeValue(Attributes.OXYGEN_BONUS), oxygenBase, 1.0E-9D,
                "long breath oxygen modifier did not clear");
        requireClose(player.getAttributeValue(Attributes.STEP_HEIGHT), stepBase, 1.0E-9D,
                "step up modifier did not clear");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void consumptionAndFishingContractsRespectOwnershipAndClamp(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        ItemStack bread = new ItemStack(Items.BREAD);
        FoodProperties food = bread.get(DataComponents.FOOD);
        require(food != null, "bread is missing vanilla food properties");

        AugmentationPlayerStateService.replace(player, empty);
        require(ConsumptionAugmentService.modifyUseDuration(bread, player, 32) == 32,
                "unowned fast eating changed use duration");
        requireClose(ConsumptionAugmentService.easedUseSpeedMultiplier(player, 0.20F), 0.20D, 1.0E-6D,
                "unowned eat-while-walking changed use movement");
        requireClose(ConsumptionAugmentService.fishingWaitReduction(player), 0.0D, 1.0E-9D,
                "unowned fishing sense changed fishing wait");
        requireClose(ConsumptionAugmentService.treasureFishingChanceBonus(player), 0.0D, 1.0E-9D,
                "unowned treasure fishing changed treasure chance");
        require(!ConsumptionAugmentService.canEat(player, false),
                "unowned reserve meal bypassed vanilla full-food rejection");

        String fastEating = id("fast_eating");
        AugmentDefinition fastDefinition = AugmentCatalog.require(fastEating);
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(fastEating, fastDefinition.maxLevel()));
        int fastMax = ConsumptionAugmentService.modifyUseDuration(bread, player, 32);
        require(fastMax >= 1 && fastMax < 32, "fast eating did not shorten food use duration");
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(fastEating, fastDefinition.maxLevel() + 50));
        require(ConsumptionAugmentService.modifyUseDuration(bread, player, 32) == fastMax,
                "fast eating exceeded canonical max-level clamp");

        String walking = id("eat_while_walking");
        AugmentDefinition walkingDefinition = AugmentCatalog.require(walking);
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(walking, walkingDefinition.maxLevel()));
        float walkingMax = ConsumptionAugmentService.easedUseSpeedMultiplier(player, 0.20F);
        require(walkingMax > 0.20F && walkingMax <= 1.0F,
                "eat while walking did not ease movement penalty into valid bounds");
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(walking, walkingDefinition.maxLevel() + 50));
        requireClose(ConsumptionAugmentService.easedUseSpeedMultiplier(player, 0.20F), walkingMax, 1.0E-6D,
                "eat while walking exceeded canonical max-level clamp");

        String fishingSense = id("fishing_sense");
        String treasureFishing = id("treasure_fishing");
        AugmentDefinition fishingDefinition = AugmentCatalog.require(fishingSense);
        AugmentDefinition treasureDefinition = AugmentCatalog.require(treasureFishing);
        AugmentationPlayerStateService.replace(player, empty
                .withAugmentLevel(fishingSense, fishingDefinition.maxLevel())
                .withAugmentLevel(treasureFishing, treasureDefinition.maxLevel()));
        double fishingMax = ConsumptionAugmentService.fishingWaitReduction(player);
        double treasureMax = ConsumptionAugmentService.treasureFishingChanceBonus(player);
        require(fishingMax > 0.0D && fishingMax <= 1.0D,
                "fishing sense produced an invalid wait reduction");
        require(treasureMax > 0.0D && treasureMax <= 1.0D,
                "treasure fishing produced an invalid chance bonus");
        AugmentationPlayerStateService.replace(player, empty
                .withAugmentLevel(fishingSense, fishingDefinition.maxLevel() + 50)
                .withAugmentLevel(treasureFishing, treasureDefinition.maxLevel() + 50));
        requireClose(ConsumptionAugmentService.fishingWaitReduction(player), fishingMax, 1.0E-9D,
                "fishing sense exceeded canonical max-level clamp");
        requireClose(ConsumptionAugmentService.treasureFishingChanceBonus(player), treasureMax, 1.0E-9D,
                "treasure fishing exceeded canonical max-level clamp");

        String reserveMeal = id("reserve_meal");
        AugmentDefinition reserveDefinition = AugmentCatalog.require(reserveMeal);
        AugmentationPlayerState reserveState = empty.withAugmentLevel(reserveMeal, reserveDefinition.maxLevel());
        AugmentationPlayerStateService.replace(player, reserveState);
        require(ConsumptionAugmentService.canEat(player, false),
                "reserve meal did not allow food use while reserve had capacity");
        int capacity = dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentTuning
                .current().reserveCapacity(reserveDefinition.maxLevel());
        AugmentationPlayerStateService.replace(player, reserveState.withPersistentInt(RESERVE_POINTS_KEY, capacity));
        require(!ConsumptionAugmentService.canEat(player, false),
                "reserve meal ignored a full reserve capacity");
        AugmentationPlayerStateService.replace(player, reserveState.withPersistentInt(RESERVE_POINTS_KEY, 3));
        require(ConsumptionAugmentService.restoreFoodFromReserve(player, 10, 8) == 10,
                "reserve meal failed to restore a two-point hunger loss");
        require(ConsumptionAugmentService.reservePoints(AugmentationPlayerStateService.state(player)) == 1,
                "reserve meal spent the wrong number of reserve points");

        String heartyMeal = id("hearty_meal");
        AugmentDefinition heartyDefinition = AugmentCatalog.require(heartyMeal);
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(heartyMeal, heartyDefinition.maxLevel()));
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0.0F);
        ConsumptionAugmentService.beforeFoodConsumed(player, food);
        player.getFoodData().setFoodLevel(Math.min(20, 10 + food.nutrition()));
        player.getFoodData().setSaturation(1.0F);
        ConsumptionAugmentService.afterFoodConsumed(player, food);
        require(player.getFoodData().getSaturationLevel() > 1.0F,
                "hearty meal did not add saturation after food consumption");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void longJumpOnlyBoostsOwnedSprintingGroundJump(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        Vec3 start = new Vec3(1.0D, 0.42D, 0.5D);

        AugmentationPlayerStateService.replace(player, empty);
        player.setSprinting(true);
        player.setDeltaMovement(start);
        MovementAugmentService.afterGroundJump(player);
        requireVec(player.getDeltaMovement(), start, 1.0E-9D,
                "unowned long jump changed ground-jump motion");

        String longJump = id("long_jump");
        AugmentDefinition definition = AugmentCatalog.require(longJump);
        AugmentationPlayerStateService.replace(player, empty.withAugmentLevel(longJump, definition.maxLevel()));
        player.setSprinting(false);
        player.setDeltaMovement(start);
        MovementAugmentService.afterGroundJump(player);
        requireVec(player.getDeltaMovement(), start, 1.0E-9D,
                "long jump activated while not sprinting");

        player.setSprinting(true);
        player.setDeltaMovement(start);
        MovementAugmentService.afterGroundJump(player);
        double multiplier = 1.0D
                + MovementTuning.current().longJumpHorizontalBonusPerLevel() * definition.maxLevel();
        Vec3 boosted = player.getDeltaMovement();
        requireClose(boosted.x, start.x * multiplier, 1.0E-9D,
                "long jump horizontal X multiplier diverged from canonical tuning");
        requireClose(boosted.z, start.z * multiplier, 1.0E-9D,
                "long jump horizontal Z multiplier diverged from canonical tuning");
        requireClose(boosted.y, start.y, 1.0E-9D,
                "long jump incorrectly changed vertical jump motion");

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

    private static void requireClose(double actual, double expected, double tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    private static void requireVec(Vec3 actual, Vec3 expected, double tolerance, String message) {
        if (actual.distanceToSqr(expected) > tolerance * tolerance) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
