package kr.moonseungjun.earthtostars.ship.client;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.content.EarthToStarsEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = EarthToStars.MOD_ID)
public final class EarthToStarsClientRendering {
    private EarthToStarsClientRendering() {
    }

    @SubscribeEvent
    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EarthToStarsEntities.SHIP_EXTERIOR.get(), ShipExteriorEntityRenderer::new);
    }
}
