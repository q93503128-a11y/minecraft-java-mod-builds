package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.network.ConsumptionStatePayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Shared food, fishing and breeding augment boundary.
 *
 * <p>The server owns nutrition, reserve-food storage, fishing timing and breeding results. The
 * client receives only the small prediction subset needed for use duration / movement feel.</p>
 */
public final class ConsumptionAugmentService {
    private static final String HEARTY_MEAL_ID = id("hearty_meal");
    private static final String FISHING_SENSE_ID = id("fishing_sense");
    private static final String FAST_EATING_ID = id("fast_eating");
    private static final String TREASURE_FISHING_ID = id("treasure_fishing");
    private static final String TWINS_ID = id("twins");
    private static final String EAT_WHILE_WALKING_ID = id("eat_while_walking");
    private static final String RESERVE_MEAL_ID = id("reserve_meal");
    private static final String RESERVE_POINTS_KEY = "reserve_meal_points";

    private static final Map<UUID, FoodSnapshot> FOOD_SNAPSHOTS = new HashMap<>();

    private ConsumptionAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                HEARTY_MEAL_ID,
                FISHING_SENSE_ID,
                FAST_EATING_ID,
                TREASURE_FISHING_ID,
                TWINS_ID,
                EAT_WHILE_WALKING_ID,
                RESERVE_MEAL_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> syncClient(player, AugmentationPlayerStateService.state(player)));
        ServerPlayerEvents.LEAVE.register(player -> FOOD_SNAPSHOTS.remove(player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {
                    FOOD_SNAPSHOTS.remove(newPlayer.getUUID());
                    syncClient(newPlayer, AugmentationPlayerStateService.state(newPlayer));
                }
        );
        AugmentationSurvival.LOGGER.info(
                "Loaded consumption/fishing contract: food, reserve, fishing and breeding runtime"
        );
    }

    public static void syncClient(ServerPlayer player, AugmentationPlayerState state) {
        if (player.connection == null) {
            return;
        }
        ServerPlayNetworking.send(player, new ConsumptionStatePayload(
                boundedLevel(state, FAST_EATING_ID),
                boundedLevel(state, EAT_WHILE_WALKING_ID),
                boundedLevel(state, RESERVE_MEAL_ID),
                Math.max(0, state.persistentInt(RESERVE_POINTS_KEY))
        ));
    }

    public static int modifyUseDuration(ItemStack stack, LivingEntity user, int original) {
        if (original <= 1 || stack.get(DataComponents.FOOD) == null) {
            return original;
        }
        int level = levelFor(user, FAST_EATING_ID, ClientConsumptionState.fastEatingLevel());
        if (level <= 0) {
            return original;
        }
        double reduction = ConsumptionAugmentTuning.current().fastEatingReductionPerLevel() * level;
        return Math.max(1, (int) Math.ceil(original * Math.max(0.05D, 1.0D - reduction)));
    }

    public static boolean canEat(Player player, boolean vanillaResult) {
        if (vanillaResult) {
            return true;
        }
        int level = levelFor(player, RESERVE_MEAL_ID, ClientConsumptionState.reserveMealLevel());
        if (level <= 0) {
            return false;
        }
        int reserve = player instanceof ServerPlayer serverPlayer
                ? reservePoints(AugmentationPlayerStateService.state(serverPlayer))
                : ClientConsumptionState.reservePoints();
        return reserve < ConsumptionAugmentTuning.current().reserveCapacity(level);
    }

    public static void beforeFoodConsumed(ServerPlayer player, FoodProperties food) {
        FOOD_SNAPSHOTS.put(
                player.getUUID(),
                new FoodSnapshot(player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel())
        );
    }

    public static void afterFoodConsumed(ServerPlayer player, FoodProperties food) {
        FoodSnapshot before = FOOD_SNAPSHOTS.remove(player.getUUID());
        if (before == null) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);

        int heartyLevel = boundedLevel(state, HEARTY_MEAL_ID);
        if (heartyLevel > 0 && food.nutrition() > 0) {
            float vanillaSaturationGain = food.nutrition() * food.saturation() * 2.0F;
            float bonus = vanillaSaturationGain
                    * (float) ConsumptionAugmentTuning.current().heartySaturationBonus(heartyLevel);
            float current = player.getFoodData().getSaturationLevel();
            player.getFoodData().setSaturation(
                    Math.min(player.getFoodData().getFoodLevel(), current + bonus)
            );
        }

        int reserveLevel = boundedLevel(state, RESERVE_MEAL_ID);
        if (reserveLevel <= 0 || food.nutrition() <= 0) {
            return;
        }
        int overflow = Math.max(0, before.foodLevel() + food.nutrition() - 20);
        if (overflow <= 0) {
            return;
        }
        int capacity = ConsumptionAugmentTuning.current().reserveCapacity(reserveLevel);
        int oldReserve = reservePoints(state);
        int nextReserve = Math.min(capacity, oldReserve + overflow);
        if (nextReserve != oldReserve) {
            AugmentationPlayerStateService.replace(
                    player,
                    state.withPersistentInt(RESERVE_POINTS_KEY, nextReserve)
            );
        }
    }

    /** Restores hunger lost by vanilla exhaustion from the reserve before the visible food bar falls. */
    public static int restoreFoodFromReserve(ServerPlayer player, int beforeFoodLevel, int afterFoodLevel) {
        if (afterFoodLevel >= beforeFoodLevel) {
            return afterFoodLevel;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, RESERVE_MEAL_ID);
        int reserve = reservePoints(state);
        if (level <= 0 || reserve <= 0) {
            return afterFoodLevel;
        }
        int spend = Math.min(beforeFoodLevel - afterFoodLevel, reserve);
        if (spend <= 0) {
            return afterFoodLevel;
        }
        AugmentationPlayerStateService.replace(
                player,
                state.withPersistentInt(RESERVE_POINTS_KEY, reserve - spend)
        );
        return Math.min(20, afterFoodLevel + spend);
    }

    public static float easedUseSpeedMultiplier(Player player, float vanillaMultiplier) {
        int level = levelFor(player, EAT_WHILE_WALKING_ID, ClientConsumptionState.eatWhileWalkingLevel());
        if (level <= 0) {
            return vanillaMultiplier;
        }
        double ease = ConsumptionAugmentTuning.current().eatWhileWalkingEase(level);
        return (float) Math.min(1.0D, vanillaMultiplier + (1.0D - vanillaMultiplier) * ease);
    }

    public static double fishingWaitReduction(ServerPlayer player) {
        int level = boundedLevel(AugmentationPlayerStateService.state(player), FISHING_SENSE_ID);
        return level <= 0 ? 0.0D : ConsumptionAugmentTuning.current().fishingWaitReduction(level);
    }

    public static double treasureFishingChanceBonus(ServerPlayer player) {
        int level = boundedLevel(AugmentationPlayerStateService.state(player), TREASURE_FISHING_ID);
        return level <= 0 ? 0.0D : ConsumptionAugmentTuning.current().treasureChanceBonus(level);
    }

    public static ServerPlayer breedingActor(Animal first, Animal second) {
        ServerPlayer a = first.getLoveCause();
        ServerPlayer b = second.getLoveCause();
        int aLevel = a == null ? 0 : boundedLevel(AugmentationPlayerStateService.state(a), TWINS_ID);
        int bLevel = b == null ? 0 : boundedLevel(AugmentationPlayerStateService.state(b), TWINS_ID);
        if (aLevel <= 0 && bLevel <= 0) {
            return null;
        }
        return bLevel > aLevel ? b : a;
    }

    public static void trySpawnTwin(
            ServerLevel level,
            Animal parent,
            Animal partner,
            AgeableMob vanillaOffspring,
            ServerPlayer actor
    ) {
        if (actor == null || vanillaOffspring == null) {
            return;
        }
        int augmentLevel = boundedLevel(AugmentationPlayerStateService.state(actor), TWINS_ID);
        if (augmentLevel <= 0
                || actor.getRandom().nextDouble() >= ConsumptionAugmentTuning.current().twinsChance(augmentLevel)) {
            return;
        }
        AgeableMob extra = parent.getBreedOffspring(level, partner);
        if (extra == null) {
            return;
        }
        extra.setBaby(true);
        extra.setPos(parent.getX(), parent.getY(), parent.getZ());
        level.addFreshEntity(extra);
    }

    public static int reservePoints(AugmentationPlayerState state) {
        return Math.max(0, state.persistentInt(RESERVE_POINTS_KEY));
    }

    private static int levelFor(LivingEntity entity, String augmentId, int clientLevel) {
        if (entity instanceof ServerPlayer serverPlayer) {
            return boundedLevel(AugmentationPlayerStateService.state(serverPlayer), augmentId);
        }
        if (entity instanceof Player && entity.level().isClientSide()) {
            return Math.max(0, clientLevel);
        }
        return 0;
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private record FoodSnapshot(int foodLevel, float saturation) {
    }
}
