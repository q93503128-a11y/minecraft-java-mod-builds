package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;
import java.util.Optional;

/**
 * Lightweight actor emphasis for the authoritative action timeline.
 * Impact on targets remains driven by snapshot deltas in BattleStageFeedbackState.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleActionTimelineHud {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_action_timeline");

    private BattleActionTimelineHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, BattleActionTimelineHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !UiLayoutMetrics.supportsBattleHud(graphics.guiWidth(), graphics.guiHeight())) return;
        BattleActionTimelineState.Cue cue = BattleActionTimelineState.cue(model.battleId()).orElse(null);
        if (cue == null) return;

        UiLayoutMetrics.Rect viewport = UiLayoutMetrics
                .battleHud(graphics.guiWidth(), graphics.guiHeight())
                .reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(
                viewport, model.playerParty().size(), model.enemies().size());
        int lineHeight = Minecraft.getInstance().font.lineHeight;

        ActorSlot actor = actorSlot(layout, model, cue.actorId()).orElse(null);
        if (actor == null || actor.participant().entityId() != null || !actor.participant().alive()) return;
        UiLayoutMetrics.Rect bounds = actionBounds(actor.slot().bounds(), actor.enemy(), lineHeight, cue.phase())
                .orElse(null);
        if (bounds == null) return;

        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        UiVisualLanguage.frame(
                graphics,
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                frameState(cue.phase()));
        graphics.disableScissor();
    }

    static UiVisualLanguage.FrameState frameState(BattleActionTimelineState.Phase phase) {
        if (phase == null) throw new IllegalArgumentException("phase required");
        return switch (phase) {
            case WINDUP, IMPACT -> UiVisualLanguage.FrameState.FOCUS;
            case RECOVERY -> UiVisualLanguage.FrameState.IDLE;
        };
    }

    static Optional<UiLayoutMetrics.Rect> actionBounds(
            UiLayoutMetrics.Rect slot,
            boolean enemy,
            int lineHeight,
            BattleActionTimelineState.Phase phase
    ) {
        if (slot == null || phase == null || lineHeight <= 0) return Optional.empty();
        int modelTop = slot.y() + (enemy ? 30 : 11);
        int modelBottom = slot.bottom() - lineHeight;
        int inset = switch (phase) {
            case WINDUP -> UiLayoutMetrics.SPACE_2;
            case IMPACT -> 0;
            case RECOVERY -> 1;
        };
        int x = slot.x() + inset;
        int y = modelTop + inset;
        int width = slot.width() - inset * 2;
        int height = modelBottom - modelTop - inset * 2;
        if (width <= 2 || height <= 2) return Optional.empty();
        return Optional.of(new UiLayoutMetrics.Rect(x, y, width, height));
    }

    private static Optional<ActorSlot> actorSlot(
            BattleStageLayout.Layout layout,
            BattlePresentationModel model,
            String actorId
    ) {
        ActorSlot enemy = find(layout.enemies(), model.enemies(), actorId, true);
        if (enemy != null) return Optional.of(enemy);
        return Optional.ofNullable(find(layout.players(), model.playerParty(), actorId, false));
    }

    private static ActorSlot find(
            List<BattleStageLayout.Slot> slots,
            List<BattleNetworkPayloads.SnapshotParticipant> participants,
            String actorId,
            boolean enemy
    ) {
        for (BattleStageLayout.Slot slot : slots) {
            if (slot.participantIndex() >= participants.size()) continue;
            BattleNetworkPayloads.SnapshotParticipant participant = participants.get(slot.participantIndex());
            if (participant.id().equals(actorId)) return new ActorSlot(slot, participant, enemy);
        }
        return null;
    }

    private record ActorSlot(
            BattleStageLayout.Slot slot,
            BattleNetworkPayloads.SnapshotParticipant participant,
            boolean enemy
    ) {}
}
