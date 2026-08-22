package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = FrontierSettlement.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FrontierSettlementClient {
    private FrontierSettlementClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        SettlementNetwork.setSnapshotSink(ClientSettlementState::accept);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "settlement_resources"),
                SettlementHudOverlay::render);
    }
}
