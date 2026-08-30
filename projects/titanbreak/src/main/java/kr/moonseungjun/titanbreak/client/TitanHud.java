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

        double health = Math.max(0.0D, CombatScale.toVisible(mc.player.getHealth()));
        double maxHealth = Math.max(1.0D, CombatScale.toVisible(mc.player.getMaxHealth()));
        double hp = Math.min(1.0D, health / maxHealth);
        double sanity = clamp100(TitanClientState.decimal("sanity", 100.0D));
        double heat = clamp100(TitanClientState.decimal("heat", 0.0D));
        boolean active = TitanClientState.flag("active");

        renderVitalsDeck(g, mc, font, width, height, health, maxHealth, hp, sanity, heat, active);

        if (TitanKeyMappings.ANALYSIS.isDown()) renderAnalysis(g, mc, font, width);
        if (TitanClientState.hasInstalled("threat_detection")) renderThreatWarning(g, mc, font, width);
    }

    private static void renderVitalsDeck(GuiGraphicsExtractor g, Minecraft mc, Font font, int width, int height,
                                         double health, double maxHealth, double hp, double sanity,
                                         double heat, boolean active) {
        int deckWidth = Math.min(214, Math.max(176, width / 3));
        int left = Math.max(8, width / 2 - deckWidth - 104);
        int bottom = height - 8;
        int top = bottom - 62;
        int right = left + deckWidth;

        g.fill(left, top, right, bottom, 0xD90A0E12);
        g.outline(left, top, deckWidth, bottom - top, TitanInterfaceTheme.LINE_SOFT);
        g.fill(left + 1, top + 1, right - 1, top + 14, 0xE51C272E);
        g.fill(left + 1, top + 14, right - 1, top + 15, TitanInterfaceTheme.CYAN);
        drawHudCorners(g, left, top, right, bottom, active ? TitanInterfaceTheme.ACCENT : TitanInterfaceTheme.CYAN);

        int adaptationLevel = TitanClientState.integer("adaptLevel", 1);
        int rd = TitanClientState.integer("rd", 0);
        Component progress = Component.translatable("hud.titanbreak.progress", adaptationLevel, rd);
        g.text(font, progress, left + 7, top + 4, TitanInterfaceTheme.TEXT_MUTED, false);

        int gaugeX = left + 7;
        int gaugeWidth = deckWidth - 14;
        int hpY = top + 24;
        int sanityY = top + 43;

        Component hpText = Component.translatable("hud.titanbreak.health",
                String.format(Locale.ROOT, "%.0f", health), String.format(Locale.ROOT, "%.0f", maxHealth));
        g.text(font, hpText, gaugeX, hpY - 8, TitanInterfaceTheme.TEXT, false);
        drawSegmentedGauge(g, gaugeX, hpY, gaugeWidth, hp,
                hp < 0.25D ? 0xFFD84A54 : 0xFFC43A45, 10, mc.player.tickCount, true);

        Component sanityText = Component.translatable("hud.titanbreak.sanity", String.format(Locale.ROOT, "%.0f", sanity));
        g.text(font, sanityText, gaugeX, sanityY - 8, 0xFFB7CDE0, false);
        drawSegmentedGauge(g, gaugeX, sanityY, gaugeWidth, sanity / 100.0D,
                sanity < 25.0D ? 0xFF785AA8 : 0xFF4A78A5, 10, mc.player.tickCount + 7, false);

        if (active || heat > 1.0D) {
            int heatX = right + 7;
            int heatTop = top + 5;
            int heatHeight = 52;
            drawHeatRail(g, heatX, heatTop, heatHeight, heat, active);
            Component state = Component.translatable(active ? "hud.titanbreak.reflex_active" : "hud.titanbreak.cooling");
            int textX = heatX + 12;
            int textY = top + 23;
            g.text(font, state, textX, textY, active ? TitanInterfaceTheme.ACCENT : TitanInterfaceTheme.TEXT_MUTED, false);
        }
    }

    private static void renderAnalysis(GuiGraphicsExtractor g, Minecraft mc, Font font, int width) {
        boolean tactical = TitanClientState.hasInstalled("tactical_eye");
        boolean thermal = TitanClientState.hasInstalled("thermal_eye");
        boolean ballistic = TitanClientState.hasInstalled("ballistic_eye");
        boolean targetAssist = TitanClientState.hasInstalled("target_assist");
        if (!tactical && !thermal && !ballistic && !targetAssist) return;

        int panelWidth = Math.min(224, Math.max(184, width / 3));
        int right = width - 12;
        int left = right - panelWidth;
        int top = 18;
        int bottom = top + 126;

        g.fill(left, top, right, bottom, 0xD510171C);
        g.outline(left, top, panelWidth, bottom - top, TitanInterfaceTheme.LINE_SOFT);
        g.fill(left + 1, top + 1, right - 1, top + 21, 0xE51E2C34);
        g.fill(left + 1, top + 21, right - 1, top + 22, TitanInterfaceTheme.CYAN);
        drawHudCorners(g, left, top, right, bottom, TitanInterfaceTheme.CYAN);
        g.text(font, Component.translatable("hud.titanbreak.analysis"), left + 8, top + 7, TitanInterfaceTheme.TEXT, false);

        int jamTicks = TitanClientState.integer("jamTicks", 0);
        if (jamTicks > 0) {
            renderJammed(g, mc, font, left, top, right, bottom, jamTicks);
            return;
        }

        Entity target = mc.crosshairPickEntity;
        int y = top + 31;
        if (target instanceof LivingEntity living) {
            double distance = mc.player.distanceTo(living);
            g.text(font, living.getType().getDescription(), left + 9, y, TitanInterfaceTheme.ACCENT, false);
            g.horizontalLine(left + 9, right - 9, y + 11, TitanInterfaceTheme.LINE_SOFT);
            y += 18;

            if (tactical) {
                Component range = Component.translatable("hud.titanbreak.analysis_range", String.format(Locale.ROOT, "%.1f", distance));
                g.text(font, range, left + 9, y, TitanInterfaceTheme.TEXT_MUTED, false);
                y += 14;
                double targetHp = CombatScale.toVisible(living.getHealth());
                double targetMax = Math.max(1.0D, CombatScale.toVisible(living.getMaxHealth()));
                Component targetHealth = Component.translatable("hud.titanbreak.analysis_health",
                        String.format(Locale.ROOT, "%.0f", targetHp), String.format(Locale.ROOT, "%.0f", targetMax));
                g.text(font, targetHealth, left + 9, y, TitanInterfaceTheme.TEXT_MUTED, false);
                y += 13;
                drawSegmentedGauge(g, left + 9, y, panelWidth - 18,
                        Math.max(0.0D, Math.min(1.0D, targetHp / targetMax)), 0xFFB34A50, 12, living.tickCount, false);
                y += 11;
            }

            if (ballistic) {
                double flight = Math.max(0.2D, Math.min(0.6D, distance / 60.0D));
                Vec3 lead = living.getDeltaMovement().scale(flight * 20.0D);
                g.text(font, Component.translatable("hud.titanbreak.analysis_lead",
                                String.format(Locale.ROOT, "%.1f", lead.horizontalDistance())),
                        left + 9, y, 0xFF8CB8E5, false);
                y += 14;
            }
            if (targetAssist && distance <= 24.0D) {
                g.text(font, Component.translatable("hud.titanbreak.target_assist_ready"),
                        left + 9, y, TitanInterfaceTheme.GOOD, false);
            }
        } else {
            g.textWithWordWrap(font, Component.translatable("hud.titanbreak.analysis_no_target"),
                    left + 9, y, panelWidth - 18, TitanInterfaceTheme.TEXT_MUTED, false);
        }

        if (thermal && mc.level != null) {
            AABB area = mc.player.getBoundingBox().inflate(24.0D);
            int signatures = mc.level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != mc.player && entity.isAlive()).size();
            Component thermalText = Component.translatable("hud.titanbreak.thermal_signatures", signatures);
            g.text(font, thermalText, left + 9, bottom - 17, 0xFFE39B71, false);
            drawContactPips(g, right - 66, bottom - 17, Math.min(6, signatures));
        }
    }

    private static void renderJammed(GuiGraphicsExtractor g, Minecraft mc, Font font,
                                     int left, int top, int right, int bottom, int jamTicks) {
        int phase = mc.player == null ? 0 : Math.floorMod(mc.player.tickCount, 14);
        for (int y = top + 29 + phase; y < bottom - 8; y += 14) {
            g.fill(left + 8, y, right - 8, y + 2, 0x886A3657);
        }
        for (int x = left + 12 + phase; x < right - 12; x += 26) {
            g.fill(x, top + 28, x + 2, bottom - 9, 0x334A83A0);
        }
        Component jammed = Component.translatable("hud.titanbreak.analysis_jammed",
                String.format(Locale.ROOT, "%.1f", jamTicks / 20.0D));
        g.centeredText(font, jammed, (left + right) / 2, top + 57, 0xFFE37C9D);
    }

    private static void renderThreatWarning(GuiGraphicsExtractor g, Minecraft mc, Font font, int width) {
        if (mc.level == null || mc.player == null) return;
        AABB area = mc.player.getBoundingBox().inflate(10.0D);
        boolean danger = !mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player && entity.isAlive() && entity instanceof Enemy).isEmpty();
        if (!danger) return;

        Component text = Component.translatable("hud.titanbreak.threat_warning");
        int textWidth = font.width(text);
        int cx = width / 2;
        int left = cx - textWidth / 2 - 13;
        int right = cx + textWidth / 2 + 13;
        int top = 31;
        int bottom = 48;
        int pulse = Math.floorMod(mc.player.tickCount, 12) < 6 ? 0xFFD95F5B : 0xFFA54848;

        g.fill(left, top, right, bottom, 0xA51B1012);
        g.fill(left, top, left + 2, bottom, pulse);
        g.fill(right - 2, top, right, bottom, pulse);
        g.horizontalLine(left, left + 12, top, pulse);
        g.horizontalLine(right - 12, right, top, pulse);
        g.horizontalLine(left, left + 12, bottom, pulse);
        g.horizontalLine(right - 12, right, bottom, pulse);
        g.centeredText(font, text, cx, top + 5, 0xFFF08B84);
    }

    private static void drawSegmentedGauge(GuiGraphicsExtractor g, int x, int y, int width,
                                           double fraction, int fillColor, int segments,
                                           int tick, boolean turbulent) {
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        g.fill(x, y, x + width, y + 7, 0xFF11171B);
        g.outline(x, y, width, 7, 0xFF47545C);

        int innerX = x + 2;
        int innerY = y + 2;
        int innerWidth = Math.max(1, width - 4);
        int filled = (int) Math.round(innerWidth * fraction);
        if (filled > 0) g.fill(innerX, innerY, innerX + filled, innerY + 3, fillColor);

        int step = Math.max(4, innerWidth / Math.max(1, segments));
        for (int sx = innerX + step; sx < innerX + innerWidth; sx += step) {
            g.fill(sx, innerY, sx + 1, innerY + 3, 0xAA11171B);
        }
        if (filled > 8) {
            int phase = Math.floorMod(tick * (turbulent ? 2 : 1), 12);
            for (int sx = innerX - phase; sx < innerX + filled; sx += 12) {
                int start = Math.max(innerX, sx);
                int end = Math.min(innerX + filled, start + 4);
                if (end > start) g.fill(start, innerY, end, innerY + 1, 0x66FFFFFF);
            }
        }
    }

    private static void drawHeatRail(GuiGraphicsExtractor g, int x, int y, int height,
                                     double heat, boolean active) {
        g.fill(x, y, x + 8, y + height, 0xDD0C1115);
        g.outline(x, y, 8, height, TitanInterfaceTheme.LINE);
        g.fill(x + 2, y + 2, x + 6, y + height - 2, 0xFF242B30);
        int inner = height - 4;
        int filled = (int) Math.round(inner * heat / 100.0D);
        int color = heat >= 80.0D ? 0xFFF05A51 : TitanInterfaceTheme.ACCENT;
        if (filled > 0) g.fill(x + 2, y + height - 2 - filled, x + 6, y + height - 2, color);
        for (int ty = y + 8; ty < y + height - 4; ty += 8) {
            g.horizontalLine(x - 2, x + 1, ty, active ? TitanInterfaceTheme.ACCENT : TitanInterfaceTheme.LINE);
        }
    }

    private static void drawContactPips(GuiGraphicsExtractor g, int x, int y, int count) {
        for (int i = 0; i < 6; i++) {
            int px = x + i * 9;
            int color = i < count ? 0xFFE39B71 : 0xFF34434A;
            g.fill(px, y + 2, px + 5, y + 6, color);
        }
    }

    private static void drawHudCorners(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int color) {
        g.horizontalLine(left, left + 10, top, color);
        g.verticalLine(left, top, top + 9, color);
        g.horizontalLine(right - 10, right, top, color);
        g.verticalLine(right - 1, top, top + 9, color);
        g.horizontalLine(left, left + 10, bottom - 1, color);
        g.verticalLine(left, bottom - 10, bottom - 1, color);
        g.horizontalLine(right - 10, right, bottom - 1, color);
        g.verticalLine(right - 1, bottom - 10, bottom - 1, color);
    }

    private static double clamp100(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
