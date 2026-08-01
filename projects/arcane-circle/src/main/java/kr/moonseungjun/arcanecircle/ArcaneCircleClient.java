package kr.moonseungjun.arcanecircle;

import kr.moonseungjun.arcanecircle.client.ArcaneClient;
import kr.moonseungjun.arcanecircle.client.ArcaneHud;
import kr.moonseungjun.arcanecircle.client.ClientNetworkHandlers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ArcaneCircle.MOD_ID, dist = Dist.CLIENT)
public final class ArcaneCircleClient {
    public ArcaneCircleClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientNetworkHandlers::register);
        modEventBus.addListener(ArcaneClient::registerKeys);
        modEventBus.addListener(ArcaneHud::registerLayers);
        NeoForge.EVENT_BUS.addListener(ArcaneClient::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(ArcaneClient::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(ArcaneHud::onScreenRender);
    }
}
