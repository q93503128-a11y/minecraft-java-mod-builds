package dev.moonseungjun.fishinggame.mixin;

import dev.moonseungjun.fishinggame.world.FishingWorldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void fishinggame$disableVanillaBiteCycle(BlockPos blockPos, CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.getPlayerOwner() instanceof ServerPlayer
                && hook.level().dimension().equals(FishingWorldManager.LAKESIDE_LEVEL)) {
            ci.cancel();
        }
    }
}
