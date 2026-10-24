package dev.moonseungjun.augmentationsurvival.acquisition;

import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative 3:1 upward core conversion used by the advanced augment device. */
public final class CoreUpgradeService {
    public static final int INPUT_COUNT = 3;

    private CoreUpgradeService() {
    }

    public static UpgradeResult upgradeOne(ServerPlayer player, AugmentTier sourceTier) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sourceTier, "sourceTier");

        AugmentTier targetTier = sourceTier.next().orElse(null);
        if (targetTier == null) {
            return new UpgradeResult(UpgradeStatus.TOP_TIER, sourceTier, null, 0);
        }
        if (AugmentOfferService.countCores(player, sourceTier) < INPUT_COUNT) {
            return new UpgradeResult(UpgradeStatus.NOT_ENOUGH_CORES, sourceTier, targetTier, 0);
        }
        if (!AugmentOfferService.consumeCores(player, sourceTier, INPUT_COUNT)) {
            return new UpgradeResult(UpgradeStatus.NOT_ENOUGH_CORES, sourceTier, targetTier, 0);
        }

        giveOrDrop(player, new ItemStack(AugmentCoreItems.item(targetTier)));
        return new UpgradeResult(UpgradeStatus.SUCCESS, sourceTier, targetTier, 1);
    }

    public static UpgradeResult upgradeAll(ServerPlayer player, AugmentTier sourceTier) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sourceTier, "sourceTier");

        AugmentTier targetTier = sourceTier.next().orElse(null);
        if (targetTier == null) {
            return new UpgradeResult(UpgradeStatus.TOP_TIER, sourceTier, null, 0);
        }
        int outputCount = AugmentOfferService.countCores(player, sourceTier) / INPUT_COUNT;
        if (outputCount <= 0) {
            return new UpgradeResult(UpgradeStatus.NOT_ENOUGH_CORES, sourceTier, targetTier, 0);
        }
        int inputCount = outputCount * INPUT_COUNT;
        if (!AugmentOfferService.consumeCores(player, sourceTier, inputCount)) {
            return new UpgradeResult(UpgradeStatus.NOT_ENOUGH_CORES, sourceTier, targetTier, 0);
        }

        int remaining = outputCount;
        int maxStack = AugmentCoreItems.item(targetTier).getDefaultMaxStackSize();
        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            giveOrDrop(player, new ItemStack(AugmentCoreItems.item(targetTier), batch));
            remaining -= batch;
        }
        return new UpgradeResult(UpgradeStatus.SUCCESS, sourceTier, targetTier, outputCount);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public enum UpgradeStatus {
        SUCCESS,
        NOT_ENOUGH_CORES,
        TOP_TIER
    }

    public record UpgradeResult(
            UpgradeStatus status,
            AugmentTier sourceTier,
            AugmentTier targetTier,
            int outputCount
    ) {
    }
}
