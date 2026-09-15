package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.IronGolemVisualEntity;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationEntities;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for the TURNBOUND Iron Golem presentation entity. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class IronGolemVisualRenderer extends LivingEntityRenderer<
        IronGolemVisualEntity,
        IronGolemVisualRenderState,
        IronGolemVisualModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
    private static final long PRESENTATION_EPOCH_NANOS = System.nanoTime();

    public IronGolemVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemVisualModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.62F);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TurnboundPresentationEntities.IRON_GOLEM.get(), IronGolemVisualRenderer::new);
    }

    @Override
    public void extractRenderState(
            IronGolemVisualEntity entity,
            IronGolemVisualRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.presentationPose = entity.presentationPose();
        state.presentationTime = (System.nanoTime() - PRESENTATION_EPOCH_NANOS) / 50_000_000.0F;
    }

    @Override
    public Identifier getTextureLocation(IronGolemVisualRenderState state) {
        return TEXTURE;
    }

    @Override
    public IronGolemVisualRenderState createRenderState() {
        return new IronGolemVisualRenderState();
    }
}
