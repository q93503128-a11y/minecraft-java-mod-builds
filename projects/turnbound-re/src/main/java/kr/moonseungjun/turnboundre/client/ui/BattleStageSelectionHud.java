package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.BattleTargetMarkerState;
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
 * Selection-only overlay for logical battle participants rendered in the reserved world viewport.
 * It never decides target legality: frames are driven exclusively by BattleTargetMarkerState,
 * which is published from the current server-authored eligible target set.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleStageSelectionHud {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_stage_selection");

    private BattleStageSelectionHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, BattleStageSelectionHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !BattleTargetMarkerState.isPublishedFor(model.battleId(), model.revision())) return;
        if (!UiLayoutMetrics.supportsBattleHud(graphics.guiWidth(), graphics.guiHeight())) return;

        UiLayoutMetrics.Rect viewport = UiLayoutMetrics
                .battleHud(graphics.guiWidth(), graphics.guiHeight())
                .reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(
                viewport, model.playerParty().size(), model.enemies().size());
        int lineHeight = Minecraft.getInstance().font.lineHeight;

        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        renderSide(graphics, model, layout.enemies(), model.enemies(), true, lineHeight);
        renderSide(graphics, model, layout.players(), model.playerParty(), false, lineHeight);
        graphics.disableScissor();
    }

    private static void renderSide(
            GuiGraphicsExtractor graphics,
            BattlePresentationModel model,
            List<BattleStageLayout.Slot> slots,
            List<BattleNetworkPayloads.SnapshotParticipant> participants,
            boolean enemy,
            int lineHeight
    ) {
        for (BattleStageLayout.Slot slot : slots) {
            if (slot.participantIndex() >= participants.size()) continue;
            BattleNetworkPayloads.SnapshotParticipant participant = participants.get(slot.participantIndex());
            if (participant.entityId() != null || !participant.alive()) continue;

            BattleTargetMarkerState.MarkerKind marker = BattleTargetMarkerState.markerForParticipant(
                    model.battleId(), model.revision(), participant.id()).orElse(null);
            if (marker == null) continue;

            selectionBounds(slot.bounds(), enemy, lineHeight, marker).ifPresent(bounds ->
                    UiVisualLanguage.frame(
                            graphics,
                            bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                            frameState(marker)));
        }
    }

    static UiVisualLanguage.FrameState frameState(BattleTargetMarkerState.MarkerKind marker) {
        if (marker == null) throw new IllegalArgumentException("marker required");
        return switch (marker) {
            case ELIGIBLE -> UiVisualLanguage.FrameState.WARNING;
            case HOVERED -> UiVisualLanguage.FrameState.FOCUS;
            case SELECTED -> UiVisualLanguage.FrameState.SUCCESS;
        };
    }

    static Optional<UiLayoutMetrics.Rect> selectionBounds(
            UiLayoutMetrics.Rect slot,
            boolean enemy,
            int lineHeight,
            BattleTargetMarkerState.MarkerKind marker
    ) {
        if (slot == null || marker == null || lineHeight <= 0) return Optional.empty();

        int modelTop = slot.y() + (enemy ? 30 : 11);
        int modelBottom = slot.bottom() - lineHeight;
        int inset = switch (marker) {
            case ELIGIBLE -> UiLayoutMetrics.SPACE_2;
            case HOVERED -> 1;
            case SELECTED -> 0;
        };

        int x = slot.x() + inset;
        int y = modelTop + inset;
        int width = slot.width() - inset * 2;
        int height = modelBottom - modelTop - inset * 2;
        if (width <= 2 || height <= 2) return Optional.empty();
        return Optional.of(new UiLayoutMetrics.Rect(x, y, width, height));
    }
}
