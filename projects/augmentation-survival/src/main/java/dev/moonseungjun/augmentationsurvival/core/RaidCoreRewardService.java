package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Reward path invoked exactly when vanilla awards a participating player a raid victory. */
public final class RaidCoreRewardService {
    private static final String PROFILE_ID = "raid_victory";

    private RaidCoreRewardService() {
    }

    public static void reward(ServerPlayer player) {
        CoreDistributionCatalog.rollEventProfile(PROFILE_ID, player.level().getRandom())
                .forEach(tier -> grantCore(player, tier));
    }

    private static void grantCore(ServerPlayer player, AugmentTier tier) {
        ItemStack stack = new ItemStack(AugmentCoreItems.item(tier));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        AugmentationSurvival.LOGGER.debug(
                "Granted {} augment core to {} from raid victory",
                tier.id(),
                player.getGameProfile().name()
        );
    }
}
