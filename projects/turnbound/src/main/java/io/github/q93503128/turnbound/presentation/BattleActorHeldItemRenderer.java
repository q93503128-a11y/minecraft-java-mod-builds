package io.github.q93503128.turnbound.presentation;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * GeckoLib 5.5.3 renderer for production humanoid enemies that carry visible weapon ItemStacks.
 *
 * GeckoLib's held-item layer requires a render state that is both LivingEntityRenderState and GeoRenderState.
 * The base renderer's generic fallback cannot express that intersection at an inline factory call, so this renderer
 * owns a concrete state implementation and creates it explicitly.
 */
final class BattleActorHeldItemRenderer
        extends GeoEntityRenderer<BattleActorEntity, BattleActorHeldItemRenderer.State> {

    BattleActorHeldItemRenderer(
            EntityRendererProvider.Context context,
            GeoModel<BattleActorEntity> model,
            float scale
    ) {
        super(context, model);
        withScale(scale);
        withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }

    @Override
    public State createRenderState(BattleActorEntity animatable, @Nullable Void relatedObject) {
        return new State();
    }

    static final class State extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckolibData;
        }
    }
}
