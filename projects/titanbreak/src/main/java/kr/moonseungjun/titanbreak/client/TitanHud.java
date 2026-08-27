package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Locale;

public final class TitanHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "vitals");

    private TitanHud() {}

    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, TitanHud::render);
    }

    public static void onVanillaLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Identifier name = event.getName();
        if (VanillaGuiLayers.PLAYER_HEALTH.equals(name)
                || VanillaGuiLayers.ARMOR_LEVEL.equals(name)
                || VanillaGuiLayers.FOOD_LEVEL.equals(name)) {
            event.setCanceled(true);
        }
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;

        int height = mc.getWindow().getGuiScaledHeight();
        int width = mc.getWindow().getGuiScaledWidth();
        Font font = mc.font;
        int barWidth = Math.min(158, Math.max(120, width / 4));
        int x = Math.max(12, width / 2 - 116);
        int y = height - 68;

        double health = Math.max(0.0, mc.player.getHealth());
        double maxHealth = Math.max(1.0, mc.player.getMaxHealth());
        double hp = Math.min(1.0, health / maxHealth);
        double sanity = Math.max(0.0, Math.min(100.0, TitanClientState.decimal("sanity", 100.0)));
        double heat = Math.max(0.0, Math.min(100.0, TitanClientState.decimal("heat", 0.0)));
        boolean active = TitanClientState.flag("active");

        drawMechanicalLiquidGauge(g, x, y, barWidth, hp,
                0xFF0D1014, 0xFF313942, 0xFF9DA8B1,
                hp < 0.25 ? 0xFFC93243 : 0xFFB92E40,
                0xFFF27882, mc.player.tickCount, true);
        drawMechanicalLiquidGauge(g, x, y + 18, barWidth, sanity / 100.0,
                0xFF0D1014, 0xFF313942, 0xFF9DA8B1,
                sanity < 25.0 ? 0xFF654A9C : 0xFF355C91,
                0xFF8DB7EA, mc.player.tickCount + 11, false);

        g.text(font, Component.translatable("hud.titanbreak.health",
                        String.format(Locale.ROOT, "%.0f", health), String.format(Locale.ROOT, "%.0f", maxHealth)),
                x + 8, y - 10, 0xFFE8EDF1);
        g.text(font, Component.translatable("hud.titanbreak.sanity", String.format(Locale.ROOT, "%.0f", sanity)),
                x + 8, y + 28, 0xFFD4E2F2);

        if (active || heat > 1.0) {
            int hx = x + barWidth + 14;
            int top = y - 1;
            int gaugeHeight = 34;
            g.fill(hx - 3, top - 2, hx + 8, top + gaugeHeight + 3, 0xFF0A0D10);
            g.fill(hx - 2, top - 1, hx + 7, top + gaugeHeight + 2, 0xFF4A5158);
            g.fill(hx, top + 1, hx + 5, top + gaugeHeight, 0xFF171B1F);
            int filled = (int) Math.round((gaugeHeight - 3) * heat / 100.0);
            int color = heat >= 80.0 ? 0xFFFF554D : 0xFFE39443;
            g.fill(hx + 1, top + gaugeHeight - filled, hx + 4, top + gaugeHeight - 1, color);
            Component stateText = Component.translatable(active
                    ? "hud.titanbreak.reflex_active"
                    : "hud.titanbreak.cooling");
            g.text(font, stateText, hx + 11, y + 9, active ? 0xFFF2D3AC : 0xFFAEB4B9);
        }
    }

    private static void drawMechanicalLiquidGauge(GuiGraphicsExtractor g, int x, int y, int width,
                                                   double fraction, int voidColor, int frameColor,
                                                   int edgeColor, int liquidColor, int highlightColor,
                                                   int tick, boolean turbulent) {
        int height = 9;

        // Mechanical housing and corner clamps. This is a P0 functional treatment;
        // final art will be replaced by licensed external UI assets.
        g.fill(x, y, x + width, y + height, voidColor);
        g.fill(x, y, x + width, y + 2, frameColor);
        g.fill(x, y + height - 2, x + width, y + height, frameColor);
        g.fill(x, y, x + 3, y + height, frameColor);
        g.fill(x + width - 3, y, x + width, y + height, frameColor);
        g.fill(x + 3, y + 2, x + width - 3, y + 3, 0xFF171C21);
        g.fill(x, y, x + 7, y + 2, edgeColor);
        g.fill(x + width - 7, y + height - 2, x + width, y + height, edgeColor);

        int innerX = x + 4;
        int innerY = y + 3;
        int innerWidth = width - 8;
        int innerHeight = 3;
        int filled = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * fraction)));
        g.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, 0xFF171B20);
        if (filled <= 0) return;

        g.fill(innerX, innerY, innerX + filled, innerY + innerHeight, liquidColor);

        int waveStep = turbulent ? 9 : 13;
        int phase = Math.floorMod(tick * (turbulent ? 2 : 1), waveStep * 2);
        for (int offset = -phase; offset < filled; offset += waveStep) {
            int left = innerX + Math.max(0, offset);
            int right = Math.min(innerX + filled, left + (turbulent ? 5 : 4));
            if (right > left) g.fill(left, innerY, right, innerY + 1, highlightColor);
        }

        if (filled > 12) {
            int bubble = Math.floorMod(tick * 3, filled - 4);
            g.fill(innerX + 2 + bubble, innerY + 1, innerX + 4 + bubble, innerY + 2, 0x88FFFFFF);
        }
    }
}
