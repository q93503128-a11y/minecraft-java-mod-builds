package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.entity.FantasyEntityTypes;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** First-pass render bridge: distinct mod entity types reuse stable vanilla skeletal animation rigs. */
public final class FantasyEntityRenderers {
    private FantasyEntityRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FantasyEntityTypes.SILVER_HART.get(), GoatRenderer::new);
        event.registerEntityRenderer(FantasyEntityTypes.ASH_HOUND.get(), WolfRenderer::new);
        event.registerEntityRenderer(FantasyEntityTypes.RIVER_WISP.get(), AllayRenderer::new);
    }
}
