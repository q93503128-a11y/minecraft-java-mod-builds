package io.github.q93503128.turnbound.presentation;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * GeckoLib renderer for production humanoid enemies that carry visible weapon ItemStacks.
 *
 * <p>Do not provide a second GeoRenderState data map here. Minecraft 26.2/GeckoLib 5 injects
 * GeoRenderState storage into the vanilla entity render-state classes. A parallel custom map can
 * leave GeckoLib's ANIMATABLE_MANAGER unavailable during controller extraction.</p>
 */
final class BattleActorHeldItemRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<BattleActorEntity, R> {

    BattleActorHeldItemRenderer(
            EntityRendererProvider.Context context,
            GeoModel<BattleActorEntity> model,
            float scale
    ) {
        super(context, model);
        withScale(scale);
        withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }
}
