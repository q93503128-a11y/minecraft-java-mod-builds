package kr.moonseungjun.titanbreak;

import kr.moonseungjun.titanbreak.client.ClientNetworkHandlers;
import kr.moonseungjun.titanbreak.client.TitanHud;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Titanbreak.MOD_ID, dist = Dist.CLIENT)
public final class TitanbreakClient {
    public TitanbreakClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientNetworkHandlers::register);
        modEventBus.addListener(TitanHud::registerLayers);
        NeoForge.EVENT_BUS.addListener(TitanHud::onVanillaLayer);
    }
}
