package io.github.q93503128.turnbound;

import io.github.q93503128.turnbound.client.ClientAudioNetwork;
import io.github.q93503128.turnbound.client.ClientAudioPlayback;
import io.github.q93503128.turnbound.client.ClientBattleNetwork;
import io.github.q93503128.turnbound.client.ClientMultiplayerPartyNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Minimal client registration for the content-rebuild foundation.
 * There is no legacy world installer, forced loading UI, quest guide, camera override or HUD suppression.
 */
@Mod(value = Turnbound.MOD_ID, dist = Dist.CLIENT)
public final class TurnboundClient {
    public TurnboundClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientBattleNetwork::register);
        modEventBus.addListener(ClientMultiplayerPartyNetwork::register);
        modEventBus.addListener(ClientAudioNetwork::register);
        NeoForge.EVENT_BUS.addListener(ClientAudioPlayback::onTick);
    }
}
