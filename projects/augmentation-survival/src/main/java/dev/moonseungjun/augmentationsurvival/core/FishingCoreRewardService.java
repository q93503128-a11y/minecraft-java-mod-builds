package dev.moonseungjun.augmentationsurvival.core;

import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Adds the authored 12% core roll only when the vanilla treasure fishing table resolves. */
public final class FishingCoreRewardService {
    private static final Identifier TREASURE_FISHING_TABLE =
            Identifier.fromNamespaceAndPath("minecraft", "gameplay/fishing/treasure");
    private static final String PROFILE_ID = "treasure_fishing";

    private FishingCoreRewardService() {
    }

    public static void initialize() {
        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> holder.unwrapKey().ifPresent(key -> {
            if (!key.identifier().equals(TREASURE_FISHING_TABLE)) {
                return;
            }
            CoreDistributionCatalog.DistributionTemplate profile =
                    CoreDistributionCatalog.current().distributionTemplates().get(PROFILE_ID);
            if (profile == null) {
                throw new IllegalStateException("Missing core distribution profile: " + PROFILE_ID);
            }
            RandomSource random = context.getRandom();
            if (random.nextDouble() >= profile.coreRollChance()) {
                return;
            }
            drops.add(new ItemStack(AugmentCoreItems.item(rollTier(profile.weights(), random))));
        }));
    }

    private static AugmentTier rollTier(Map<AugmentTier, Double> weights, RandomSource random) {
        double roll = random.nextDouble();
        double cumulative = 0.0;
        AugmentTier last = null;
        for (AugmentTier tier : AugmentTier.values()) {
            Double weight = weights.get(tier);
            if (weight == null || weight <= 0.0) {
                continue;
            }
            last = tier;
            cumulative += weight;
            if (roll < cumulative) {
                return tier;
            }
        }
        if (last == null) {
            throw new IllegalStateException("Cannot roll empty fishing core distribution");
        }
        return last;
    }
}
