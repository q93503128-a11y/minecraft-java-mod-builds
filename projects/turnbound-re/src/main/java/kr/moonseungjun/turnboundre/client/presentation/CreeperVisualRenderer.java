package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.CreeperVisualEntity;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationEntities;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for the TURNBOUND Creeper presentation entity. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class CreeperVisualRenderer extends LivingEntityRenderer<
        CreeperVisualEntity,
        CreeperVisualRenderState,
        CreeperVisualModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png");
    private static final long PRESENTATION_EPOCH_NANOS = System.nanoTime();

    public CreeperVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperVisualModel(context.bakeLayer(ModelLayers.CREEPER)), 0.50F);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TurnboundPresentationEntities.CREEPER.get(), CreeperVisualRenderer::new);
    }

    @Override
    public void extractRenderState(
            CreeperVisualEntity entity,
            CreeperVisualRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.presentationPose = entity.presentationPose();
        state.volatileCharged = entity.volatileCharged();
        state.presentationTime = (System.nanoTime() - PRESENTATION_EPOCH_NANOS) / 50_000_000.0F;
    }

    @Override
    public Identifier getTextureLocation(CreeperVisualRenderState state) {
        return TEXTURE;
    }

    @Override
    public CreeperVisualRenderState createRenderState() {
        return new CreeperVisualRenderState();
    }
}
