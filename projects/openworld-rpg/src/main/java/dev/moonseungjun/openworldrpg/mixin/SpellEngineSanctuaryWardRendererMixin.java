package dev.moonseungjun.openworldrpg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.moonseungjun.openworldrpg.client.ConsecratedGroundRenderer;
import dev.moonseungjun.openworldrpg.client.SanctuaryWardRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces project-owned Cleric holy-ground model effects with authored 3D ground wards.
 */
@Mixin(
        targets = "net.spell_engine.client.render.SpellModelEffectRenderer",
        remap = false
)
public abstract class SpellEngineSanctuaryWardRendererMixin {
    @Inject(
            method = "submit",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void openworldRpg$renderSanctuaryWard(
            @Coerce Object state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (ConsecratedGroundRenderer.renderIfOwned(
                state,
                matrices,
                queue
        ) || SanctuaryWardRenderer.renderIfOwned(
                state,
                matrices,
                queue
        )) {
            ci.cancel();
        }
    }
}
