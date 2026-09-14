package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.StarterZombieVisualEntity;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderer registration for TURNBOUND-only character presentation entities. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class StarterZombieVisualRenderer extends LivingEntityRenderer<
        StarterZombieVisualEntity,
        LivingEntityRenderState,
        StarterZombieVisualModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "textures/entity/starter_zombie.png");

    public StarterZombieVisualRenderer(EntityRendererProvider.Context context) {
        super(context, new StarterZombieVisualModel(context.bakeLayer(StarterZombieVisualModel.LAYER)), 0.42F);
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StarterZombieVisualModel.LAYER, StarterZombieVisualModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TurnboundPresentationEntities.STARTER_ZOMBIE.get(), StarterZombieVisualRenderer::new);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
