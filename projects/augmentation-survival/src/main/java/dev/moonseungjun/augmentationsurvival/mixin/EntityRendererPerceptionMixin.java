package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.ClientPerceptionState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-local danger outline; no global glowing/team state is mutated. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererPerceptionMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void augmentationSurvival$dangerOutline(
            Entity entity,
            EntityRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        if (ClientPerceptionState.isDangerEntity(entity.getId())) {
            state.outlineColor = ClientPerceptionState.dangerOutlineColor();
        }
    }
}
