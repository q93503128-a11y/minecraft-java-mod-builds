package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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
        modBus.addListener(RegisterGuiLayersEvent.class, FrontierSettlementClient::onRegisterGuiLayers);
        modBus.addListener(RegisterKeyMappingsEvent.class, BuildingPlacementClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(BuildingPlacementClient::tick);
        NeoForge.EVENT_BUS.addListener(PlacementGhostRenderer::extract);
        NeoForge.EVENT_BUS.addListener(PlacementGhostRenderer::submit);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(RESOURCE_LAYER, SettlementHudOverlay::render);
    }
}
