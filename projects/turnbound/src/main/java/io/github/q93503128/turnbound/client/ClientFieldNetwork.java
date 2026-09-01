package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.FieldSnapshotPayload;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientFieldNetwork {
    private ClientFieldNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(FieldSnapshotPayload.TYPE, ClientFieldNetwork::handle);
    }

    private static void handle(FieldSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientFieldState.update(payload.snapshot());
            Minecraft minecraft = Minecraft.getInstance();
            FieldUiSnapshot snapshot = ClientFieldState.snapshot();
            ClientAudioDirector.onFieldSnapshot(snapshot);
            if (!snapshot.active()) {
                if (minecraft.gui.screen() instanceof FieldPanelScreen || minecraft.gui.screen() instanceof WorldLoadingScreen) {
                    minecraft.gui.setScreen(null);
                }
                return;
            }
            if (snapshot.mode() == FieldUiSnapshot.Mode.LOADING) {
                if (!(minecraft.gui.screen() instanceof WorldLoadingScreen)) minecraft.gui.setScreen(new WorldLoadingScreen());
                return;
            }
            if (minecraft.gui.screen() instanceof WorldLoadingScreen) minecraft.gui.setScreen(null);

            // QUEST/NONE/RESULT are passive state: QuestGuideLayer shows the current objective without stealing input.
            if (snapshot.mode() != FieldUiSnapshot.Mode.TRAVEL) {
                if (minecraft.gui.screen() instanceof FieldPanelScreen) minecraft.gui.setScreen(null);
                return;
            }
            if (!(minecraft.gui.screen() instanceof BattleScreen) && !(minecraft.gui.screen() instanceof BattleResultScreen)) {
                minecraft.gui.setScreen(new FieldPanelScreen(FieldUiSnapshot.Mode.TRAVEL));
            }
        });
    }
}
