package io.github.q93503128.turnbound;

import io.github.q93503128.turnbound.client.ClientBattleNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value=Turnbound.MOD_ID,dist=Dist.CLIENT)
public final class TurnboundClient { public TurnboundClient(IEventBus modEventBus){modEventBus.addListener(ClientBattleNetwork::register);} }
