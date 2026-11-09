package io.github.q93503128.turnbound.mixin;

import io.github.q93503128.turnbound.TurnboundClientWorldState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide only vanilla survival meters, never TURNBOUND battle HUD or other overlays. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void turnbound$hideSurvivalBars(DrawContext context, CallbackInfo ci) {
        if (TurnboundClientWorldState.enabled()) ci.cancel();
    }
}
