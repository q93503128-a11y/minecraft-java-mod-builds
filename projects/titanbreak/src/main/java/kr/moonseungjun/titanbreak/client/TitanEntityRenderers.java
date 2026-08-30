package kr.moonseungjun.titanbreak.client;

import com.geckolib.renderer.GeoEntityRenderer;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class TitanEntityRenderers {
    private TitanEntityRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLLOW_COLOSSUS.get(), HollowColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.RIPPER.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.RIPPER.get()).withScale(1.05F));
        event.registerEntityRenderer(ModEntities.SKITTER.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.SKITTER.get()).withScale(1.15F));
        event.registerEntityRenderer(ModEntities.BULWARK.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.BULWARK.get()).withScale(1.12F));
        event.registerEntityRenderer(ModEntities.NEEDLER.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.NEEDLER.get()).withScale(1.06F));
        event.registerEntityRenderer(ModEntities.HOWLER.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.HOWLER.get()).withScale(1.08F));
        event.registerEntityRenderer(ModEntities.CHRONO_HOUND.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.CHRONO_HOUND.get()).withScale(1.12F));
        event.registerEntityRenderer(ModEntities.NULL_EYE.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.NULL_EYE.get()).withScale(1.10F));
        event.registerEntityRenderer(ModEntities.THE_PURSUER.get(),
                context -> new GeoEntityRenderer<>(context, ModEntities.THE_PURSUER.get()).withScale(18.0F));
    }
}
