package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ClientPhaseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes only the camera-inside-block overlay while Phase Shift intentionally occupies terrain. */
@Mixin(ScreenEffectRenderer.class)
abstract class ClientPhaseShiftScreenEffectMixin {
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$seeThroughPhasedTerrain(
            Player player,
            CallbackInfoReturnable<BlockState> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientPhaseState.phaseShiftActive() && minecraft.player == player) {
            cir.setReturnValue(null);
        }
    }
}
