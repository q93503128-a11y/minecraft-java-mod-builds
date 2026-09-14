package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.SpiderVisualEntity;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for the TURNBOUND Spider presentation entity. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class SpiderVisualRenderer extends LivingEntityRenderer<
        SpiderVisualEntity,
        SpiderVisualRenderState,
        SpiderVisualModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/spider/spider.png");
    private static final long PRESENTATION_EPOCH_NANOS = System.nanoTime();

    public SpiderVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderVisualModel(context.bakeLayer(SpiderVisualModel.LAYER)), 0.62F);
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SpiderVisualModel.LAYER, SpiderVisualModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TurnboundPresentationEntities.SPIDER.get(), SpiderVisualRenderer::new);
    }

    @Override
    public void extractRenderState(
            SpiderVisualEntity entity,
            SpiderVisualRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.presentationPose = entity.presentationPose();
        state.presentationTime = (System.nanoTime() - PRESENTATION_EPOCH_NANOS) / 50_000_000.0F;
    }

    @Override
    public Identifier getTextureLocation(SpiderVisualRenderState state) {
        return TEXTURE;
    }

    @Override
    public SpiderVisualRenderState createRenderState() {
        return new SpiderVisualRenderState();
    }
}
