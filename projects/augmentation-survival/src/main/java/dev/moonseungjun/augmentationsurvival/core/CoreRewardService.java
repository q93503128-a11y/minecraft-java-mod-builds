package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative first vertical slice for combat-sourced augmentation cores. */
public final class CoreRewardService {
    private static final Set<String> BASELINE_NORMAL_HOSTILES = Set.of(
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:creeper",
            "minecraft:spider"
    );

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
        if (player == null) {
            return;
        }

        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (entityId == null || !BASELINE_NORMAL_HOSTILES.contains(entityId.toString())) {
            return;
        }

        CoreDistributionCatalog.rollMobProfile("normal_hostile", level.getRandom())
                .ifPresent(tier -> grantCore(player, tier));
    }

    private static ServerPlayer resolvePlayer(Entity killer, DamageSource damageSource) {
        if (killer instanceof ServerPlayer player) {
            return player;
        }
        Entity sourceEntity = damageSource.getEntity();
        return sourceEntity instanceof ServerPlayer player ? player : null;
    }

    private static void grantCore(ServerPlayer player, AugmentTier tier) {
        ItemStack stack = new ItemStack(AugmentCoreItems.item(tier));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        AugmentationSurvival.LOGGER.debug(
                "Granted {} augment core to {} from baseline hostile reward",
                tier.id(),
                player.getGameProfile().name()
        );
    }
}
