package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ExternalActorProgressionMixin {
    @Inject(
            method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void openworldRpg$suppressDonorExperience(
            ServerLevel level,
            Entity killer,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExternalActorBindingRuntime.ownsProgression(self)) {
            ci.cancel();
        }
    }
}
