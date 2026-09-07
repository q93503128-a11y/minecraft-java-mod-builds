package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattleCommandOverlayState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.input.BattleInputHandler;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * First production battle HUD: a Minecraft-native tactical overlay over authoritative S2C presentation data.
 * It deliberately leaves the center world viewport unobstructed and owns no combat calculations.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_hud");

    private static final Identifier FRAME_IDLE = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    private static final Identifier FRAME_ACTIVE = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    private static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
    private static final Identifier BAR_BACKGROUND = Identifier.withDefaultNamespace("boss_bar/white_background");
    private static final Identifier HP_PROGRESS = Identifier.withDefaultNamespace("boss_bar/red_progress");
    private static final Identifier POISE_PROGRESS = Identifier.withDefaultNamespace("boss_bar/yellow_progress");
    private static final Identifier ENERGY_PROGRESS = Identifier.withDefaultNamespace("boss_bar/blue_progress");

    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;

    private BattleHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, BattleHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null) return;
        if (!UiLayoutMetrics.supportsBattleHud(graphics.guiWidth(), graphics.guiHeight())) return;

        UiLayoutMetrics.BattleHudLayout layout = UiLayoutMetrics.battleHud(graphics.guiWidth(), graphics.guiHeight());
        Font font = Minecraft.getInstance().font;
        renderTurnRail(graphics, font, layout.turnRail(), model);
        renderEnemySummary(graphics, font, layout.enemySummary(), model);
        renderPartyStatus(graphics, font, layout.partyStatus(), model);
        if (!BattleCommandOverlayState.isOpen()) {
            renderCommandStrip(graphics, font, layout.commandStrip(), model);
        }
    }

    private static void renderTurnRail(
            GuiGraphicsExtractor graphics,
            Font font,
            UiLayoutMetrics.Rect region,
            BattlePresentationModel model
    ) {
        List<BattleNetworkPayloads.SnapshotParticipant> order = model.turnOrder();
        int rowHeight = 24;
        int visible = Math.min(order.size(), Math.max(1, region.height() / rowHeight));
        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());
        for (int i = 0; i < visible; i++) {
            BattleNetworkPayloads.SnapshotParticipant participant = order.get(i);
            int y = region.y() + i * rowHeight;
            boolean current = participant.id().equals(model.currentActorId());
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, current ? FRAME_ACTIVE : FRAME_IDLE,
                    region.x(), y, 20, 20);
            String teamGlyph = "PLAYER".equals(participant.team()) ? "P" : "E";
            drawCentered(graphics, font, teamGlyph, region.x() + 10, y + 6,
                    participant.alive() ? TEXT_PRIMARY : TEXT_SECONDARY, true);
            String label = fit(font, displayName(participant), region.width() - 24);
            graphics.text(font, Component.literal(label), region.x() + 24, y + 2,
                    participant.alive() ? TEXT_PRIMARY : TEXT_SECONDARY, true);
            String secondary = current ? "NOW" : (participant.exposed() ? "EXPOSED" : resourceLine(participant));
            graphics.text(font, Component.literal(fit(font, secondary, region.width() - 24)),
                    region.x() + 24, y + 11, TEXT_SECONDARY, true);
        }
        if (order.size() > visible) {
            String more = "+" + (order.size() - visible);
            graphics.text(font, Component.literal(more), region.x() + 2, region.bottom() - font.lineHeight,
                    TEXT_SECONDARY, true);
        }
        graphics.disableScissor();
    }

    private static void renderEnemySummary(
            GuiGraphicsExtractor graphics,
            Font font,
            UiLayoutMetrics.Rect region,
            BattlePresentationModel model
    ) {
        BattleNetworkPayloads.SnapshotParticipant enemy = model.focusEnemy().orElse(null);
        if (enemy == null) return;
        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());

        String headline = displayName(enemy) + "  HP " + enemy.hp() + "/" + enemy.maxHp()
                + "  P " + enemy.poise() + "/" + enemy.poiseMax();
        graphics.text(font, Component.literal(fit(font, headline, region.width())), region.x(), region.y(),
                enemy.alive() ? TEXT_PRIMARY : TEXT_SECONDARY, true);
        drawBar(graphics, region.x(), region.y() + 11, region.width(), 5, enemy.hp(), enemy.maxHp(), HP_PROGRESS);
        drawBar(graphics, region.x(), region.y() + 18, region.width(), 4, enemy.poise(), enemy.poiseMax(), POISE_PROGRESS);

        String intent = intentLine(enemy);
        graphics.text(font, Component.literal(fit(font, intent, region.width())), region.x(), region.y() + 24,
                TEXT_PRIMARY, true);
        String status = statusLine(enemy);
        if (!status.isBlank()) {
            graphics.text(font, Component.literal(fit(font, status, region.width())), region.x(), region.y() + 34,
                    TEXT_SECONDARY, true);
        }
        graphics.disableScissor();
    }

    private static void renderPartyStatus(
            GuiGraphicsExtractor graphics,
            Font font,
            UiLayoutMetrics.Rect region,
            BattlePresentationModel model
    ) {
        List<BattleNetworkPayloads.SnapshotParticipant> party = model.playerParty();
        if (party.isEmpty()) return;
        int count = Math.min(4, party.size());
        int rawWidth = region.width() / count;
        int slotWidth = Math.min(180, rawWidth);
        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());

        for (int i = 0; i < count; i++) {
            BattleNetworkPayloads.SnapshotParticipant member = party.get(i);
            int x = region.x() + i * rawWidth;
            boolean current = member.id().equals(model.currentActorId());
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, current ? FRAME_ACTIVE : FRAME_IDLE,
                    x, region.y(), 20, 20);
            drawCentered(graphics, font, Integer.toString(i + 1), x + 10, region.y() + 6,
                    member.alive() ? TEXT_PRIMARY : TEXT_SECONDARY, true);

            int textX = x + 24;
            int contentWidth = Math.max(24, slotWidth - 24 - UiLayoutMetrics.SPACE_4);
            graphics.text(font, Component.literal(fit(font, displayName(member), contentWidth)), textX, region.y() + 1,
                    member.alive() ? TEXT_PRIMARY : TEXT_SECONDARY, true);
            graphics.text(font, Component.literal("HP " + member.hp() + "/" + member.maxHp()), textX, region.y() + 11,
                    TEXT_SECONDARY, true);

            int barWidth = Math.max(24, slotWidth - UiLayoutMetrics.SPACE_4);
            drawBar(graphics, x, region.y() + 24, barWidth, 5, member.hp(), member.maxHp(), HP_PROGRESS);
            drawBar(graphics, x, region.y() + 31, barWidth, 4, member.energy(), 100, ENERGY_PROGRESS);
            graphics.text(font, Component.literal("E " + member.energy() + "/100"), x, region.y() + 38,
                    TEXT_SECONDARY, true);
            String status = statusLine(member);
            if (!status.isBlank() && region.height() >= 58) {
                graphics.text(font, Component.literal(fit(font, status, barWidth)), x, region.y() + 48,
                        TEXT_SECONDARY, true);
            }
        }
        graphics.disableScissor();
    }

    private static void renderCommandStrip(
            GuiGraphicsExtractor graphics,
            Font font,
            UiLayoutMetrics.Rect region,
            BattlePresentationModel model
    ) {
        if (!model.awaitingPlayerCommand()) return;
        BattleNetworkPayloads.SnapshotParticipant actor = model.currentActor().orElse(null);
        if (actor == null) return;

        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX,
                region.x(), region.y(), region.width(), 16);
        String header = Component.translatable(
                "hud.turnbound_re.command_header",
                displayName(actor),
                BattleInputHandler.openKeyName()).getString();
        drawCentered(graphics, font, fit(font, header, region.width() - UiLayoutMetrics.SPACE_8),
                region.x() + region.width() / 2, region.y() + 4, TEXT_PRIMARY, true);

        List<BattleNetworkPayloads.SnapshotAction> actions = model.availableActions();
        if (actions.isEmpty()) {
            String unavailable = Component.translatable("hud.turnbound_re.no_actions").getString();
            graphics.text(font, Component.literal(fit(font, unavailable, region.width() - UiLayoutMetrics.SPACE_8)),
                    region.x() + UiLayoutMetrics.SPACE_4, region.y() + 24, TEXT_SECONDARY, true);
            graphics.disableScissor();
            return;
        }

        int slotCount = actions.size();
        int slotWidth = Math.max(34, region.width() / slotCount);
        for (int i = 0; i < slotCount; i++) {
            BattleNetworkPayloads.SnapshotAction action = actions.get(i);
            int x = region.x() + i * slotWidth;
            int centerX = x + slotWidth / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, action.usable() ? FRAME_ACTIVE : FRAME_IDLE,
                    centerX - 9, region.y() + 18, 18, 18);
            drawCentered(graphics, font, slotGlyph(action.slot()), centerX, region.y() + 23,
                    action.usable() ? TEXT_PRIMARY : TEXT_SECONDARY, true);

            String name = conciseActionName(action.id(), displayName(actor));
            drawCentered(graphics, font, fit(font, name, slotWidth - UiLayoutMetrics.SPACE_2), centerX, region.y() + 38,
                    action.usable() ? TEXT_PRIMARY : TEXT_SECONDARY, true);
            String cost = BattleActionPresentation.hudCost(action).getString();
            drawCentered(graphics, font, fit(font, cost, slotWidth - UiLayoutMetrics.SPACE_2), centerX, region.y() + 48,
                    TEXT_SECONDARY, true);
        }
        graphics.disableScissor();
    }

    private static void drawBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int value,
            int max,
            Identifier progressSprite
    ) {
        if (width <= 0 || height <= 0 || max <= 0) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND, x, y, width, height);
        int fill = Math.max(0, Math.min(width, (int) Math.round(width * (Math.max(0, Math.min(value, max)) / (double) max))));
        if (fill > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, progressSprite, x, y, fill, height);
    }

    private static void drawCentered(
            GuiGraphicsExtractor graphics,
            Font font,
            String text,
            int centerX,
            int y,
            int color,
            boolean shadow
    ) {
        graphics.text(font, Component.literal(text), centerX - font.width(text) / 2, y, color, shadow);
    }

    private static String resourceLine(BattleNetworkPayloads.SnapshotParticipant participant) {
        if ("PLAYER".equals(participant.team())) return "E " + participant.energy();
        return "P " + participant.poise() + "/" + participant.poiseMax();
    }

    private static String intentLine(BattleNetworkPayloads.SnapshotParticipant enemy) {
        BattleNetworkPayloads.SnapshotIntent intent = enemy.intent();
        if (intent == null) return enemy.alive() ? "INTENT -" : "DEFEATED";
        String action = conciseActionName(intent.actionId(), displayName(enemy));
        String breakRule = intent.breakCancelable() ? " · BREAK CANCEL" : "";
        return "INTENT " + intent.risk() + " · " + action + " · " + intent.type() + "/" + intent.targeting() + breakRule;
    }

    private static String statusLine(BattleNetworkPayloads.SnapshotParticipant participant) {
        if (!participant.alive()) return "DEFEATED";
        if (participant.exposed()) return "EXPOSED";
        if (participant.guard()) return "GUARD";
        if (participant.poiseGuard()) return "POISE GUARD";
        if (participant.statuses().isEmpty()) return "";
        BattleNetworkPayloads.SnapshotStatus status = participant.statuses().getFirst();
        String remaining = status.remaining() > 0 ? " " + status.remaining() + "T" : "";
        String stacks = status.stacks() > 1 ? " x" + status.stacks() : "";
        return humanizeId(status.id()) + stacks + remaining;
    }

    private static String slotGlyph(String slot) {
        return switch (slot) {
            case "BASIC" -> "B";
            case "GUARD" -> "G";
            case "BURST" -> "BR";
            default -> slot.startsWith("SKILL_") ? "S" + slot.substring("SKILL_".length()) : "?";
        };
    }

    private static String displayName(BattleNetworkPayloads.SnapshotParticipant participant) {
        return humanizeId(participant.characterId().isBlank() ? participant.id() : participant.characterId());
    }

    private static String conciseActionName(String actionId, String actorName) {
        String name = BattleActionPresentation.actionName(actionId);
        String prefix = actorName + " ";
        return name.regionMatches(true, 0, prefix, 0, prefix.length()) ? name.substring(prefix.length()) : name;
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
            if (word.length() > 1) out.append(word.substring(1).toLowerCase());
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (suffixWidth >= maxWidth) return "";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) + suffixWidth > maxWidth) end--;
        return end <= 0 ? "" : text.substring(0, end) + suffix;
    }
}
