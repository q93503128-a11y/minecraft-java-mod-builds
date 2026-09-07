package kr.moonseungjun.frontiersettlement.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared Frontier Settlement UI tokens.
 *
 * Keep the visual language restrained and settlement-focused: warm charcoal surfaces, brass accent,
 * explicit semantic state colors and one spacing scale. Individual screens should not invent a new
 * palette or arbitrary padding unless the component genuinely needs a different role.
 */
public final class FrontierUiTheme {
    public static final int BACKGROUND = 0xF21B1916;
    public static final int SURFACE = 0xF0292621;
    public static final int SURFACE_ELEVATED = 0xF238332B;
    public static final int SURFACE_SOFT = 0xD922201D;

    public static final int PRIMARY = 0xFFD3A34F;
    public static final int PRIMARY_SOFT = 0xFF8F6E37;
    public static final int SECONDARY = 0xFF74806B;
    public static final int ACCENT = 0xFFF0C878;

    public static final int SUCCESS = 0xFF91C783;
    public static final int WARNING = 0xFFE7B666;
    public static final int DANGER = 0xFFD98272;
    public static final int DISABLED = 0xFF777168;

    public static final int TEXT_PRIMARY = 0xFFF3EEE5;
    public static final int TEXT_SECONDARY = 0xFFC1B8AA;
    public static final int TEXT_MUTED = 0xFF938B80;
    public static final int BORDER = 0xFF665B4E;
    public static final int DIVIDER = 0xB0504940;
    public static final int TRACK = 0xFF332F2A;

    public static final int XS = 4;
    public static final int S = 8;
    public static final int M = 12;
    public static final int L = 16;
    public static final int XL = 24;

    private FrontierUiTheme() {}

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    public static void surface(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SURFACE);
        graphics.fill(x, y, x + 2, y + height, PRIMARY_SOFT);
    }

    public static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIVIDER);
    }

    public static void progress(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                int current, int maximum) {
        int safeMaximum = Math.max(1, maximum);
        int safeCurrent = Math.max(0, Math.min(current, safeMaximum));
        graphics.fill(x, y, x + width, y + height, TRACK);
        int fill = Math.round(width * (safeCurrent / (float) safeMaximum));
        if (fill > 0) graphics.fill(x, y, x + fill, y + height, PRIMARY);
    }
}
