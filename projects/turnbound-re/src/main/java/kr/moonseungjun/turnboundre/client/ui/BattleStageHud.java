package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.BattleStageFeedbackState;
import kr.moonseungjun.turnboundre.client.BattleTargetMarkerState;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Renders logical/virtual battle participants as real Minecraft entity models in the reserved world viewport.
 * Participants already bound to live world entities are deliberately skipped to avoid duplicate presentation.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleStageHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_stage");
    private static final EntityCache ENTITY_CACHE = new EntityCache();

    private BattleStageHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, BattleStageHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null) {
            ENTITY_CACHE.clear();
            return;
        }
        if (!UiLayoutMetrics.supportsBattleHud(graphics.guiWidth(), graphics.guiHeight())) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ENTITY_CACHE.clear();
            return;
        }

        UiLayoutMetrics.Rect viewport = UiLayoutMetrics
                .battleHud(graphics.guiWidth(), graphics.guiHeight())
                .reservedWorldViewport();
        BattleStageLayout.Layout stage = BattleStageLayout.arrange(
                viewport, model.playerParty().size(), model.enemies().size());
        Font font = minecraft.font;

        ENTITY_CACHE.begin(minecraft.level, model.battleId());
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        renderSide(graphics, font, minecraft, model, stage.enemies(), model.enemies(), true);
        renderSide(graphics, font, minecraft, model, stage.players(), model.playerParty(), false);
        graphics.disableScissor();
    }

    private static void renderSide(
            GuiGraphicsExtractor graphics,
            Font font,
            Minecraft minecraft,
            BattlePresentationModel model,
            List<BattleStageLayout.Slot> slots,
            List<BattleNetworkPayloads.SnapshotParticipant> participants,
            boolean enemy
    ) {
        for (BattleStageLayout.Slot slot : slots) {
            if (slot.participantIndex() >= participants.size()) continue;
            BattleNetworkPayloads.SnapshotParticipant participant = participants.get(slot.participantIndex());
            if (participant.entityId() != null) continue;
            renderParticipant(graphics, font, minecraft, model, slot.bounds(), participant, enemy);
        }
    }

    private static void renderParticipant(
            GuiGraphicsExtractor graphics,
            Font font,
            Minecraft minecraft,
            BattlePresentationModel model,
            UiLayoutMetrics.Rect slot,
            BattleNetworkPayloads.SnapshotParticipant participant,
            boolean enemy
    ) {
        boolean current = participant.id().equals(model.currentActorId());
        BattleTargetMarkerState.MarkerKind targetMarker = BattleTargetMarkerState.markerForParticipant(
                model.battleId(), model.revision(), participant.id()).orElse(null);
        int markerOrdinal = BattleTargetMarkerState.markerOrdinalForParticipant(
                model.battleId(), model.revision(), participant.id()).orElse(0);
        BattleStageFeedbackState.Cue feedback = BattleStageFeedbackState
                .cue(model.battleId(), participant.id()).orElse(null);

        boolean freshDefeat = feedback != null && feedback.defeatStrength() > 0.0D;
        int textColor = participant.alive()
                ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                : (freshDefeat ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_DISABLED);
        String name = (current ? "> " : "") + displayName(participant);
        graphics.text(font, Component.literal(fit(font, name, slot.width())),
                slot.x(), slot.y(), textColor, true);

        int modelTop;
        if (enemy) {
            int barWidth = Math.max(8, slot.width() - UiLayoutMetrics.SPACE_4);
            UiVisualLanguage.meter(graphics, slot.x() + UiLayoutMetrics.SPACE_2, slot.y() + 10,
                    barWidth, 3, participant.hp(), participant.maxHp(), UiVisualLanguage.HP_PROGRESS);
            UiVisualLanguage.meter(graphics, slot.x() + UiLayoutMetrics.SPACE_2, slot.y() + 15,
                    barWidth, 3, participant.poise(), participant.poiseMax(), UiVisualLanguage.POISE_PROGRESS);
            String intent = compactIntent(participant);
            graphics.text(font, Component.literal(fit(font, intent, slot.width())),
                    slot.x(), slot.y() + 20,
                    participant.intent() == null ? UiVisualLanguage.TEXT_SECONDARY : UiVisualLanguage.TEXT_WARNING,
                    true);
            modelTop = slot.y() + 30;
        } else {
            modelTop = slot.y() + 11;
        }

        int markerRows = targetMarker != null || current || participant.exposed() ? font.lineHeight : 0;
        int modelBottom = Math.max(modelTop + 1, slot.bottom() - markerRows);
        if (participant.alive()) {
            LivingEntity visual = ENTITY_CACHE.resolve(minecraft, participant);
            if (visual != null) {
                int shakeX = feedbackShakeX(feedback);
                int recoilY = feedbackRecoilY(feedback, enemy);
                renderEntity(graphics, slot.x() + shakeX, modelTop + recoilY,
                        slot.width(), modelBottom - modelTop, visual);
            }
        }
        renderHpFeedback(graphics, font, slot, modelTop, feedback);

        String stateLine = stageStateLine(participant, current, targetMarker, markerOrdinal);
        if (!stateLine.isBlank()) {
            int stateColor = markerColor(targetMarker, current, participant.exposed(), participant.alive());
            graphics.text(font, Component.literal(fit(font, stateLine, slot.width())),
                    slot.x(), slot.bottom() - font.lineHeight, stateColor, true);
        }
    }

    private static int feedbackShakeX(BattleStageFeedbackState.Cue feedback) {
        if (feedback == null || feedback.impactStrength() <= 0.0D) return 0;
        int amplitude = Math.max(1, (int) Math.ceil(feedback.impactStrength() * 3.0D));
        boolean positive = ((System.nanoTime() / 42_000_000L) & 1L) == 0L;
        return positive ? amplitude : -amplitude;
    }

    private static int feedbackRecoilY(BattleStageFeedbackState.Cue feedback, boolean enemy) {
        if (feedback == null || feedback.impactStrength() <= 0.0D) return 0;
        int amplitude = Math.max(1, (int) Math.ceil(feedback.impactStrength() * 2.0D));
        return enemy ? -amplitude : amplitude;
    }

    private static void renderHpFeedback(
            GuiGraphicsExtractor graphics,
            Font font,
            UiLayoutMetrics.Rect slot,
            int modelTop,
            BattleStageFeedbackState.Cue feedback
    ) {
        if (feedback == null || feedback.hpStrength() <= 0.0D || feedback.hpDelta() == 0) return;
        int delta = feedback.hpDelta();
        String text = (delta > 0 ? "+" : "−") + Math.abs(delta);
        int color = delta > 0 ? UiVisualLanguage.TEXT_SUCCESS : UiVisualLanguage.TEXT_WARNING;
        int rise = (int) Math.round((1.0D - feedback.hpStrength()) * 6.0D);
        int x = Math.max(slot.x(), slot.right() - font.width(text));
        int y = Math.max(slot.y() + font.lineHeight, modelTop + 2 - rise);
        graphics.text(font, Component.literal(text), x, y, color, true);
    }

    private static void renderEntity(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            LivingEntity entity
    ) {
        if (width < 8 || height < 8) return;
        float entityWidth = entity.getBbWidth();
        float entityHeight = entity.getBbHeight();
        if (!Float.isFinite(entityWidth) || !Float.isFinite(entityHeight)
                || entityWidth <= 0.0F || entityHeight <= 0.0F) return;

        int innerWidth = Math.max(1, width - UiLayoutMetrics.SPACE_8);
        int innerHeight = Math.max(1, height - UiLayoutMetrics.SPACE_4);
        int scale = (int) Math.floor(Math.min(innerWidth / entityWidth, innerHeight / entityHeight));
        scale = Math.max(1, Math.min(48, scale));

        int x0 = x + UiLayoutMetrics.SPACE_2;
        int y0 = y;
        int x1 = x + width - UiLayoutMetrics.SPACE_2;
        int y1 = y + height;
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics, x0, y0, x1, y1, scale, 0.0F, 0.0F, 0.35F, entity);
    }

    private static String compactIntent(BattleNetworkPayloads.SnapshotParticipant participant) {
        if (!participant.alive()) return Component.translatable("hud.turnbound_re.state.defeated").getString();
        BattleNetworkPayloads.SnapshotIntent intent = participant.intent();
        if (intent == null) return Component.translatable("hud.turnbound_re.intent.none").getString();
        String risk = Component.translatable(BattleHudPresentation.intentRiskKey(intent.risk())).getString();
        return risk + " · " + humanizeId(intent.actionId());
    }

    private static String stageStateLine(
            BattleNetworkPayloads.SnapshotParticipant participant,
            boolean current,
            BattleTargetMarkerState.MarkerKind marker,
            int ordinal
    ) {
        if (!participant.alive()) return Component.translatable("hud.turnbound_re.state.defeated").getString();
        if (marker != null) {
            String key = switch (marker) {
                case ELIGIBLE -> "marker.turnbound_re.target.eligible";
                case HOVERED -> "marker.turnbound_re.target.hovered";
                case SELECTED -> "marker.turnbound_re.target.selected";
            };
            String prefix = ordinal > 0 ? "#" + ordinal + " " : "";
            return prefix + Component.translatable(key).getString();
        }
        if (participant.exposed()) return BattleHudPresentation.exposedLabel().getString();
        return current ? BattleHudPresentation.currentTurnLabel().getString() : "";
    }

    private static int markerColor(
            BattleTargetMarkerState.MarkerKind marker,
            boolean current,
            boolean exposed,
            boolean alive
    ) {
        if (!alive) return UiVisualLanguage.TEXT_DISABLED;
        if (marker == BattleTargetMarkerState.MarkerKind.SELECTED) return UiVisualLanguage.TEXT_SUCCESS;
        if (marker == BattleTargetMarkerState.MarkerKind.HOVERED) return UiVisualLanguage.TEXT_FOCUS;
        if (marker == BattleTargetMarkerState.MarkerKind.ELIGIBLE || exposed) return UiVisualLanguage.TEXT_WARNING;
        return current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_SECONDARY;
    }

    private static String displayName(BattleNetworkPayloads.SnapshotParticipant participant) {
        String source = participant.characterId().isBlank() ? participant.id() : participant.characterId();
        return humanizeId(source);
    }

    private static String humanizeId(String id) {
        if (id == null || id.isBlank()) return "?";
        int colon = id.indexOf(':');
        String raw = colon >= 0 ? id.substring(colon + 1) : id;
        String[] words = raw.replace('-', '_').split("_+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }

    private static final class EntityCache {
        private final Map<String, CachedVisual> visuals = new HashMap<>();
        private ClientLevel level;
        private UUID battleId;

        void begin(ClientLevel nextLevel, UUID nextBattleId) {
            if (nextLevel != level || !nextBattleId.equals(battleId)) {
                clear();
                level = nextLevel;
                battleId = nextBattleId;
            }
        }

        LivingEntity resolve(Minecraft minecraft, BattleNetworkPayloads.SnapshotParticipant participant) {
            if (minecraft.level == null || participant.characterId().isBlank()) return null;
            String sourceEntity = ProgressionClientState.sourceEntity(participant.characterId()).orElse("");
            if (sourceEntity.isBlank()) {
                visuals.remove(participant.id());
                return null;
            }

            CachedVisual cached = visuals.get(participant.id());
            if (cached != null && sourceEntity.equals(cached.sourceEntity())) return cached.entity();

            Identifier id = Identifier.tryParse(sourceEntity);
            if (id == null) return null;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) return null;
            Entity created = type.create(minecraft.level, EntitySpawnReason.COMMAND);
            if (!(created instanceof LivingEntity living)) return null;

            visuals.put(participant.id(), new CachedVisual(sourceEntity, living));
            return living;
        }

        void clear() {
            visuals.clear();
            level = null;
            battleId = null;
        }
    }

    private record CachedVisual(String sourceEntity, LivingEntity entity) {}
}
