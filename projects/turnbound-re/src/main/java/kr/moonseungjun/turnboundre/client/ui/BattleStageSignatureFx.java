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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Adds short, semantic accents for authored action shapes without inventing target legality or damage.
 * The base projectile/impact layer remains in BattleStageHud; this layer only differentiates strong action families.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleStageSignatureFx {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_signature_fx");

    private BattleStageSignatureFx() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, BattleStageSignatureFx::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !UiLayoutMetrics.supportsBattleHud(graphics.guiWidth(), graphics.guiHeight())) return;
        BattleActionTimelineState.Cue cue = BattleActionTimelineState.cue(model.battleId()).orElse(null);
        if (cue == null || cue.presentationStyle() == BattleActionTimelineState.PresentationStyle.STANDARD) return;

        UiLayoutMetrics.Rect viewport = UiLayoutMetrics
                .battleHud(graphics.guiWidth(), graphics.guiHeight())
                .reservedWorldViewport();
        BattleStageLayout.Layout layout = BattleStageLayout.arrange(
                viewport, model.playerParty().size(), model.enemies().size());
        int lineHeight = Minecraft.getInstance().font.lineHeight;

        StageParticipant actor = participant(layout, model, cue.actorId());
        if (actor == null || actor.participant().entityId() != null) return;
        UiLayoutMetrics.Rect actorModel = BattleStageActionFx
                .modelBounds(actor.slot().bounds(), actor.enemy(), lineHeight)
                .orElse(null);
        if (actorModel == null) return;

        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        if (cue.presentationStyle() == BattleActionTimelineState.PresentationStyle.VOLLEY
                && cue.phase() == BattleActionTimelineState.Phase.WINDUP) {
            renderVolley(graphics, model, layout, actorModel, cue, lineHeight);
        } else if (cue.presentationStyle() == BattleActionTimelineState.PresentationStyle.RIFT
                && cue.phase() == BattleActionTimelineState.Phase.WINDUP) {
            renderRiftTravel(graphics, model, layout, actorModel, cue, lineHeight);
        }

        if (accentVisible(cue)) {
            UiVisualLanguage.FrameState frameState = accentState(cue.presentationStyle());
            for (String targetId : cue.targetIds()) {
                StageParticipant target = participant(layout, model, targetId);
                if (target == null || target.participant().entityId() != null) continue;
                UiLayoutMetrics.Rect targetModel = BattleStageActionFx
                        .modelBounds(target.slot().bounds(), target.enemy(), lineHeight)
                        .orElse(null);
                if (targetModel == null) continue;
                renderAccent(graphics, targetModel, cue.presentationStyle(), frameState);
            }
        }
        graphics.disableScissor();
    }

    private static void renderVolley(
            GuiGraphicsExtractor graphics,
            BattlePresentationModel model,
            BattleStageLayout.Layout layout,
            UiLayoutMetrics.Rect actorModel,
            BattleActionTimelineState.Cue cue,
            int lineHeight
    ) {
        ItemStack projectile = projectileItem(cue.impactStyle());
        if (projectile.isEmpty()) return;

        BattleStageActionFx.Point from = BattleStageActionFx.center(actorModel);
        for (String targetId : cue.targetIds()) {
            StageParticipant target = participant(layout, model, targetId);
            if (target == null || target.participant().entityId() != null) continue;
            UiLayoutMetrics.Rect targetModel = BattleStageActionFx
                    .modelBounds(target.slot().bounds(), target.enemy(), lineHeight)
                    .orElse(null);
            if (targetModel == null) continue;
            BattleStageActionFx.Point to = BattleStageActionFx.center(targetModel);

            for (int ordinal = 1; ordinal <= 2; ordinal++) {
                double progress = extraProjectileProgress(cue.phaseProgress(), ordinal);
                if (progress <= 0.0D) continue;
                BattleStageActionFx.Point point = BattleStageActionFx.travel(from, to, progress);
                int side = ordinal == 1 ? -5 : 5;
                graphics.item(projectile, point.x() - 8 + side, point.y() - 8);
            }
        }
    }

    private static ItemStack projectileItem(BattleActionTimelineState.ImpactStyle style) {
        return switch (style) {
            case FIRE -> new ItemStack(Items.FIRE_CHARGE);
            case PROJECTILE -> new ItemStack(Items.ARROW);
            default -> ItemStack.EMPTY;
        };
    }

    private static void renderRiftTravel(
            GuiGraphicsExtractor graphics,
            BattlePresentationModel model,
            BattleStageLayout.Layout layout,
            UiLayoutMetrics.Rect actorModel,
            BattleActionTimelineState.Cue cue,
            int lineHeight
    ) {
        ItemStack riftProjectile = new ItemStack(Items.ENDER_PEARL);
        BattleStageActionFx.Point from = BattleStageActionFx.center(actorModel);
        for (String targetId : cue.targetIds()) {
            StageParticipant target = participant(layout, model, targetId);
            if (target == null || target.participant().entityId() != null) continue;
            UiLayoutMetrics.Rect targetModel = BattleStageActionFx
                    .modelBounds(target.slot().bounds(), target.enemy(), lineHeight)
                    .orElse(null);
            if (targetModel == null) continue;
            BattleStageActionFx.Point to = BattleStageActionFx.center(targetModel);
            BattleStageActionFx.Point point = BattleStageActionFx.travel(from, to, cue.phaseProgress());
            graphics.item(riftProjectile,
                    point.x() - 8 + riftSideOffset(cue.phaseProgress()),
                    point.y() - 8);
        }
    }

    private static void renderAccent(
            GuiGraphicsExtractor graphics,
            UiLayoutMetrics.Rect bounds,
            BattleActionTimelineState.PresentationStyle style,
            UiVisualLanguage.FrameState state
    ) {
        UiVisualLanguage.frame(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), state);
        if (style == BattleActionTimelineState.PresentationStyle.RIFT && bounds.width() > 8 && bounds.height() > 8) {
            UiVisualLanguage.frame(graphics,
                    bounds.x() + 3, bounds.y() + 3,
                    bounds.width() - 6, bounds.height() - 6,
                    UiVisualLanguage.FrameState.FOCUS);
        }
    }

    static double extraProjectileProgress(double baseProgress, int ordinal) {
        if (!Double.isFinite(baseProgress) || ordinal <= 0) return 0.0D;
        double delay = Math.min(0.48D, ordinal * 0.14D);
        if (baseProgress <= delay) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, (baseProgress - delay) / (1.0D - delay)));
    }

    static int riftSideOffset(double progress) {
        double p = Double.isFinite(progress) ? Math.max(0.0D, Math.min(1.0D, progress)) : 0.0D;
        return (int) Math.round(Math.sin(p * Math.PI) * 7.0D);
    }

    static boolean accentVisible(BattleActionTimelineState.Cue cue) {
        return cue != null
                && cue.presentationStyle() != BattleActionTimelineState.PresentationStyle.STANDARD
                && cue.phase() == BattleActionTimelineState.Phase.IMPACT
                && cue.phaseProgress() < 0.62D;
    }

    static UiVisualLanguage.FrameState accentState(BattleActionTimelineState.PresentationStyle style) {
        if (style == null) return UiVisualLanguage.FrameState.FOCUS;
        return switch (style) {
            case RITUAL -> UiVisualLanguage.FrameState.SUCCESS;
            case HEAVY, SLAM, AREA, RIFT -> UiVisualLanguage.FrameState.WARNING;
            case VOLLEY, STANDARD -> UiVisualLanguage.FrameState.FOCUS;
        };
    }

    private static StageParticipant participant(
            BattleStageLayout.Layout layout,
            BattlePresentationModel model,
            String participantId
    ) {
        StageParticipant enemy = find(layout.enemies(), model.enemies(), participantId, true);
        if (enemy != null) return enemy;
        return find(layout.players(), model.playerParty(), participantId, false);
    }

    private static StageParticipant find(
            List<BattleStageLayout.Slot> slots,
            List<BattleNetworkPayloads.SnapshotParticipant> participants,
            String participantId,
            boolean enemy
    ) {
        for (BattleStageLayout.Slot slot : slots) {
            if (slot.participantIndex() >= participants.size()) continue;
            BattleNetworkPayloads.SnapshotParticipant participant = participants.get(slot.participantIndex());
            if (participant.id().equals(participantId)) return new StageParticipant(slot, participant, enemy);
        }
        return null;
    }

    private record StageParticipant(
            BattleStageLayout.Slot slot,
            BattleNetworkPayloads.SnapshotParticipant participant,
            boolean enemy
    ) {}
}
