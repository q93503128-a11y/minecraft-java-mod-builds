package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AnomalousClasses.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ClassSelectionKeyMappings.OPEN_PROFILE.consumeClick() && minecraft.player != null
                && minecraft.level != null && minecraft.screen == null) {
            PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
        }
    }
}
