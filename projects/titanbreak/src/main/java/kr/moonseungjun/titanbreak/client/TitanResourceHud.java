package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class TitanResourceHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "resources");

    private TitanResourceHud() {}

    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, TitanResourceHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;

        double power = Math.max(0.0D, TitanClientState.decimal("power", 100.0D));
        double powerCap = Math.max(1.0D, TitanClientState.decimal("powerCap", 100.0D));
        double heat = clamp01(TitanClientState.decimal("heat", 0.0D) / 100.0D);
        double neuralLoad = Math.max(0.0D, TitanClientState.decimal("neuralLoad", 0.0D));
        double neuralCap = Math.max(1.0D, TitanClientState.decimal("neuralCap", 100.0D));
        double neural = neuralLoad / neuralCap;
        boolean active = TitanClientState.flag("active");
        int integrityWorst = Math.max(0, Math.min(3, TitanClientState.integer("integrityWorst", 0)));
        int integrityDamaged = Math.max(0, TitanClientState.integer("integrityDamaged", 0));

        double powerFraction = clamp01(power / powerCap);
        boolean showPower = active || powerFraction < 0.995D;
        boolean showHeat = heat > 0.01D;
        boolean showNeural = neural >= 0.70D;
        boolean showIntegrity = integrityWorst > 0 && integrityDamaged > 0;
        if (!showPower && !showHeat && !showNeural && !showIntegrity) return;

        int x = 13;
        int y = 63;

        if (showPower) {
            drawThinGauge(graphics, x, y, 96, powerFraction, TitanInterfaceTheme.CYAN);
            y += 6;
        }

        if (showHeat) {
            int width = heat <= 0.40D ? 64 : 96;
            int color = heat >= 0.70D ? TitanInterfaceTheme.SIGNAL_RED : TitanInterfaceTheme.ACCENT;
            drawThinGauge(graphics, x, y, width, heat, color);
            y += 6;
        }

        if (showNeural) {
            int color = neural >= 1.0D ? TitanInterfaceTheme.SIGNAL_RED : 0xFFB46EEA;
            drawNeuralWarning(graphics, x, y, neural, color);
            y += 10;
        }

        if (showIntegrity) drawIntegrityWarning(graphics, x, y, integrityWorst, integrityDamaged);
    }

    private static void drawThinGauge(GuiGraphicsExtractor graphics, int x, int y, int width,
                                      double fraction, int color) {
        graphics.fill(x, y, x + width, y + 3, 0xA8111619);
        int filled = (int) Math.round(width * clamp01(fraction));
        if (filled > 0) graphics.fill(x, y, x + filled, y + 3, color);
        graphics.horizontalLine(x, x + width, y + 3, 0x66404B50);
    }

    private static void drawNeuralWarning(GuiGraphicsExtractor graphics, int x, int y,
                                          double fraction, int color) {
        int lit = Math.max(1, Math.min(5, (int) Math.ceil(Math.min(1.0D, fraction) * 5.0D)));
        for (int i = 0; i < 5; i++) {
            int px = x + i * 7;
            graphics.fill(px, y, px + 4, y + 4, i < lit ? color : 0x55404B50);
        }
        if (fraction > 1.0D) graphics.horizontalLine(x, x + 32, y + 6, color);
    }

    private static void drawIntegrityWarning(GuiGraphicsExtractor graphics, int x, int y,
                                             int worst, int damagedCount) {
        int color = worst >= 3 ? TitanInterfaceTheme.SIGNAL_RED
                : worst == 2 ? 0xFFFF8A3D : TitanInterfaceTheme.ACCENT;
        int lit = Math.max(1, Math.min(3, worst));
        for (int i = 0; i < 3; i++) {
            int px = x + i * 8;
            graphics.fill(px, y, px + 5, y + 5, i < lit ? color : 0x44373E41);
        }
        int countMarks = Math.min(5, damagedCount);
        for (int i = 0; i < countMarks; i++) {
            int px = x + 29 + i * 4;
            graphics.fill(px, y + 1, px + 2, y + 4, color);
        }
        if (worst >= 3) graphics.horizontalLine(x, x + 47, y + 7, color);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
