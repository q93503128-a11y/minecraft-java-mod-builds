package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class TitanEntityRenderers {
    private TitanEntityRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLLOW_COLOSSUS.get(), HollowColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.RIPPER.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SKITTER.get(), SpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.BULWARK.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.NEEDLER.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.HOWLER.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.CHRONO_HOUND.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.NULL_EYE.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.THE_PURSUER.get(), PursuerRenderer::new);
    }
}
