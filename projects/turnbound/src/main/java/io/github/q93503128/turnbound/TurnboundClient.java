package io.github.q93503128.turnbound;

import io.github.q93503128.turnbound.client.BattleCameraController;
import io.github.q93503128.turnbound.client.ClientBattleNetwork;
import io.github.q93503128.turnbound.client.ClientFieldNetwork;
import io.github.q93503128.turnbound.client.ClientMetaNetwork;
import io.github.q93503128.turnbound.client.MetaMenuKeyHandler;
import io.github.q93503128.turnbound.client.VanillaHudPolicy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Turnbound.MOD_ID, dist = Dist.CLIENT)
public final class TurnboundClient {
    public TurnboundClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientBattleNetwork::register);
        modEventBus.addListener(ClientFieldNetwork::register);
        modEventBus.addListener(ClientMetaNetwork::register);
        NeoForge.EVENT_BUS.addListener(VanillaHudPolicy::onGuiLayer);
        NeoForge.EVENT_BUS.addListener(BattleCameraController::onDetachedCameraDistance);
        NeoForge.EVENT_BUS.addListener(BattleCameraController::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(BattleCameraController::onFov);
        NeoForge.EVENT_BUS.addListener(MetaMenuKeyHandler::onKey);
    }
}
