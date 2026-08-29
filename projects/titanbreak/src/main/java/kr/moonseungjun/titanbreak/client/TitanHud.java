package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        double health = Math.max(0.0D, CombatScale.toVisible(mc.player.getHealth()));
        double maxHealth = Math.max(1.0D, CombatScale.toVisible(mc.player.getMaxHealth()));
        double hp = Math.min(1.0D, health / maxHealth);
        double sanity = Math.max(0.0D, Math.min(100.0D, TitanClientState.decimal("sanity", 100.0D)));
        double heat = Math.max(0.0D, Math.min(100.0D, TitanClientState.decimal("heat", 0.0D)));
        boolean active = TitanClientState.flag("active");

        drawMechanicalLiquidGauge(g, x, y, barWidth, hp,
                0xFF0D1014, 0xFF313942, 0xFF9DA8B1,
                hp < 0.25D ? 0xFFC93243 : 0xFFB92E40,
                0xFFF27882, mc.player.tickCount, true);
        drawMechanicalLiquidGauge(g, x, y + 18, barWidth, sanity / 100.0D,
                0xFF0D1014, 0xFF313942, 0xFF9DA8B1,
                sanity < 25.0D ? 0xFF654A9C : 0xFF355C91,
                0xFF8DB7EA, mc.player.tickCount + 11, false);

        g.text(font, Component.translatable("hud.titanbreak.health",
                        String.format(Locale.ROOT, "%.0f", health), String.format(Locale.ROOT, "%.0f", maxHealth)),
                x + 8, y - 10, 0xFFE8EDF1);
        g.text(font, Component.translatable("hud.titanbreak.sanity", String.format(Locale.ROOT, "%.0f", sanity)),
                x + 8, y + 28, 0xFFD4E2F2);

        int adaptationLevel = TitanClientState.integer("adaptLevel", 1);
        int rd = TitanClientState.integer("rd", 0);
        g.text(font, Component.translatable("hud.titanbreak.progress", adaptationLevel, rd), x, y + 42, 0xFF9FAEB7);

        if (active || heat > 1.0D) {
            int hx = x + barWidth + 14;
            int top = y - 1;
            int gaugeHeight = 34;
            g.fill(hx - 3, top - 2, hx + 8, top + gaugeHeight + 3, 0xFF0A0D10);
            g.fill(hx - 2, top - 1, hx + 7, top + gaugeHeight + 2, 0xFF4A5158);
            g.fill(hx, top + 1, hx + 5, top + gaugeHeight, 0xFF171B1F);
            int filled = (int) Math.round((gaugeHeight - 3) * heat / 100.0D);
            int color = heat >= 80.0D ? 0xFFFF554D : 0xFFE39443;
            g.fill(hx + 1, top + gaugeHeight - filled, hx + 4, top + gaugeHeight - 1, color);
            Component stateText = Component.translatable(active
                    ? "hud.titanbreak.reflex_active"
                    : "hud.titanbreak.cooling");
            g.text(font, stateText, hx + 11, y + 9, active ? 0xFFF2D3AC : 0xFFAEB4B9);
        }

        if (TitanKeyMappings.ANALYSIS.isDown()) renderAnalysis(g, mc, font, width);
        if (TitanClientState.hasInstalled("threat_detection")) renderThreatWarning(g, mc, font, width);
    }

    private static void renderAnalysis(GuiGraphicsExtractor g, Minecraft mc, Font font, int width) {
        boolean tactical = TitanClientState.hasInstalled("tactical_eye");
        boolean thermal = TitanClientState.hasInstalled("thermal_eye");
        boolean ballistic = TitanClientState.hasInstalled("ballistic_eye");
        boolean targetAssist = TitanClientState.hasInstalled("target_assist");
        if (!tactical && !thermal && !ballistic && !targetAssist) return;

        int left = width - 228;
        int top = 24;
        g.fill(left, top, width - 14, top + 112, 0xB810171C);
        g.fill(left + 2, top + 2, width - 16, top + 22, 0xDD20313B);
        g.text(font, Component.translatable("hud.titanbreak.analysis"), left + 10, top + 8, 0xFFE7EEF2);

        Entity target = mc.crosshairPickEntity;
        int y = top + 32;
        if (target instanceof LivingEntity living) {
            double distance = mc.player.distanceTo(living);
            g.text(font, living.getType().getDescription(), left + 10, y, 0xFFF1D39A);
            y += 15;
            if (tactical) {
                g.text(font, Component.translatable("hud.titanbreak.analysis_range", String.format(Locale.ROOT, "%.1f", distance)),
                        left + 10, y, 0xFFC6D4DB);
                y += 15;
                g.text(font, Component.translatable("hud.titanbreak.analysis_health",
                                String.format(Locale.ROOT, "%.0f", CombatScale.toVisible(living.getHealth())),
                                String.format(Locale.ROOT, "%.0f", CombatScale.toVisible(living.getMaxHealth()))),
                        left + 10, y, 0xFFC6D4DB);
                y += 15;
            }
            if (ballistic) {
                double flight = Math.max(0.2D, Math.min(0.6D, distance / 60.0D));
                Vec3 lead = living.getDeltaMovement().scale(flight * 20.0D);
                g.text(font, Component.translatable("hud.titanbreak.analysis_lead",
                                String.format(Locale.ROOT, "%.1f", lead.horizontalDistance())),
                        left + 10, y, 0xFF8CB8E5);
                y += 15;
            }
            if (targetAssist && distance <= 24.0D) {
                g.text(font, Component.translatable("hud.titanbreak.target_assist_ready"), left + 10, y, 0xFF8ED0A2);
            }
        } else {
            g.text(font, Component.translatable("hud.titanbreak.analysis_no_target"), left + 10, y, 0xFF778994);
        }

        if (thermal && mc.level != null) {
            AABB area = mc.player.getBoundingBox().inflate(24.0D);
            int signatures = mc.level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != mc.player && entity.isAlive()).size();
            g.text(font, Component.translatable("hud.titanbreak.thermal_signatures", signatures), left + 10, top + 92, 0xFFE39B71);
        }
    }

    private static void renderThreatWarning(GuiGraphicsExtractor g, Minecraft mc, Font font, int width) {
        if (mc.level == null || mc.player == null) return;
        AABB area = mc.player.getBoundingBox().inflate(10.0D);
        boolean danger = !mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player && entity.isAlive() && entity instanceof Enemy).isEmpty();
        if (danger) {
            Component text = Component.translatable("hud.titanbreak.threat_warning");
            int textWidth = font.width(text);
            g.text(font, text, width / 2 - textWidth / 2, 38, 0xFFF06D66);
        }
    }

    private static void drawMechanicalLiquidGauge(GuiGraphicsExtractor g, int x, int y, int width,
                                                   double fraction, int voidColor, int frameColor,
                                                   int edgeColor, int liquidColor, int highlightColor,
                                                   int tick, boolean turbulent) {
        int height = 9;
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
            int segmentLeft = innerX + Math.max(0, offset);
            int right = Math.min(innerX + filled, segmentLeft + (turbulent ? 5 : 4));
            if (right > segmentLeft) g.fill(segmentLeft, innerY, right, innerY + 1, highlightColor);
        }
        if (filled > 12) {
            int bubble = Math.floorMod(tick * 3, filled - 4);
            g.fill(innerX + 2 + bubble, innerY + 1, innerX + 4 + bubble, innerY + 2, 0x88FFFFFF);
        }
    }
}
