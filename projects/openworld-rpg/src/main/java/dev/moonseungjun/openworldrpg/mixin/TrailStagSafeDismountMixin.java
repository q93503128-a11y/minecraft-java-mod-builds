package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.entity.TrailStagEntity;
import dev.moonseungjun.openworldrpg.mount.TrailStagDismountGuard;
import dev.moonseungjun.openworldrpg.mount.TrailStagLiveBinding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla shift-dismount from placing a Trail Stag rider inside unsafe geometry. */
@Mixin(Entity.class)
public abstract class TrailStagSafeDismountMixin {
    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    private void openworldRpg$authorizeTrailStagDismount(CallbackInfo ci) {
        if (TrailStagDismountGuard.authorized()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer player)
                || !(player.getVehicle() instanceof TrailStagEntity stag)
                || !stag.ownedBy(player)) {
            return;
        }

        TrailStagLiveBinding.requestSafeDismount(player, stag);
        // The authorized nested dismount already completed when a safe position existed. Otherwise
        // cancelling here intentionally keeps the rider mounted.
        ci.cancel();
    }
}
