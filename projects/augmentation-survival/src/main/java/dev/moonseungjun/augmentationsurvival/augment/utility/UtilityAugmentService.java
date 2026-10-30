package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/** Server-authoritative runtime for utility, machine, pickup, spawn and death-XP augments. */
public final class UtilityAugmentService {
    private static final String PICKUP_HAND_ID = id("pickup_hand");
    private static final String SHIELD_GRIP_ID = id("shield_grip");
    private static final String FUEL_SAVER_ID = id("fuel_saver");
    private static final String FAST_BREWING_ID = id("fast_brewing");
    private static final String WATER_RUN_ID = id("water_run");
    private static final String EXPERIENCE_PRESERVATION_ID = id("experience_preservation");
    public static final String INSTANT_MINING_ID = id("instant_mining");
    public static final String INSTANT_MINING_TOGGLE_KEY = "active_toggle/instant_mining";

    private UtilityAugmentService() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(UtilityAugmentService::tickPickupRadius);
        ServerEntityEvents.ALLOW_LOAD.register(UtilityAugmentService::allowEntityLoad);
        ServerPlayerEvents.COPY_FROM.register(UtilityAugmentService::copyExperienceOnRespawn);
    }

    public static float modifyShieldDisableSeconds(ServerPlayer player, float originalSeconds) {
        if (!(originalSeconds > 0.0F) || !Float.isFinite(originalSeconds)) {
            return originalSeconds;
        }
        int level = boundedLevel(AugmentationPlayerStateService.state(player), SHIELD_GRIP_ID);
        if (level <= 0) {
            return originalSeconds;
        }
        double reduction = UtilityAugmentTuning.current().shieldDisableReduction(level);
        return (float) Math.max(0.0D, originalSeconds * (1.0D - reduction));
    }

    public static int modifyFuelBurnDuration(
            ServerLevel level,
            BlockPos pos,
            int originalDuration
    ) {
        if (originalDuration <= 0) {
            return originalDuration;
        }
        int augmentLevel = highestNearbyLevel(level, pos, FUEL_SAVER_ID);
        if (augmentLevel <= 0) {
            return originalDuration;
        }
        double scaled = originalDuration
                * (1.0D + UtilityAugmentTuning.current().fuelBurnTimeBonus(augmentLevel));
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, Math.round(scaled)));
    }

    public static int modifyBrewingDuration(
            ServerLevel level,
            BlockPos pos,
            int originalDuration
    ) {
        if (originalDuration <= 0) {
            return originalDuration;
        }
        int augmentLevel = highestNearbyLevel(level, pos, FAST_BREWING_ID);
        if (augmentLevel <= 0) {
            return originalDuration;
        }
        double reduction = UtilityAugmentTuning.current().brewingTimeReduction(augmentLevel);
        double scaled = originalDuration * Math.max(0.01D, 1.0D - reduction);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, Math.round(scaled)));
    }

    /** Water Run ownership only; crouch/passenger/dive boundaries are enforced at canStandOnFluid. */
    public static boolean canRunOnWater(ServerPlayer player) {
        return boundedLevel(AugmentationPlayerStateService.state(player), WATER_RUN_ID) > 0;
    }

    public static boolean isInstantMiningEnabled(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        return boundedLevel(state, INSTANT_MINING_ID) > 0
                && state.persistentInt(INSTANT_MINING_TOGGLE_KEY) > 0;
    }

    /** Mythic instant mining is an explicit active toggle; ownership alone never changes mining speed. */
    public static boolean shouldInstantMine(ServerPlayer player, BlockState state, float vanillaSpeed) {
        return vanillaSpeed > 0.0F
                && Float.isFinite(vanillaSpeed)
                && isInstantMiningEnabled(player)
                && player.hasCorrectToolForDrops(state);
    }

    public static int modifyDeathExperienceReward(ServerPlayer player, int vanillaReward) {
        if (vanillaReward <= 0) {
            return vanillaReward;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, EXPERIENCE_PRESERVATION_ID);
        if (level <= 0) {
            return vanillaReward;
        }
        double retained = UtilityAugmentTuning.current().experienceRetainFraction(level);
        return Math.max(0, (int) Math.floor(vanillaReward * (1.0D - retained)));
    }

    private static void tickPickupRadius(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isDeadOrDying() || player.isSpectator()) {
                continue;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            int level = boundedLevel(state, PICKUP_HAND_ID);
            if (level <= 0) {
                continue;
            }
            double radius = UtilityAugmentTuning.current().pickupRadius(level);
            List<ItemEntity> items = player.level().getEntitiesOfClass(
                    ItemEntity.class,
                    player.getBoundingBox().inflate(radius),
                    item -> item.isAlive()
            );
            for (ItemEntity item : items) {
                item.playerTouch(player);
            }
            List<ExperienceOrb> orbs = player.level().getEntitiesOfClass(
                    ExperienceOrb.class,
                    player.getBoundingBox().inflate(radius),
                    orb -> orb.isAlive()
            );
            for (ExperienceOrb orb : orbs) {
                orb.playerTouch(player);
            }
        }
    }

    private static boolean allowEntityLoad(
            Entity entity,
            ServerLevel level,
            EntitySpawnReason spawnReason,
            boolean loadedFromDisk
    ) {
        if (loadedFromDisk
                || spawnReason != EntitySpawnReason.NATURAL
                || !(entity instanceof LivingEntity living)
                || !(entity instanceof Enemy)
                || EntityCompatibilityProfileService.isBoss(living)) {
            return true;
        }
        double radius = UtilityAugmentTuning.current().threatSuppressionRadius();
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (!player.isDeadOrDying()
                    && !player.isSpectator()
                    && ActiveAbilityRouter.isThreatSuppressionEnabled(player)
                    && player.distanceToSqr(entity) <= radiusSquared) {
                return false;
            }
        }
        return true;
    }

    private static void copyExperienceOnRespawn(
            ServerPlayer oldPlayer,
            ServerPlayer newPlayer,
            boolean alive
    ) {
        if (alive || oldPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        AugmentationPlayerState oldState = AugmentationPlayerStateService.state(oldPlayer);
        int level = boundedLevel(oldState, EXPERIENCE_PRESERVATION_ID);
        if (level <= 0) {
            return;
        }

        double fraction = UtilityAugmentTuning.current().experienceRetainFraction(level);
        int retained = (int) Math.min(
                Integer.MAX_VALUE,
                Math.floor(currentExperiencePoints(oldPlayer) * fraction)
        );
        newPlayer.experienceLevel = 0;
        newPlayer.experienceProgress = 0.0F;
        newPlayer.totalExperience = 0;
        if (retained > 0) {
            newPlayer.giveExperiencePoints(retained);
        }
    }

    private static long currentExperiencePoints(ServerPlayer player) {
        int level = Math.max(0, player.experienceLevel);
        long start;
        if (level <= 16) {
            start = (long) level * level + 6L * level;
        } else if (level <= 31) {
            start = (5L * level * level - 81L * level + 720L) / 2L;
        } else {
            start = (9L * level * level - 325L * level + 4440L) / 2L;
        }
        long within = (long) Math.floor(
                Math.max(0.0F, Math.min(1.0F, player.experienceProgress))
                        * player.getXpNeededForNextLevel()
        );
        return Math.max(0L, Math.min(Integer.MAX_VALUE, start + within));
    }

    private static int highestNearbyLevel(ServerLevel level, BlockPos pos, String augmentId) {
        double radius = UtilityAugmentTuning.current().machineHolderRadius();
        double radiusSquared = radius * radius;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        int highest = 0;
        for (ServerPlayer player : level.players()) {
            if (player.isDeadOrDying() || player.isSpectator()) {
                continue;
            }
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dz = player.getZ() - z;
            if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                continue;
            }
            highest = Math.max(
                    highest,
                    boundedLevel(AugmentationPlayerStateService.state(player), augmentId)
            );
        }
        return highest;
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
