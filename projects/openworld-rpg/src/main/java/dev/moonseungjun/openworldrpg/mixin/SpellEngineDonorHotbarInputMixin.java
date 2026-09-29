package dev.moonseungjun.openworldrpg.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Spell Engine's donor hotbar keys from becoming a second player-facing control scheme.
 */
@Mixin(
        targets = "net.spell_engine.client.input.SpellHotbar",
        remap = false
)
public abstract class SpellEngineDonorHotbarInputMixin {
    @Inject(
            method = "handleSlotsInternal",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void openworldRpg$disableDonorHotbarInput(
            @Coerce Object player,
            List<?> slots,
            @Coerce Object options,
            List<?> exclude,
            CallbackInfoReturnable<Object> cir
    ) {
        cir.setReturnValue(null);
    }
}
