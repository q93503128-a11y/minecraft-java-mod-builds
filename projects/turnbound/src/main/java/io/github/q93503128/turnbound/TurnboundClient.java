package io.github.q93503128.turnbound;

import io.github.q93503128.turnbound.client.ClientBattleNetwork;
import io.github.q93503128.turnbound.client.VanillaHudPolicy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Turnbound.MOD_ID, dist = Dist.CLIENT)
public final class TurnboundClient {
    public TurnboundClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientBattleNetwork::register);
        NeoForge.EVENT_BUS.addListener(VanillaHudPolicy::onGuiLayer);
    }
}
