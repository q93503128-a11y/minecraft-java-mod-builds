package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.CombatHudClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Time Stop is presented in world space; suppress the obsolete screen-edge/wash treatment. */
@Mixin(value = CombatHudClient.class, remap = false)
abstract class TimeStopHudPresentationMixin {
    @Inject(method = "drawTimeStopEdge", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$suppressScreenOverlay(
            GuiGraphicsExtractor g,
            int width,
            int height,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
