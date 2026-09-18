package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.ExpeditionJournalClientState;
import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;
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

/** Compact world-first route guidance for the locally selected Expedition Journal route. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class ExpeditionRouteHud {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "expedition_route_hud");
    private static final double ARRIVAL_DISTANCE = 18.0D;

    private ExpeditionRouteHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, ExpeditionRouteHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (BattleClientState.presentation().isPresent()) return;

        ExpeditionNetworkPayloads.EncounterView route = ExpeditionJournalClientState.trackedRoute().orElse(null);
        if (route == null || !route.hasWorldRoute()) return;

        String currentDimension = minecraft.player.level().dimension().identifier().toString();
        Component encounter = encounterName(route.id());
        Component line;
        UiVisualLanguage.FrameState state = UiVisualLanguage.FrameState.FOCUS;

        if (!route.dimension().equals(currentDimension)) {
            line = Component.translatable(
                    "hud.turnbound_re.expedition.other_dimension",
                    encounter.getString(), route.dimension());
            state = UiVisualLanguage.FrameState.WARNING;
        } else {
            double dx = route.x() + 0.5D - minecraft.player.getX();
            double dy = route.y() - minecraft.player.getY();
            double dz = route.z() + 0.5D - minecraft.player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= ARRIVAL_DISTANCE) {
                line = Component.translatable("hud.turnbound_re.expedition.near", encounter.getString());
                state = UiVisualLanguage.FrameState.SUCCESS;
            } else {
                line = Component.translatable(
                        "hud.turnbound_re.expedition.route",
                        encounter.getString(), cardinal(dx, dz), Math.round(distance));
            }
        }

        Font font = minecraft.font;
        int width = Math.min(300, Math.max(180, graphics.guiWidth() - UiLayoutMetrics.SPACE_16));
        int height = 24;
        int x = Math.max(0, (graphics.guiWidth() - width) / 2);
        int y = Math.max(UiLayoutMetrics.SPACE_8, graphics.guiHeight() - 70);
        UiVisualLanguage.frame(graphics, x, y, width, height, state);

        String text = fit(font, line.getString(), width - UiLayoutMetrics.SPACE_16);
        int textX = x + Math.max(UiLayoutMetrics.SPACE_8, (width - font.width(text)) / 2);
        int textY = y + Math.max(UiLayoutMetrics.SPACE_4, (height - font.lineHeight) / 2);
        graphics.text(font, Component.literal(text), textX, textY, UiVisualLanguage.textColor(state), true);
    }

    static String cardinal(double dx, double dz) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz)
                || (Math.abs(dx) < 0.001D && Math.abs(dz) < 0.001D)) {
            return "·";
        }
        double angle = Math.atan2(dz, dx);
        int octant = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8);
        return switch (octant) {
            case 0 -> "E";
            case 1 -> "SE";
            case 2 -> "S";
            case 3 -> "SW";
            case 4 -> "W";
            case 5 -> "NW";
            case 6 -> "N";
            case 7 -> "NE";
            default -> "·";
        };
    }

    private static Component encounterName(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return Component.translatable("encounter.turnbound_re." + path + ".name");
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }
}
