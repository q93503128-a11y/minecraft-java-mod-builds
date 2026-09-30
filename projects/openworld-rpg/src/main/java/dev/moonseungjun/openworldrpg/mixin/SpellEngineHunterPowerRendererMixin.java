package dev.moonseungjun.openworldrpg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.moonseungjun.openworldrpg.client.HunterPowerArrowRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.spell_engine.client.render.SpellProjectileRenderer",
        remap = false
)
public abstract class SpellEngineHunterPowerRendererMixin {
    @Inject(
            method = "submit",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void openworldRpg$renderHunterPowerArrow(
            @Coerce Object state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (HunterPowerArrowRenderer.renderIfOwned(
                state, matrices, queue, cameraState
        )) {
            ci.cancel();
        }
    }
}
