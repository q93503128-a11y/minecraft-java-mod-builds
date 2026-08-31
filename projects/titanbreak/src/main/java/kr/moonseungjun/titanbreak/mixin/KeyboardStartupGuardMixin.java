package kr.moonseungjun.titanbreak.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.2 registers the GLFW key callback before constructing FramerateLimitTracker.
 * A key event during NeoForge's long parallel-load window can therefore enter KeyboardHandler
 * while the tracker is still null. Ignore only that pre-initialization input window.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardStartupGuardMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void titanbreak$ignorePreInitKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (minecraft.getFramerateLimitTracker() == null) ci.cancel();
    }
}
