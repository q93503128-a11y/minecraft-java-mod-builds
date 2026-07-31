package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = LivingKingdoms.MOD_ID, dist = Dist.CLIENT)
public final class LivingKingdomsClient {
    public LivingKingdomsClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientNetworkHandlers::register);
    }
}
