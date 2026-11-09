package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AnomalousClasses.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClassSelectionKeyMappings.OPEN_PROFILE);
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ClassSelectionStatePayload.TYPE, ClassSelectionStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(ProfileStatePayload.TYPE, ProfileStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(SkillTreeStatePayload.TYPE, SkillTreeStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
    }
}
