package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationEntities;
import kr.moonseungjun.turnboundre.presentation.WitchVisualEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for the TURNBOUND Witch presentation entity. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class WitchVisualRenderer extends LivingEntityRenderer<
        WitchVisualEntity,
        WitchVisualRenderState,
        WitchVisualModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/witch/witch.png");
    private static final long PRESENTATION_EPOCH_NANOS = System.nanoTime();

    public WitchVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new WitchVisualModel(context.bakeLayer(ModelLayers.WITCH)), 0.46F);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TurnboundPresentationEntities.WITCH.get(), WitchVisualRenderer::new);
    }

    @Override
    public void extractRenderState(
            WitchVisualEntity entity,
            WitchVisualRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.offensive = entity.isAggressive();
        state.presentationTime = (System.nanoTime() - PRESENTATION_EPOCH_NANOS) / 50_000_000.0F;
    }

    @Override
    public Identifier getTextureLocation(WitchVisualRenderState state) {
        return TEXTURE;
    }

    @Override
    public WitchVisualRenderState createRenderState() {
        return new WitchVisualRenderState();
    }
}
