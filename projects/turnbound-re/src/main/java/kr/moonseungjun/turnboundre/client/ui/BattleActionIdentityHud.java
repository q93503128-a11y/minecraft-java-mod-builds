package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.BattleTargetMarkerState;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Adds source-backed action identity without changing battle facts or legality.
 * Icons are Mojang runtime items selected by BattleActionRuntimeVisuals; unknown actions render no icon.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleActionIdentityHud {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_action_identity");

    private BattleActionIdentityHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, BattleActionIdentityHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !UiLayoutMetrics.supportsBattleHud(graphics.guiWidth(), graphics.guiHeight())) return;

        BattleActionTimelineState.Cue cue = BattleActionTimelineState.cue(model.battleId()).orElse(null);
        if (cue != null) {
            renderActiveCue(graphics, model, cue);
            return;
        }

        if (Minecraft.getInstance().gui.screen() instanceof BattleCommandScreen
                && model.awaitingPlayerCommand()
                && !BattleTargetMarkerState.isPublishedFor(model.battleId(), model.revision())) {
            renderActionPicker(graphics, model.availableActions());
        }
    }

    private static void renderActiveCue(
            GuiGraphicsExtractor graphics,
            BattlePresentationModel model,
            BattleActionTimelineState.Cue cue
    ) {
        ItemStack icon = BattleActionRuntimeVisuals.icon(cue.actionId());
        if (icon.isEmpty()) return;

        UiLayoutMetrics.Rect viewport = UiLayoutMetrics
                .battleHud(graphics.guiWidth(), graphics.guiHeight())
                .reservedWorldViewport();
        int x = viewport.x() + viewport.width() / 2 - 9;
        int y = viewport.y() + UiLayoutMetrics.SPACE_2;
        UiVisualLanguage.FrameState state = switch (cue.phase()) {
            case WINDUP, IMPACT -> UiVisualLanguage.FrameState.FOCUS;
            case RECOVERY -> UiVisualLanguage.FrameState.IDLE;
        };

        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        UiVisualLanguage.frame(graphics, x, y, 18, 18, state);
        graphics.item(icon, x + 1, y + 1);
        graphics.disableScissor();
    }

    private static void renderActionPicker(
            GuiGraphicsExtractor graphics,
            List<BattleNetworkPayloads.SnapshotAction> actions
    ) {
        if (actions.isEmpty()) return;

        UiLayoutMetrics.Rect command = UiLayoutMetrics
                .battleHud(graphics.guiWidth(), graphics.guiHeight())
                .commandStrip();
        int gap = UiLayoutMetrics.SPACE_2;
        int totalGap = gap * Math.max(0, actions.size() - 1);
        int cell = Math.max(1, (command.width() - totalGap) / actions.size());
        int totalWidth = cell * actions.size() + totalGap;
        int x = command.x() + Math.max(0, (command.width() - totalWidth) / 2);
        int buttonY = command.bottom() - 20;
        int iconY = Math.max(command.y() + 17, buttonY - 18);

        graphics.enableScissor(command.x(), command.y(), command.right(), command.bottom());
        for (BattleNetworkPayloads.SnapshotAction action : actions) {
            ItemStack icon = BattleActionRuntimeVisuals.icon(action.id());
            if (!icon.isEmpty()) {
                int iconX = x + cell / 2 - 9;
                UiVisualLanguage.frame(
                        graphics,
                        iconX,
                        iconY,
                        18,
                        18,
                        action.usable() ? UiVisualLanguage.FrameState.IDLE : UiVisualLanguage.FrameState.DISABLED);
                graphics.item(icon, iconX + 1, iconY + 1);
            }
            x += cell + gap;
        }
        graphics.disableScissor();
    }
}
