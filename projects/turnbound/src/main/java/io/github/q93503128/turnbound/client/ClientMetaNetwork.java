package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientMetaNetwork {
    private ClientMetaNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(MetaSnapshotPayload.TYPE, ClientMetaNetwork::handle);
    }

    private static void handle(MetaSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientMetaState.update(payload.snapshot());
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof BattleScreen || minecraft.gui.screen() instanceof BattleResultScreen) return;
            if (minecraft.gui.screen() instanceof MetaMenuScreen screen) {
                screen.refreshSnapshot();
            } else {
                minecraft.gui.setScreen(new MetaMenuScreen(MetaMenuScreen.Tab.PARTY));
            }
        });
    }
}
