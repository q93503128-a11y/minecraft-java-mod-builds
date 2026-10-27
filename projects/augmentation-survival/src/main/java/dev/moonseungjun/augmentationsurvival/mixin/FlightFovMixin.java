package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Speed-readable FOV for powered flight. Respects the player's vanilla FOV-effect scale. */
@Mixin(GameRenderer.class)
abstract class FlightFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$poweredFlightFov(
            Camera camera,
            float tickProgress,
            boolean changingFov,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!changingFov) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || ClientAugmentProgressState.level(FlightAugmentService.FLIGHT_ID) <= 0
                || !client.player.isNoGravity()
                || client.player.getAbilities().flying) return;

        double speed = client.player.getDeltaMovement().length();
        float normalized = (float) Mth.clamp((speed - 0.42D) / (2.30D - 0.42D), 0.0D, 1.0D);
        if (normalized <= 0.0F) return;
        float eased = normalized * normalized * (3.0F - 2.0F * normalized);
        double effectScale = client.options.fovEffectScale().get();
        cir.setReturnValue(cir.getReturnValueF() + (float) (10.0D * effectScale * eased));
    }
}
