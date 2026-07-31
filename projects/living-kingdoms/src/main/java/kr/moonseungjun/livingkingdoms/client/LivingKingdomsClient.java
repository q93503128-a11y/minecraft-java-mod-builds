package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LivingKingdoms.MOD_ID, dist = Dist.CLIENT)
public final class LivingKingdomsClient {
    public LivingKingdomsClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientNetworkHandlers::register);
        modEventBus.addListener(RealmCodexClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientSmokeDiagnostics::onClientTick);
        NeoForge.EVENT_BUS.addListener(VanillaTutorialSuppressor::onClientTick);
        NeoForge.EVENT_BUS.addListener(RealmCodexClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RealmCodexClient::onScreenInit);
    }
}
