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

        renderVitalsRail(g, mc, font, width, health, maxHealth, hp, sanity, heat, active);
        if (TitanKeyMappings.ANALYSIS.isDown()) renderAnalysis(g, mc, font, width, height);
        if (TitanClientState.hasInstalled("threat_detection")) renderThreatWarning(g, mc, font, width);
    }

    private static void renderVitalsRail(GuiGraphicsExtractor g, Minecraft mc, Font font, int width,
                                         double health, double maxHealth, double hp, double sanity,
                                         double heat, boolean active) {
        int railWidth = Math.min(174, Math.max(142, width / 5));
        int left = 8;
        int top = 8;
        int right = left + railWidth;
        int bottom = top + 51;

        /* Edge-mounted rather than hotbar-mounted: no inventory-slot or chat obstruction. */
        g.fill(left + 5, top, right, bottom, 0x9B060A0C);
        g.fill(left, top + 5, right, bottom - 4, 0x9B060A0C);
        g.horizontalLine(left + 5, right, top, 0x8843D7E8);
        g.verticalLine(left, top + 5, bottom - 4, 0x8843D7E8);
        g.horizontalLine(left, left + 18, bottom - 1, 0x66F0D23D);
        g.fill(left + 6, top + 4, left + 8, top + 12, active ? TitanInterfaceTheme.ACCENT : TitanInterfaceTheme.CYAN);

        int adaptationLevel = TitanClientState.integer("adaptLevel", 1);
        int rd = TitanClientState.integer("rd", 0);
        Component progress = Component.translatable("hud.titanbreak.progress", adaptationLevel, rd);
        g.text(font, progress, left + 12, top + 4, TitanInterfaceTheme.TEXT_MUTED, false);

        int gaugeX = left + 8;
        int gaugeWidth = railWidth - 16;
        Component hpText = Component.translatable("hud.titanbreak.health",
                String.format(Locale.ROOT, "%.0f", health), String.format(Locale.ROOT, "%.0f", maxHealth));
        g.text(font, hpText, gaugeX, top + 16, TitanInterfaceTheme.TEXT, false);
        drawSegmentedGauge(g, gaugeX, top + 27, gaugeWidth, hp,
                hp < 0.25D ? 0xFFE14D58 : 0xFFBF3944, 12);

        Component sanityText = Component.translatable("hud.titanbreak.sanity", String.format(Locale.ROOT, "%.0f", sanity));
        g.text(font, sanityText, gaugeX, top + 33, 0xFF9BC8E7, false);
        drawSegmentedGauge(g, gaugeX, top + 44, gaugeWidth, sanity / 100.0D,
                sanity < 25.0D ? 0xFF8C5DC0 : 0xFF4387B5, 12);

        if (active || heat > 1.0D) {
            int heatY = bottom + 3;
            int heatWidth = Math.min(railWidth, 130);
            g.fill(left + 5, heatY, left + 5 + heatWidth, heatY + 3, 0xBB181A16);
            int filled = (int) Math.round(heatWidth * heat / 100.0D);
            if (filled > 0) g.fill(left + 5, heatY, left + 5 + filled, heatY + 3,
                    heat >= 80.0D ? TitanInterfaceTheme.SIGNAL_RED : TitanInterfaceTheme.ACCENT);
            Component state = Component.translatable(active ? "hud.titanbreak.reflex_active" : "hud.titanbreak.cooling");
            g.text(font, state, left + 10 + heatWidth, heatY - 4,
                    active ? TitanInterfaceTheme.ACCENT : TitanInterfaceTheme.TEXT_MUTED, false);
        }
    }

    private static void renderAnalysis(GuiGraphicsExtractor g, Minecraft mc, Font font, int width, int height) {
        boolean tactical = TitanClientState.hasInstalled("tactical_eye");
        boolean thermal = TitanClientState.hasInstalled("thermal_eye");
        boolean ballistic = TitanClientState.hasInstalled("ballistic_eye");
        boolean targetAssist = TitanClientState.hasInstalled("target_assist");
        if (!tactical && !thermal && !ballistic && !targetAssist) return;

        int panelWidth = Math.min(190, Math.max(154, width / 4));
        int right = width - 8;
        int left = right - panelWidth;
        int top = Math.min(68, Math.max(8, height / 12));
        int bottom = Math.min(height - 34, top + 120);

        g.fill(left + 5, top, right, bottom, 0xB9080C0F);
        g.fill(left, top + 5, right, bottom - 4, 0xB9080C0F);
        g.horizontalLine(left + 5, right, top, TitanInterfaceTheme.CYAN);
        g.verticalLine(right - 1, top, bottom - 4, 0x8843D7E8);
        g.fill(left + 7, top + 5, left + 9, top + 15, TitanInterfaceTheme.ACCENT);
        g.text(font, Component.translatable("hud.titanbreak.analysis"), left + 13, top + 5,
                TitanInterfaceTheme.TEXT, false);

        int jamTicks = TitanClientState.integer("jamTicks", 0);
        if (jamTicks > 0) {
            renderJammed(g, mc, font, left, top, right, bottom, jamTicks);
            return;
        }

        Entity target = mc.crosshairPickEntity;
        int y = top + 22;
        if (target instanceof LivingEntity living) {
            double distance = mc.player.distanceTo(living);
            y = TitanInterfaceTheme.wrapped(g, font, living.getType().getDescription(), left + 8, y,
                    panelWidth - 16, TitanInterfaceTheme.ACCENT, 1) + 2;
            g.horizontalLine(left + 8, right - 8, y, 0x66404B50);
            y += 5;

            if (tactical) {
                g.text(font, Component.translatable("hud.titanbreak.analysis_range", String.format(Locale.ROOT, "%.1f", distance)),
                        left + 8, y, TitanInterfaceTheme.TEXT_MUTED, false);
                y += 12;
                double targetHp = CombatScale.toVisible(living.getHealth());
                double targetMax = Math.max(1.0D, CombatScale.toVisible(living.getMaxHealth()));
                g.text(font, Component.translatable("hud.titanbreak.analysis_health",
                                String.format(Locale.ROOT, "%.0f", targetHp), String.format(Locale.ROOT, "%.0f", targetMax)),
                        left + 8, y, TitanInterfaceTheme.TEXT_MUTED, false);
                y += 11;
                drawSegmentedGauge(g, left + 8, y, panelWidth - 16,
                        Math.max(0.0D, Math.min(1.0D, targetHp / targetMax)), TitanInterfaceTheme.SIGNAL_RED, 14);
                y += 9;
            }

            if (ballistic && y < bottom - 28) {
                double flight = Math.max(0.2D, Math.min(0.6D, distance / 60.0D));
                Vec3 lead = living.getDeltaMovement().scale(flight * 20.0D);
                g.text(font, Component.translatable("hud.titanbreak.analysis_lead",
                                String.format(Locale.ROOT, "%.1f", lead.horizontalDistance())),
                        left + 8, y, TitanInterfaceTheme.BLUE, false);
                y += 12;
            }
            if (targetAssist && distance <= 24.0D && y < bottom - 17) {
                g.text(font, Component.translatable("hud.titanbreak.target_assist_ready"),
                        left + 8, y, TitanInterfaceTheme.GOOD, false);
            }
        } else {
            TitanInterfaceTheme.wrapped(g, font, Component.translatable("hud.titanbreak.analysis_no_target"),
                    left + 8, y + 4, panelWidth - 16, TitanInterfaceTheme.TEXT_MUTED, 3);
        }

        if (thermal && mc.level != null) {
            AABB area = mc.player.getBoundingBox().inflate(24.0D);
            int signatures = mc.level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != mc.player && entity.isAlive()).size();
            Component thermalText = Component.translatable("hud.titanbreak.thermal_signatures", signatures);
            g.text(font, thermalText, left + 8, bottom - 14, TitanInterfaceTheme.ORANGE, false);
            drawContactPips(g, right - 54, bottom - 13, Math.min(5, signatures));
        }
    }

    private static void renderJammed(GuiGraphicsExtractor g, Minecraft mc, Font font,
                                     int left, int top, int right, int bottom, int jamTicks) {
        int phase = mc.player == null ? 0 : Math.floorMod(mc.player.tickCount, 12);
        for (int y = top + 24 + phase; y < bottom - 6; y += 12) {
            g.fill(left + 7, y, right - 7, y + 1, 0xAA7C284E);
        }
        for (int x = left + 11 + phase; x < right - 11; x += 24) {
            g.fill(x, top + 23, x + 1, bottom - 7, 0x5543D7E8);
        }
        Component jammed = Component.translatable("hud.titanbreak.analysis_jammed",
                String.format(Locale.ROOT, "%.1f", jamTicks / 20.0D));
        g.centeredText(font, jammed, (left + right) / 2, top + 52, 0xFFF0749C);
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
        int left = cx - textWidth / 2 - 10;
        int right = cx + textWidth / 2 + 10;
        int top = 8;
        int bottom = 22;
        int pulse = Math.floorMod(mc.player.tickCount, 12) < 6 ? TitanInterfaceTheme.SIGNAL_RED : 0xFF8C343B;

        g.fill(left + 5, top, right, bottom, 0xA51A090C);
        g.fill(left, top + 5, right, bottom, 0xA51A090C);
        g.horizontalLine(left + 5, right, top, pulse);
        g.verticalLine(left, top + 5, bottom, pulse);
        g.centeredText(font, text, cx, top + 3, 0xFFF38D90);
    }

    private static void drawSegmentedGauge(GuiGraphicsExtractor g, int x, int y, int width,
                                           double fraction, int fillColor, int segments) {
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        g.fill(x, y, x + width, y + 5, 0xCC111619);
        int innerWidth = Math.max(1, width - 2);
        int filled = (int) Math.round(innerWidth * fraction);
        if (filled > 0) g.fill(x + 1, y + 1, x + 1 + filled, y + 4, fillColor);
        int step = Math.max(4, innerWidth / Math.max(1, segments));
        for (int sx = x + 1 + step; sx < x + width - 1; sx += step) {
            g.fill(sx, y + 1, sx + 1, y + 4, 0xAA080C0F);
        }
        g.horizontalLine(x, x + width, y + 5, 0x66404B50);
    }

    private static void drawContactPips(GuiGraphicsExtractor g, int x, int y, int count) {
        for (int i = 0; i < 5; i++) {
            int px = x + i * 8;
            int color = i < count ? TitanInterfaceTheme.ORANGE : 0x55404B50;
            g.fill(px, y, px + 4, y + 3, color);
        }
    }

    private static double clamp100(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
