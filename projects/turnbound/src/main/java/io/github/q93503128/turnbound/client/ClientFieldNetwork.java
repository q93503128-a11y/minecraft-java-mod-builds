package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.FastTravelTransitionPayload;
import io.github.q93503128.turnbound.network.FieldSnapshotPayload;
import io.github.q93503128.turnbound.network.NpcDialoguePayload;
import io.github.q93503128.turnbound.network.QuestTargetOutlinePayload;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientFieldNetwork {
    private ClientFieldNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(FieldSnapshotPayload.TYPE, ClientFieldNetwork::handle);
        event.register(NpcDialoguePayload.TYPE, ClientFieldNetwork::handleDialogue);
        event.register(QuestTargetOutlinePayload.TYPE, ClientFieldNetwork::handleOutline);
        event.register(FastTravelTransitionPayload.TYPE, ClientFieldNetwork::handleFastTravelTransition);
    }

    private static void handleOutline(QuestTargetOutlinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestTargetOutlineState.update(payload.entityIds()));
    }

    private static void handleFastTravelTransition(FastTravelTransitionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FastTravelTransitionLayer.accept(payload.transition()));
    }

    private static void handleDialogue(NpcDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof BattleScreen
                    || minecraft.gui.screen() instanceof BattleResultScreen
                    || minecraft.gui.screen() instanceof GachaPresentationScreen) return;
            minecraft.gui.setScreen(NpcDialogueScreen.decode(payload.dialogue()));
        });
    }

    private static void handle(FieldSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientFieldState.update(payload.snapshot());
            ClientPresentationTransition.onFieldSnapshot(ClientFieldState.snapshot());
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
            if (snapshot.mode() == FieldUiSnapshot.Mode.BATTLE_TRANSITION) {
                if (minecraft.gui.screen() instanceof FieldPanelScreen
                        || minecraft.gui.screen() instanceof DrehmalWorldMapScreen
                        || minecraft.gui.screen() instanceof MetaMenuScreen
                        || minecraft.gui.screen() instanceof WorldLoadingScreen) {
                    minecraft.gui.setScreen(null);
                }
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
