package dev.moonseungjun.openworldrpg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides Spell Engine's default spell hotbar while retaining its cast/target/error presentation.
 */
@Mixin(
        targets = "net.spell_engine.client.gui.HudRenderHelper$SpellHotBarWidget",
        remap = false
)
public abstract class SpellEngineDonorHotbarHudMixin {
    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void openworldRpg$hideDonorSpellHotbar(
            @Coerce Object context,
            int screenWidth,
            int screenHeight,
            @Coerce Object viewModel,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
