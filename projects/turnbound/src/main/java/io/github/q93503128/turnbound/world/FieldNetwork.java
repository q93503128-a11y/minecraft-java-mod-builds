package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import io.github.q93503128.turnbound.network.FastTravelTransitionPayload;
import io.github.q93503128.turnbound.network.FieldSnapshotPayload;
import io.github.q93503128.turnbound.network.NpcDialoguePayload;
import io.github.q93503128.turnbound.network.QuestTargetOutlinePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FieldNetwork {
    public static final String PROTOCOL = "turnbound-field-alpha12";
    public record DialogueChoice(String label, String command) {}

    private FieldNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(FieldSnapshotPayload.TYPE, FieldSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(NpcDialoguePayload.TYPE, NpcDialoguePayload.STREAM_CODEC);
        registrar.playToClient(QuestTargetOutlinePayload.TYPE, QuestTargetOutlinePayload.STREAM_CODEC);
        registrar.playToClient(FastTravelTransitionPayload.TYPE, FastTravelTransitionPayload.STREAM_CODEC);
        registrar.playToServer(
                FieldCommandPayload.TYPE,
                FieldCommandPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        FieldCommandRouter.command(player, payload.command());
                    }
                }));
    }

    public static void sync(ServerPlayer player, FieldUiSnapshot snapshot) {
        FieldUiSnapshot projected = AsterMarchFastTravelService.project(player, snapshot);
        PacketDistributor.sendToPlayer(player, new FieldSnapshotPayload(FieldUiCodec.encode(projected)));
        PacketDistributor.sendToPlayer(player, new QuestTargetOutlinePayload(""));
    }

    /** External authored-world state must not receive retired Aster March coordinate projections. */
    public static void syncExternal(ServerPlayer player, FieldUiSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new FieldSnapshotPayload(FieldUiCodec.encode(snapshot)));
        PacketDistributor.sendToPlayer(player,
                new QuestTargetOutlinePayload(QuestTargetGlowService.targetEntityIds(player, snapshot)));
    }

    static void fastTravelTransition(ServerPlayer player, String phase, String label) {
        if (player == null) return;
        String cleanPhase = phase == null ? "" : phase.replace('\n', ' ').replace('\r', ' ').trim();
        String cleanLabel = label == null ? "" : label.replace('\n', ' ').replace('\r', ' ').trim();
        PacketDistributor.sendToPlayer(player, new FastTravelTransitionPayload(cleanPhase + "\n" + cleanLabel));
    }

    /** Short ownership handoff: field HUD yields before the first battle snapshot arrives. */
    public static void suspendForBattle(ServerPlayer player) {
        syncExternal(player, FieldUiSnapshot.battleTransition());
    }

    public static void showDialogue(ServerPlayer player, String speaker, String dialogue) {
        showDialogueChoices(player, speaker, dialogue, java.util.List.of());
    }

    public static void showDialogueChoices(ServerPlayer player, String speaker, String dialogue, java.util.List<DialogueChoice> choices) {
        if (player == null) return;
        String cleanSpeaker = speaker == null ? "" : speaker.replace('\n', ' ').replace('\r', ' ');
        String cleanDialogue = dialogue == null ? "" : dialogue.replace('\r', ' ');
        StringBuilder encoded = new StringBuilder(cleanSpeaker).append('\n').append(cleanDialogue);
        if (choices != null) {
            int count = 0;
            for (DialogueChoice choice : choices) {
                if (choice == null || choice.label() == null || choice.label().isBlank() || count++ >= 3) continue;
                String label = choice.label().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
                String command = choice.command() == null ? "" : choice.command().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
                encoded.append("\n@@TURNBOUND_CHOICE@@").append(label).append('\t').append(command);
            }
        }
        PacketDistributor.sendToPlayer(player, new NpcDialoguePayload(encoded.toString()));
    }

    public static void close(ServerPlayer player) {
        syncExternal(player, FieldUiSnapshot.inactive());
    }
}
