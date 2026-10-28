package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.ReplantFallbackService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lifecycle and delayed-replant correctness repairs without changing the existing harvest pipeline. */
@Mixin(value = ResourceHarvestService.class, remap = false)
abstract class ResourceHarvestAuditFixMixin {
    @Shadow @Final private static Map<UUID, ?> BREAK_SNAPSHOTS;
    @Shadow @Final private static List<?> PENDING_REPLANTS;
    @Shadow @Final private static Map<UUID, Double> TUNNEL_DURABILITY_REFUND_CREDIT;

    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$registerLifecycleCleanup(CallbackInfo ci) {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BREAK_SNAPSHOTS.clear();
            PENDING_REPLANTS.clear();
            TUNNEL_DURABILITY_REFUND_CREDIT.clear();
            ReplantFallbackService.clear(server);
        });
    }

    @Inject(method = "scheduleReplant", at = @At("HEAD"))
    private static void augmentationSurvival$rememberReplantOwner(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            CropBlock crop,
            BlockState harvestedState,
            CallbackInfo ci
    ) {
        ReplantFallbackService.record(level, pos, player);
    }

    @Inject(method = "consumeNearbyPlantingItem", at = @At("RETURN"), cancellable = true)
    private static void augmentationSurvival$consumePickedUpPlantingItem(
            ServerLevel level,
            BlockPos pos,
            Item plantingItem,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            ReplantFallbackService.discard(level, pos);
            return;
        }
        if (ReplantFallbackService.consumeOwnerInventory(level, pos, plantingItem)) {
            cir.setReturnValue(true);
        }
    }
}
