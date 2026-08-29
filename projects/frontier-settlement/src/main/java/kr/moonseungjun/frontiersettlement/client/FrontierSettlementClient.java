package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FrontierSettlement.MOD_ID, dist = Dist.CLIENT)
public final class FrontierSettlementClient {
    private static final Identifier RESOURCE_LAYER =
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "settlement_resources");

    public FrontierSettlementClient(IEventBus modBus) {
        SettlementNetwork.setSnapshotSink(ClientSettlementState::accept);
        SettlementNetwork.setPlacementPreviewSink(BuildingPlacementClient::acceptPreview);
        SettlementNetwork.setRoadPreviewSink(RoadPlacementClient::acceptPreview);
        SettlementNetwork.setOutpostPreviewSink(OutpostPlacementClient::acceptPreview);
        SettlementNetwork.setCivilWorkPreviewSink(CivilWorkPlacementClient::acceptPreview);
        modBus.addListener(RegisterGuiLayersEvent.class, FrontierSettlementClient::onRegisterGuiLayers);
        modBus.addListener(RegisterKeyMappingsEvent.class, BuildingPlacementClient::registerKeys);
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, FrontierSettlementClient::onRegisterEntityRenderers);
        NeoForge.EVENT_BUS.addListener(CompanionKeyProfile::tick);
        NeoForge.EVENT_BUS.addListener(BuildingPlacementClient::tick);
        NeoForge.EVENT_BUS.addListener(RoadPlacementClient::tick);
        NeoForge.EVENT_BUS.addListener(OutpostPlacementClient::tick);
        NeoForge.EVENT_BUS.addListener(CivilWorkPlacementClient::tick);
        NeoForge.EVENT_BUS.addListener(PlacementGhostRenderer::extract);
        NeoForge.EVENT_BUS.addListener(PlacementGhostRenderer::submit);
        NeoForge.EVENT_BUS.addListener(RoadGhostRenderer::extract);
        NeoForge.EVENT_BUS.addListener(RoadGhostRenderer::submit);
        NeoForge.EVENT_BUS.addListener(OutpostGhostRenderer::extract);
        NeoForge.EVENT_BUS.addListener(OutpostGhostRenderer::submit);
        NeoForge.EVENT_BUS.addListener(CivilWorkGhostRenderer::extract);
        NeoForge.EVENT_BUS.addListener(CivilWorkGhostRenderer::submit);
        NeoForge.EVENT_BUS.addListener(FrontierSettlementClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSettlementState.reset();
        BuildingPlacementClient.cancelAllModes();
        SettlementNoticeQueue.clear();
        CompanionKeyProfile.resetSession();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(RESOURCE_LAYER, SettlementHudOverlay::render);
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FrontierContent.FRONTIER_SOLDIER.get(), FrontierSoldierRenderer::new);
        event.registerEntityRenderer(FrontierContent.FRONTIER_WORKER.get(), FrontierWorkerRenderer::new);
    }
}
