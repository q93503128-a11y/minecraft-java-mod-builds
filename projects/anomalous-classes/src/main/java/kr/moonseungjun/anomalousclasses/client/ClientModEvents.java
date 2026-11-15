package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import kr.moonseungjun.anomalousclasses.network.payload.ClassManagementStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ClassFocusStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.MentalDominatorStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteTraitModeStatePayload;
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
    public static void registerPuppetRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PuppetEntities.MARIONETTE.get(), PuppetRenderer::new);
    }

    @SubscribeEvent
    public static void registerPuppetLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PuppetModel.LAYER, PuppetModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClassSelectionKeyMappings.OPEN_PROFILE);
        event.register(ClassSelectionKeyMappings.OPEN_SKILL_TREE);
        event.register(ClassSelectionKeyMappings.OPEN_CLASS_MANAGEMENT);
        event.register(ClassSelectionKeyMappings.MENTAL_INTRUSION);
        event.register(ClassSelectionKeyMappings.MENTAL_SHOCK);
        event.register(ClassSelectionKeyMappings.MENTAL_SUGGESTION);
        event.register(ClassSelectionKeyMappings.MENTAL_OVERLOAD);
        event.register(ClassSelectionKeyMappings.MENTAL_CONTAINMENT);
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(ParasiteTraitModeStatePayload.TYPE, ParasiteTraitModeStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ParasiteTraitClientControls.confirm(payload.enabled())));
        registrar.playToClient(PuppetWorkshopStatePayload.TYPE, PuppetWorkshopStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(ClassSelectionStatePayload.TYPE, ClassSelectionStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(ProfileStatePayload.TYPE, ProfileStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(ClassManagementStatePayload.TYPE, ClassManagementStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(ClassFocusStatePayload.TYPE, ClassFocusStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(SkillTreeStatePayload.TYPE, SkillTreeStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
        registrar.playToClient(MentalDominatorStatePayload.TYPE, MentalDominatorStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClassSelectionClient.receive(payload)));
    }
}
