package kr.moonseungjun.titanbreak;

import kr.moonseungjun.titanbreak.client.ClientNetworkHandlers;
import kr.moonseungjun.titanbreak.client.ColossusHitboxRenderer;
import kr.moonseungjun.titanbreak.client.TitanEntityRenderers;
import kr.moonseungjun.titanbreak.client.TitanHud;
import kr.moonseungjun.titanbreak.client.TitanKeyMappings;
import kr.moonseungjun.titanbreak.client.TitanResourceHud;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Titanbreak.MOD_ID, dist = Dist.CLIENT)
public final class TitanbreakClient {
    public TitanbreakClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientNetworkHandlers::register);
        modEventBus.addListener(TitanHud::registerLayers);
        modEventBus.addListener(TitanResourceHud::registerLayers);
        modEventBus.addListener(TitanEntityRenderers::register);
        modEventBus.addListener(TitanKeyMappings::register);
        NeoForge.EVENT_BUS.addListener(TitanHud::onVanillaLayer);
        NeoForge.EVENT_BUS.addListener(TitanKeyMappings::onKeyInput);
        NeoForge.EVENT_BUS.addListener(TitanKeyMappings::onInteractionInput);
        NeoForge.EVENT_BUS.addListener(ColossusHitboxRenderer::onExtract);
        NeoForge.EVENT_BUS.addListener(ColossusHitboxRenderer::onSubmit);
    }
}
