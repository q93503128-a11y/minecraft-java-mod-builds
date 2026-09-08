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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Production battle HUD over authoritative S2C presentation data.
 * The center world viewport stays unobstructed while semantic focus/warning states share the M5 visual language.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_hud");

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
            UiVisualLanguage.frame(graphics, region.x(), y, 20, 20, current);
            String teamGlyph = "PLAYER".equals(participant.team()) ? "P" : "E";
            drawCentered(graphics, font, teamGlyph, region.x() + 10, y + 6,
                    participant.alive() ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                            : UiVisualLanguage.TEXT_SECONDARY,
                    true);
            String label = fit(font, displayName(participant), region.width() - 24);
            graphics.text(font, Component.literal(label), region.x() + 24, y + 2,
                    participant.alive() ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                            : UiVisualLanguage.TEXT_SECONDARY,
                    true);
            String secondary = current
                    ? BattleHudPresentation.currentTurnLabel().getString()
                    : (participant.exposed() ? BattleHudPresentation.exposedLabel().getString() : resourceLine(participant));
            int secondaryColor = current ? UiVisualLanguage.TEXT_FOCUS
                    : (participant.exposed() ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_SECONDARY);
            graphics.text(font, Component.literal(fit(font, secondary, region.width() - 24)),
                    region.x() + 24, y + 11, secondaryColor, true);
        }
        if (order.size() > visible) {
            String more = "+" + (order.size() - visible);
            graphics.text(font, Component.literal(more), region.x() + 2, region.bottom() - font.lineHeight,
                    UiVisualLanguage.TEXT_SECONDARY, true);
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
                enemy.alive() ? UiVisualLanguage.TEXT_PRIMARY : UiVisualLanguage.TEXT_SECONDARY, true);
        UiVisualLanguage.meter(graphics, region.x(), region.y() + 11, region.width(), 5,
                enemy.hp(), enemy.maxHp(), UiVisualLanguage.HP_PROGRESS);
        UiVisualLanguage.meter(graphics, region.x(), region.y() + 18, region.width(), 4,
                enemy.poise(), enemy.poiseMax(), UiVisualLanguage.POISE_PROGRESS);

        BattleNetworkPayloads.SnapshotIntent snapshotIntent = enemy.intent();
        String actionName = snapshotIntent == null ? "" : conciseActionName(snapshotIntent.actionId(), displayName(enemy));
        String intent = BattleHudPresentation.intentLine(enemy, actionName).getString();
        graphics.text(font, Component.literal(fit(font, intent, region.width())), region.x(), region.y() + 24,
                snapshotIntent == null ? UiVisualLanguage.TEXT_SECONDARY : UiVisualLanguage.TEXT_WARNING, true);
        String status = BattleHudPresentation.statusLine(enemy).getString();
        if (!status.isBlank()) {
            graphics.text(font, Component.literal(fit(font, status, region.width())), region.x(), region.y() + 34,
                    enemy.exposed() ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_SECONDARY, true);
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
        UiLayoutMetrics.PartyGridLayout grid = UiLayoutMetrics.partyGrid(region, count);
        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());

        for (int i = 0; i < count; i++) {
            BattleNetworkPayloads.SnapshotParticipant member = party.get(i);
            int col = i % grid.columns();
            int row = i / grid.columns();
            int x = region.x() + col * grid.cellWidth();
            int y = region.y() + row * grid.cellHeight();
            if (grid.compact()) {
                renderCompactPartyMember(graphics, font, x, y, grid.cellWidth(), grid.cellHeight(), i, member, model);
            } else {
                renderRegularPartyMember(graphics, font, x, y, grid.cellWidth(), grid.cellHeight(), i, member, model);
            }
        }
        graphics.disableScissor();
    }

    private static void renderRegularPartyMember(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int cellWidth,
            int cellHeight,
            int index,
            BattleNetworkPayloads.SnapshotParticipant member,
            BattlePresentationModel model
    ) {
        int slotWidth = Math.min(180, cellWidth);
        boolean current = member.id().equals(model.currentActorId());
        UiVisualLanguage.frame(graphics, x, y, 20, 20, current);
        drawCentered(graphics, font, Integer.toString(index + 1), x + 10, y + 6,
                member.alive() ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                        : UiVisualLanguage.TEXT_SECONDARY,
                true);

        int textX = x + 24;
        int contentWidth = Math.max(24, slotWidth - 24 - UiLayoutMetrics.SPACE_4);
        graphics.text(font, Component.literal(fit(font, displayName(member), contentWidth)), textX, y + 1,
                member.alive() ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                        : UiVisualLanguage.TEXT_SECONDARY,
                true);
        graphics.text(font, Component.literal("HP " + member.hp() + "/" + member.maxHp()), textX, y + 11,
                UiVisualLanguage.TEXT_SECONDARY, true);

        int barWidth = Math.max(24, slotWidth - UiLayoutMetrics.SPACE_4);
        UiVisualLanguage.meter(graphics, x, y + 24, barWidth, 5,
                member.hp(), member.maxHp(), UiVisualLanguage.HP_PROGRESS);
        UiVisualLanguage.meter(graphics, x, y + 31, barWidth, 4,
                member.energy(), 100, UiVisualLanguage.ENERGY_PROGRESS);
        graphics.text(font, Component.literal("E " + member.energy() + "/100"), x, y + 38,
                UiVisualLanguage.TEXT_SECONDARY, true);
        String status = BattleHudPresentation.statusLine(member).getString();
        if (!status.isBlank() && cellHeight >= 58) {
            graphics.text(font, Component.literal(fit(font, status, barWidth)), x, y + 48,
                    member.exposed() ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_SECONDARY, true);
        }
    }

    private static void renderCompactPartyMember(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int cellWidth,
            int cellHeight,
            int index,
            BattleNetworkPayloads.SnapshotParticipant member,
            BattlePresentationModel model
    ) {
        int contentWidth = Math.max(24, cellWidth - 22 - UiLayoutMetrics.SPACE_4);
        int barWidth = Math.max(24, cellWidth - UiLayoutMetrics.SPACE_4);
        boolean current = member.id().equals(model.currentActorId());
        UiVisualLanguage.frame(graphics, x, y, 18, 18, current);
        drawCentered(graphics, font, Integer.toString(index + 1), x + 9, y + 5,
                member.alive() ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                        : UiVisualLanguage.TEXT_SECONDARY,
                true);
        graphics.text(font, Component.literal(fit(font, displayName(member), contentWidth)), x + 22, y,
                member.alive() ? (current ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_PRIMARY)
                        : UiVisualLanguage.TEXT_SECONDARY,
                true);

        String resources = "HP " + member.hp() + "/" + member.maxHp() + " · E " + member.energy();
        graphics.text(font, Component.literal(fit(font, resources, contentWidth)), x + 22, y + 10,
                UiVisualLanguage.TEXT_SECONDARY, true);
        UiVisualLanguage.meter(graphics, x, y + 21, barWidth, 4,
                member.hp(), member.maxHp(), UiVisualLanguage.HP_PROGRESS);
        UiVisualLanguage.meter(graphics, x, y + 27, barWidth, 4,
                member.energy(), 100, UiVisualLanguage.ENERGY_PROGRESS);

        String status = BattleHudPresentation.statusLine(member).getString();
        if (!status.isBlank() && cellHeight >= 40) {
            graphics.text(font, Component.literal(fit(font, status, barWidth)), x, y + 32,
                    member.exposed() ? UiVisualLanguage.TEXT_WARNING : UiVisualLanguage.TEXT_SECONDARY, true);
        }
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
        String header = Component.translatable(
                "hud.turnbound_re.command_header",
                displayName(actor),
                BattleInputHandler.openKeyName()).getString();
        UiVisualLanguage.titleBand(
                graphics,
                font,
                region.x(),
                region.y(),
                region.width(),
                16,
                Component.literal(fit(font, header, region.width() - UiLayoutMetrics.SPACE_8)),
                UiVisualLanguage.TEXT_FOCUS,
                true);

        List<BattleNetworkPayloads.SnapshotAction> actions = model.availableActions();
        if (actions.isEmpty()) {
            String unavailable = Component.translatable("hud.turnbound_re.no_actions").getString();
            graphics.text(font, Component.literal(fit(font, unavailable, region.width() - UiLayoutMetrics.SPACE_8)),
                    region.x() + UiLayoutMetrics.SPACE_4, region.y() + 24, UiVisualLanguage.TEXT_SECONDARY, true);
            graphics.disableScissor();
            return;
        }

        int slotCount = actions.size();
        int slotWidth = Math.max(34, region.width() / slotCount);
        for (int i = 0; i < slotCount; i++) {
            BattleNetworkPayloads.SnapshotAction action = actions.get(i);
            int x = region.x() + i * slotWidth;
            int centerX = x + slotWidth / 2;
            UiVisualLanguage.frame(graphics, centerX - 9, region.y() + 18, 18, 18, action.usable());
            drawCentered(graphics, font, slotGlyph(action.slot()), centerX, region.y() + 23,
                    action.usable() ? UiVisualLanguage.TEXT_FOCUS : UiVisualLanguage.TEXT_SECONDARY, true);

            String name = conciseActionName(action.id(), displayName(actor));
            drawCentered(graphics, font, fit(font, name, slotWidth - UiLayoutMetrics.SPACE_2), centerX, region.y() + 38,
                    action.usable() ? UiVisualLanguage.TEXT_PRIMARY : UiVisualLanguage.TEXT_SECONDARY, true);
            String cost = BattleActionPresentation.hudCost(action).getString();
            drawCentered(graphics, font, fit(font, cost, slotWidth - UiLayoutMetrics.SPACE_2), centerX, region.y() + 48,
                    action.usable() ? UiVisualLanguage.TEXT_SECONDARY : UiVisualLanguage.TEXT_WARNING, true);
        }
        graphics.disableScissor();
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
