package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.movement.MantleAugmentService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One-block ledge grab/mantle using server collision as the authority. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMantleMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$tryMantle(CallbackInfo ci) {
        MantleAugmentService.tryMantle((ServerPlayer) (Object) this);
    }
}
