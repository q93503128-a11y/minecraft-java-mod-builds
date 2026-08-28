package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.BattleSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientBattleNetwork { private ClientBattleNetwork(){}
    public static void register(RegisterClientPayloadHandlersEvent e){e.register(BattleSnapshotPayload.TYPE,ClientBattleNetwork::handle);}
    private static void handle(BattleSnapshotPayload p, IPayloadContext c){ ClientBattleState.update(p.snapshot()); Minecraft mc=Minecraft.getInstance(); var s=ClientBattleState.snapshot(); if(s.active()){if(!(mc.gui.screen() instanceof BattleScreen))mc.setScreen(new BattleScreen());}else if(mc.gui.screen() instanceof BattleScreen)mc.setScreen(null); }
}
