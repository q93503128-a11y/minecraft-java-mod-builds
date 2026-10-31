package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative personal augmentation-core rewards for authored boss encounters. */
public final class BossCoreRewardService {
    private static final Map<UUID, Set<UUID>> PARTICIPANTS = new HashMap<>();

    private BossCoreRewardService() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(BossCoreRewardService::afterDamage);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(BossCoreRewardService::afterKilledOtherEntity);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> PARTICIPANTS.remove(entity.getUUID()));
    }

    private static void afterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked || damageTaken <= 0.0F || resolveRewardTarget(entity).isEmpty()) {
            return;
        }
        ServerPlayer player = resolvePlayer(source);
        if (player != null) {
            PARTICIPANTS.computeIfAbsent(entity.getUUID(), ignored -> new LinkedHashSet<>())
                    .add(player.getUUID());
        }
    }

    private static void afterKilledOtherEntity(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource damageSource
    ) {
        Optional<RewardTarget> target = resolveRewardTarget(killed);
        if (target.isEmpty()) {
            return;
        }

        Set<UUID> participantIds = PARTICIPANTS.remove(killed.getUUID());
        if (participantIds == null) {
            participantIds = new LinkedHashSet<>();
        } else {
            participantIds = new LinkedHashSet<>(participantIds);
        }
        ServerPlayer finalKiller = resolvePlayer(killer, damageSource);
        if (finalKiller != null) {
            participantIds.add(finalKiller.getUUID());
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        for (UUID playerId : participantIds) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                rewardBoss(player, target.get(), level);
            }
        }
    }

    /** Existing vanilla exact-assignment entry point retained for tests and explicit integrations. */
    public static void rewardExactBoss(
            ServerPlayer player,
            Identifier bossId,
            ServerLevel rewardLevel
    ) {
        BossCoreRewardCatalog.Assignment assignment = BossCoreRewardCatalog.exactAssignment(bossId)
                .orElseThrow(() -> new IllegalArgumentException("No exact boss reward assignment: " + bossId));
        rewardBoss(
                player,
                new RewardTarget(bossId.toString(), bossId.toString(), assignment),
                rewardLevel
        );
    }

    private static Optional<RewardTarget> resolveRewardTarget(LivingEntity entity) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) {
            return Optional.empty();
        }

        Optional<BossCoreRewardCatalog.Assignment> exact = BossCoreRewardCatalog.exactAssignment(entityId);
        if (exact.isPresent()) {
            return Optional.of(new RewardTarget(entityId.toString(), entityId.toString(), exact.get()));
        }

        Optional<EntityCompatibilityProfileService.Profile> compatibility =
                EntityCompatibilityProfileService.resolve(entity);
        if (compatibility.isEmpty() || !compatibility.get().boss()) {
            return Optional.empty();
        }
        String profileId = compatibility.get().id();
        return BossCoreRewardCatalog.compatibilityAssignment(profileId)
                .map(assignment -> new RewardTarget(
                        "compat:" + profileId,
                        entityId + " via " + profileId,
                        assignment
                ));
    }

    private static void rewardBoss(
            ServerPlayer player,
            RewardTarget target,
            ServerLevel rewardLevel
    ) {
        boolean firstKill = !AugmentationPlayerStateService.hasBossFirstKillReward(
                player,
                target.firstKillKey()
        );
        var rewards = BossCoreRewardCatalog.rollAssignedReward(
                target.assignment(),
                firstKill,
                rewardLevel.getRandom()
        );
        for (AugmentTier tier : rewards) {
            grantCore(player, tier, target.logSource());
        }
        if (firstKill) {
            AugmentationPlayerStateService.markBossFirstKillRewardClaimed(player, target.firstKillKey());
        }
    }

    private static ServerPlayer resolvePlayer(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        return sourceEntity instanceof ServerPlayer player ? player : null;
    }

    private static ServerPlayer resolvePlayer(Entity killer, DamageSource source) {
        if (killer instanceof ServerPlayer player) {
            return player;
        }
        return resolvePlayer(source);
    }

    private static void grantCore(ServerPlayer player, AugmentTier tier, String bossSource) {
        ItemStack stack = new ItemStack(AugmentCoreItems.item(tier));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        AugmentationSurvival.LOGGER.debug(
                "Granted {} augment core to {} from boss {}",
                tier.id(), player.getGameProfile().name(), bossSource
        );
    }

    private record RewardTarget(
            String firstKillKey,
            String logSource,
            BossCoreRewardCatalog.Assignment assignment
    ) {
    }
}
