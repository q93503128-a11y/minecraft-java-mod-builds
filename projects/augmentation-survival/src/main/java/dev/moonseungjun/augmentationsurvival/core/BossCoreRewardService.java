package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

/**
 * Server-authoritative participant rewards for exact boss assignments.
 *
 * <p>The baseline recognizes Wither and Ender Dragon through the authored boss contract. External
 * boss adapters can call the same reward path later after proving an encounter and its participants.</p>
 */
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
        if (blocked || damageTaken <= 0.0F) {
            return;
        }
        Identifier bossId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (bossId == null || BossCoreRewardCatalog.exactAssignment(bossId).isEmpty()) {
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
        Identifier bossId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (bossId == null || BossCoreRewardCatalog.exactAssignment(bossId).isEmpty()) {
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
                rewardExactBoss(player, bossId, level);
            }
        }
    }

    public static void rewardExactBoss(
            ServerPlayer player,
            Identifier bossId,
            ServerLevel rewardLevel
    ) {
        String stableBossId = bossId.toString();
        boolean firstKill = !AugmentationPlayerStateService.hasBossFirstKillReward(player, stableBossId);
        var rewards = BossCoreRewardCatalog.rollExactBossReward(
                bossId,
                firstKill,
                rewardLevel.getRandom()
        );
        for (AugmentTier tier : rewards) {
            grantCore(player, tier, stableBossId);
        }
        if (firstKill) {
            AugmentationPlayerStateService.markBossFirstKillRewardClaimed(player, stableBossId);
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

    private static void grantCore(ServerPlayer player, AugmentTier tier, String bossId) {
        ItemStack stack = new ItemStack(AugmentCoreItems.item(tier));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        AugmentationSurvival.LOGGER.debug(
                "Granted {} augment core to {} from boss {}",
                tier.id(),
                player.getGameProfile().name(),
                bossId
        );
    }
}
