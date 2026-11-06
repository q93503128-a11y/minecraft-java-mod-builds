package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.integration.WorldBossesRaidCacheCoreIntegration;
import java.util.List;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional World Bosses 2.0.2 adapter: a single loot table drives both claim and preview. */
@Pseudo
@Mixin(targets = "com.mki.worldbosses.config.RaidCacheLootConfig", remap = false)
abstract class WorldBossesRaidCacheLootMixin {
    @Inject(method = "get", at = @At("RETURN"), remap = false, require = 0)
    private static void augment$extendPool(String cacheId, CallbackInfoReturnable<Object> callback) {
        WorldBossesRaidCacheCoreIntegration.applyPool(cacheId, callback.getReturnValue());
    }

    @Inject(method = "rollReward", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void augment$renderWonCoreAtItsTier(
            String cacheId, Random random, CallbackInfoReturnable<Object> callback
    ) {
        Object fixed = WorldBossesRaidCacheCoreIntegration.decorateReward(callback.getReturnValue());
        if (fixed != callback.getReturnValue()) callback.setReturnValue(fixed);
    }

    @Inject(method = "buildLootPool", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void augment$renderActualCoreChances(
            String cacheId, int maxEntries, CallbackInfoReturnable<List<?>> callback
    ) {
        List<?> fixed = WorldBossesRaidCacheCoreIntegration.decoratePreview(
                cacheId, maxEntries, callback.getReturnValue());
        if (fixed != callback.getReturnValue()) callback.setReturnValue(fixed);
    }
}
