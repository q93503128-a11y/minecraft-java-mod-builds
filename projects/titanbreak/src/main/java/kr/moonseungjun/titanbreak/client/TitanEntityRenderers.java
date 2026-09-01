package kr.moonseungjun.titanbreak.client;

import com.geckolib.renderer.GeoEntityRenderer;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class TitanEntityRenderers {
    private TitanEntityRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLLOW_COLOSSUS.get(), HollowColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.RIPPER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.RIPPER.get()).withScale(1.05F));
        event.registerEntityRenderer(ModEntities.SPITTER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.SPITTER.get()).withScale(1.04F));
        event.registerEntityRenderer(ModEntities.SKITTER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.SKITTER.get()).withScale(1.15F));
        event.registerEntityRenderer(ModEntities.GLIDER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.GLIDER.get()).withScale(1.10F));
        event.registerEntityRenderer(ModEntities.BULWARK.get(), context -> new GeoEntityRenderer<>(context, ModEntities.BULWARK.get()).withScale(1.12F));
        event.registerEntityRenderer(ModEntities.NEEDLER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.NEEDLER.get()).withScale(1.06F));
        event.registerEntityRenderer(ModEntities.HOWLER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.HOWLER.get()).withScale(1.08F));
        event.registerEntityRenderer(ModEntities.JAMMER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.JAMMER.get()).withScale(1.08F));
        event.registerEntityRenderer(ModEntities.VOLTAIC.get(), context -> new GeoEntityRenderer<>(context, ModEntities.VOLTAIC.get()).withScale(1.08F));
        event.registerEntityRenderer(ModEntities.CINDER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.CINDER.get()).withScale(1.08F));
        event.registerEntityRenderer(ModEntities.REGROWER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.REGROWER.get()).withScale(1.10F));
        event.registerEntityRenderer(ModEntities.BURROWER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.BURROWER.get()).withScale(1.10F));
        event.registerEntityRenderer(ModEntities.CRUSHER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.CRUSHER.get()).withScale(1.18F));
        event.registerEntityRenderer(ModEntities.STALKER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.STALKER.get()).withScale(1.06F));
        event.registerEntityRenderer(ModEntities.BURSTLING.get(), context -> new GeoEntityRenderer<>(context, ModEntities.BURSTLING.get()).withScale(1.02F));
        event.registerEntityRenderer(ModEntities.SIPHON.get(), context -> new GeoEntityRenderer<>(context, ModEntities.SIPHON.get()).withScale(1.09F));
        event.registerEntityRenderer(ModEntities.CHRONO_HOUND.get(), context -> new GeoEntityRenderer<>(context, ModEntities.CHRONO_HOUND.get()).withScale(1.12F));
        event.registerEntityRenderer(ModEntities.NULL_EYE.get(), context -> new GeoEntityRenderer<>(context, ModEntities.NULL_EYE.get()).withScale(1.10F));
        event.registerEntityRenderer(ModEntities.IRON_MAW.get(), context -> new GeoEntityRenderer<>(context, ModEntities.IRON_MAW.get()).withScale(1.24F));
        event.registerEntityRenderer(ModEntities.REVENANT.get(), context -> new GeoEntityRenderer<>(context, ModEntities.REVENANT.get()).withScale(1.14F));
        event.registerEntityRenderer(ModEntities.APEX_STALKER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.APEX_STALKER.get()).withScale(1.10F));
        event.registerEntityRenderer(ModEntities.SHOCK_CHOIR.get(), context -> new GeoEntityRenderer<>(context, ModEntities.SHOCK_CHOIR.get()).withScale(1.16F));
        event.registerEntityRenderer(ModEntities.SIEGEBACK.get(), context -> new GeoEntityRenderer<>(context, ModEntities.SIEGEBACK.get()).withScale(1.20F));
        event.registerEntityRenderer(ModEntities.PHASE_LURKER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.PHASE_LURKER.get()).withScale(1.08F));
        event.registerEntityRenderer(ModEntities.WARDEN_NODE.get(), context -> new GeoEntityRenderer<>(context, ModEntities.WARDEN_NODE.get()).withScale(1.12F));
        event.registerEntityRenderer(ModEntities.HARVESTER.get(), context -> new GeoEntityRenderer<>(context, ModEntities.HARVESTER.get()).withScale(1.16F));
        event.registerEntityRenderer(ModEntities.THE_PURSUER.get(), PursuerRenderer::new);
        event.registerEntityRenderer(ModBossEntities.GRAVEMARCH_COLOSSUS.get(), GravemarchRenderer::new);
        event.registerEntityRenderer(ModBossEntities.BASTION_WALKER.get(), BastionWalkerRenderer::new);
        event.registerEntityRenderer(ModBossEntities.REGNANT_FLESH.get(), RegnantFleshRenderer::new);
        event.registerEntityRenderer(ModBossEntities.HUNDRED_EYED_WATCHER.get(), HundredEyedWatcherRenderer::new);
        event.registerEntityRenderer(ModBossEntities.CHRONOPHAGE.get(), ChronophageRenderer::new);
    }
}
