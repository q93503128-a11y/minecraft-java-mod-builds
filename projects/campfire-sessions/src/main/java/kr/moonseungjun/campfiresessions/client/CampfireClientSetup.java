package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.client.museum.MuseumRenderPipelines;
import kr.moonseungjun.campfiresessions.client.museum.MuseumStaticMeshRenderer;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CampfireClientSetup {
    private CampfireClientSetup() {}

    public static void register(IEventBus modBus) {
        LocalMusicLibrary.register(modBus);
        modBus.addListener(CampfireClientSetup::registerClientExtensions);
        modBus.addListener(MuseumRenderPipelines::register);
        NeoForge.EVENT_BUS.addListener(MuseumStaticMeshRenderer::registerFeatureRenderer);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final HumanoidModel.ArmPose guitarPose = GuitarArmPoseParams.GUITAR_POSE.getValue();

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                var player = Minecraft.getInstance().player;
                if (player != null && entity.getId() == player.getId() && CampfireMusicClient.isPlaying()) {
                    return guitarPose;
                }
                return null;
            }
        }, ModItems.ACOUSTIC_GUITAR.get());
    }
}
