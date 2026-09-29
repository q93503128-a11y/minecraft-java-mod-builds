package dev.moonseungjun.openworldrpg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.moonseungjun.openworldrpg.client.RebukeBurstRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only the Rebuke model-effect placeholder with the authored Kenney-backed 3D fan.
 */
@Mixin(
        targets = "net.spell_engine.client.render.SpellModelEffectRenderer",
        remap = false
)
public abstract class SpellEngineRebukeBurstRendererMixin {
    @Inject(
            method = "submit",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void openworldRpg$renderRebukeBurst(
            @Coerce Object state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (RebukeBurstRenderer.renderIfOwned(
                state,
                matrices,
                queue
        )) {
            ci.cancel();
        }
    }
}
