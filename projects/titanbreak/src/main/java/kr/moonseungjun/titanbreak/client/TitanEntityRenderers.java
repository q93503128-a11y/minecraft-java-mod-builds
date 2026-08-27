package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class TitanEntityRenderers {
    private TitanEntityRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLLOW_COLOSSUS.get(), PigRenderer::new);
    }
}
