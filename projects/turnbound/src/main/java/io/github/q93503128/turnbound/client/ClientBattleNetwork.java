package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.BattleSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientBattleNetwork {
    private ClientBattleNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(BattleSnapshotPayload.TYPE, ClientBattleNetwork::handle);
    }

    private static void handle(BattleSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var previousSnapshot = ClientBattleState.snapshot();
            boolean wasActive = previousSnapshot.active();
            ClientBattleState.update(payload.snapshot());
            Minecraft minecraft = Minecraft.getInstance();
            var snapshot = ClientBattleState.snapshot();
            ClientAudioDirector.onBattleSnapshot(snapshot);

            if (snapshot.active()) {
                if (!wasActive) BattleCameraController.enter(snapshot.arenaYaw());
                else BattleCameraController.onSnapshotTransition(previousSnapshot, snapshot);
                if (snapshot.finished()) {
                    if (!(minecraft.gui.screen() instanceof BattleResultScreen)) minecraft.gui.setScreen(new BattleResultScreen());
                } else if (!(minecraft.gui.screen() instanceof BattleScreen)) {
                    minecraft.gui.setScreen(new BattleScreen());
                }
            } else {
                if (wasActive) BattleCameraController.exit();
                if (minecraft.gui.screen() instanceof BattleScreen || minecraft.gui.screen() instanceof BattleResultScreen) {
                    minecraft.gui.setScreen(null);
                }
            }
        });
    }
}
