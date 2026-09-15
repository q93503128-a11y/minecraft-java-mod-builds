package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.BlazeVisualEntity;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationEntities;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for the TURNBOUND Blaze presentation entity. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BlazeVisualRenderer extends LivingEntityRenderer<
        BlazeVisualEntity,
        BlazeVisualRenderState,
        BlazeVisualModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/entity/blaze.png");
    private static final long PRESENTATION_EPOCH_NANOS = System.nanoTime();

    public BlazeVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeVisualModel(context.bakeLayer(ModelLayers.BLAZE)), 0.38F);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TurnboundPresentationEntities.BLAZE.get(), BlazeVisualRenderer::new);
    }

    @Override
    public void extractRenderState(
            BlazeVisualEntity entity,
            BlazeVisualRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.aggressive = entity.isAggressive();
        state.presentationTime = (System.nanoTime() - PRESENTATION_EPOCH_NANOS) / 50_000_000.0F;
    }

    @Override
    public Identifier getTextureLocation(BlazeVisualRenderState state) {
        return TEXTURE;
    }

    @Override
    public BlazeVisualRenderState createRenderState() {
        return new BlazeVisualRenderState();
    }
}
