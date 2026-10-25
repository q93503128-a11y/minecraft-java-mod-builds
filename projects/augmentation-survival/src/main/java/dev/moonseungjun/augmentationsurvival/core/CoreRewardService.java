package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative combat-sourced augmentation-core rewards. */
public final class CoreRewardService {
    private CoreRewardService() {
    }

    public static void initialize() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(CoreRewardService::afterKilledOtherEntity);
    }

    private static void afterKilledOtherEntity(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource damageSource
    ) {
        ServerPlayer player = resolvePlayer(killer, damageSource);
        if (player == null || CompanionAugmentService.isDominated(killed)) {
            return;
        }

        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (entityId == null) {
            return;
        }

        CoreSourceResolver.HostileResolution resolution = CoreSourceResolver.resolveHostile(
                entityId,
                killed instanceof Enemy
        );
        switch (resolution.kind()) {
            case NONE, BOSS_SUPPRESSED -> {
                return;
            }
            case MOB -> CoreDistributionCatalog.rollMobProfile(resolution.profileId(), level.getRandom())
                    .ifPresent(tier -> grantCore(player, tier, resolution.profileId()));
            case EVENT -> CoreDistributionCatalog.rollEventProfile(resolution.profileId(), level.getRandom())
                    .forEach(tier -> grantCore(player, tier, resolution.profileId()));
        }
    }

    private static ServerPlayer resolvePlayer(Entity killer, DamageSource damageSource) {
        if (killer instanceof ServerPlayer player) {
            return player;
        }
        Entity sourceEntity = damageSource.getEntity();
        return sourceEntity instanceof ServerPlayer player ? player : null;
    }

    private static void grantCore(ServerPlayer player, AugmentTier tier, String sourceProfile) {
        ItemStack stack = new ItemStack(AugmentCoreItems.item(tier));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        AugmentationSurvival.LOGGER.debug(
                "Granted {} augment core to {} from {}",
                tier.id(),
                player.getGameProfile().name(),
                sourceProfile
        );
    }
}
