package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared client-side visual language for combat HUDs and command surfaces.
 * The palette keeps the battlefield readable without turning every panel into a bright rectangle.
 */
public final class VillageDefenseUiTheme {
    public static final int BACKDROP = 0x8A05090D;
    public static final int PANEL = 0xE80B1319;
    public static final int PANEL_SOFT = 0xD8121D24;
    public static final int PANEL_ACTIVE = 0xEE172730;
    public static final int EDGE = 0xA33D5661;
    public static final int EDGE_SOFT = 0x66445C66;
    public static final int TEXT = 0xFFF2F6F7;
    public static final int MUTED = 0xFF98A8AF;
    public static final int CYAN = 0xFF55DCC5;
    public static final int GOLD = 0xFFF4C55F;
    public static final int GREEN = 0xFF70D69B;
    public static final int AMBER = 0xFFF2A65A;
    public static final int RED = 0xFFE86562;
    public static final int BLUE = 0xFF77AEEA;
    public static final int TRACK = 0xFF26343A;

    private VillageDefenseUiTheme() {}

    public static void panel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 1, EDGE);
        graphics.fill(left, bottom - 1, right, bottom, EDGE_SOFT);
    }

    public static void card(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int accent,
            boolean active) {
        graphics.fill(left, top, right, bottom, active ? PANEL_ACTIVE : PANEL_SOFT);
        graphics.fill(left, top, left + 3, bottom, accent);
        graphics.fill(left + 3, bottom - 1, right, bottom, EDGE_SOFT);
    }

    public static void progressBar(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int current,
            int maximum,
            int accent) {
        graphics.fill(left, top, right, bottom, TRACK);
        int safeMaximum = Math.max(1, maximum);
        int safeCurrent = Math.max(0, Math.min(current, safeMaximum));
        int fill = left + Math.round((right - left) * safeCurrent / (float) safeMaximum);
        if (fill > left) graphics.fill(left, top, fill, bottom, accent);
    }

    public static int integrityColor(int current, int maximum) {
        float ratio = maximum <= 0 ? 0.0f : current / (float) maximum;
        if (ratio <= 0.25f) return RED;
        if (ratio <= 0.48f) return AMBER;
        if (ratio <= 0.72f) return GOLD;
        return GREEN;
    }

    public static int pressureColor(int count) {
        if (count <= 0) return MUTED;
        if (count <= 3) return CYAN;
        if (count <= 7) return GOLD;
        if (count <= 12) return AMBER;
        return RED;
    }

    public static void pip(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int width,
            int count) {
        int color = pressureColor(count);
        graphics.fill(left, top, left + width, top + 3, TRACK);
        if (count > 0) {
            int fill = Math.max(4, Math.min(width, 4 + count * 3));
            graphics.fill(left, top, left + fill, top + 3, color);
        }
    }
}
