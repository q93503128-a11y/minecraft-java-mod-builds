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
        int x = Math.max(12, width / 2 - 112);
        int y = height - 58;
        int barWidth = Math.min(150, Math.max(112, width / 4));

        double health = Math.max(0.0, mc.player.getHealth());
        double maxHealth = Math.max(1.0, mc.player.getMaxHealth());
        double hp = Math.min(1.0, health / maxHealth);
        double sanity = Math.max(0.0, Math.min(100.0, TitanClientState.decimal("sanity", 100.0)));
        double heat = Math.max(0.0, Math.min(100.0, TitanClientState.decimal("heat", 0.0)));
        boolean active = TitanClientState.flag("active");

        drawRail(g, x, y, barWidth, hp, 0xFF21191C, hp < 0.25 ? 0xFFE75262 : 0xFFD95A67,
                mc.player.tickCount, true);
        drawRail(g, x, y + 12, barWidth, sanity / 100.0, 0xFF171D27, 0xFF83A8E8,
                mc.player.tickCount + 7, false);

        g.text(font, Component.translatable("hud.titanbreak.health",
                        String.format(Locale.ROOT, "%.0f", health), String.format(Locale.ROOT, "%.0f", maxHealth)),
                x, y - 10, 0xFFE6DCE0);
        g.text(font, Component.translatable("hud.titanbreak.sanity", String.format(Locale.ROOT, "%.0f", sanity)),
                x, y + 16, 0xFFC8D5EA);

        if (active || heat > 1.0) {
            int hx = x + barWidth + 10;
            int heatHeight = 24;
            int filled = (int) Math.round(heatHeight * heat / 100.0);
            g.fill(hx, y, hx + 3, y + heatHeight, 0xFF2D2020);
            g.fill(hx, y + heatHeight - filled, hx + 3, y + heatHeight,
                    heat >= 80.0 ? 0xFFFF5A4C : 0xFFE59655);
            Component stateText = Component.translatable(active
                    ? "hud.titanbreak.reflex_active"
                    : "hud.titanbreak.cooling");
            g.text(font, stateText, hx + 7, y + 7, active ? 0xFFF0D8B8 : 0xFF9A928A);
        }
    }

    private static void drawRail(GuiGraphicsExtractor g, int x, int y, int width, double fraction,
                                 int background, int foreground, int tick, boolean flowing) {
        g.fill(x, y, x + width, y + 4, background);
        int filled = Math.max(0, Math.min(width, (int) Math.round(width * fraction)));
        if (filled > 0) g.fill(x, y, x + filled, y + 4, foreground);
        if (flowing && filled > 8) {
            int sweep = Math.floorMod(tick * 2, filled + 16) - 8;
            int left = Math.max(x, x + sweep);
            int right = Math.min(x + filled, left + 8);
            if (right > left) g.fill(left, y, right, y + 1, 0x66FFFFFF);
        }
    }
}
