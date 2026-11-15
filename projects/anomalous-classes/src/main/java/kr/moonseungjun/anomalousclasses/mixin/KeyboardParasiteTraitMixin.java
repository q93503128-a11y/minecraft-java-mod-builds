package kr.moonseungjun.anomalousclasses.mixin;

import kr.moonseungjun.anomalousclasses.client.ParasiteTraitClientControls;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Input interception is isolated to the parasite's explicit numeric-key mode. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardParasiteTraitMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void anomalousclasses$traitNumber(long window, int key, int scanCode,
                                              int action, int modifiers, CallbackInfo ci) {
        if (ParasiteTraitClientControls.interceptNumber(key, action)) ci.cancel();
    }
}
