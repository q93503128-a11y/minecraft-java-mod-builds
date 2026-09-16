package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishRarity;
import dev.moonseungjun.fishinggame.fishing.FishSizeGrade;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class FishingUiTheme {
    public static final Identifier PANEL = FishingGameMod.id("textures/gui/kenney/grey_panel.png");
    public static final Identifier SLIDER = FishingGameMod.id("textures/gui/kenney/grey_slider_horizontal.png");

    public static final int TEXT_PRIMARY = 0xFF27343D;
    public static final int TEXT_SECONDARY = 0xFF4F5D67;
    public static final int TEXT_MUTED = 0xFF6F7B84;
    public static final int TEXT_DISABLED = 0xFF8B949C;
    public static final int ACCENT = 0xFF2678B8;
    public static final int MONEY = 0xFF9B6900;
    public static final int SUCCESS = 0xFF2D7B49;
    public static final int WARNING = 0xFFB05B00;
    public static final int DANGER = 0xFFC54242;
    public static final int BORDER = 0x665D6973;

    public static final int OVERLAY_PRIMARY = 0xFFF7FBFF;
    public static final int OVERLAY_ACCENT = 0xFF86C5FF;
    public static final int OVERLAY_MONEY = 0xFFFFD86B;
    public static final int OVERLAY_SUCCESS = 0xFFB8FFCF;
    public static final int OVERLAY_WARNING = 0xFFFFB264;
    public static final int OVERLAY_DANGER = 0xFFFF9C9C;

    private static final int PANEL_TILE = 100;
    // Kenney's bundled grey panel only uses the outer four source pixels for its frame.
    // Sampling twelve pixels, as alpha.20 did, made every GUI frame visually three times heavier than the asset itself.
    private static final int PANEL_BORDER = 4;
    private static final int PANEL_CENTER = PANEL_TILE - PANEL_BORDER * 2;

    private FishingUiTheme() {
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int columns, int rows) {
        drawPanelPixels(graphics, x, y, columns * PANEL_TILE, rows * PANEL_TILE);
    }

    public static void drawPanelPixels(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int safeWidth = Math.max(PANEL_BORDER * 2, width);
        int safeHeight = Math.max(PANEL_BORDER * 2, height);
        int innerWidth = safeWidth - PANEL_BORDER * 2;
        int innerHeight = safeHeight - PANEL_BORDER * 2;
        int sourceFar = PANEL_TILE - PANEL_BORDER;

        blit(graphics, x, y, 0, 0, PANEL_BORDER, PANEL_BORDER);
        blit(graphics, x + safeWidth - PANEL_BORDER, y, sourceFar, 0, PANEL_BORDER, PANEL_BORDER);
        blit(graphics, x, y + safeHeight - PANEL_BORDER, 0, sourceFar, PANEL_BORDER, PANEL_BORDER);
        blit(graphics, x + safeWidth - PANEL_BORDER, y + safeHeight - PANEL_BORDER,
                sourceFar, sourceFar, PANEL_BORDER, PANEL_BORDER);

        tile(graphics, x + PANEL_BORDER, y, innerWidth, PANEL_BORDER,
                PANEL_BORDER, 0, PANEL_CENTER, PANEL_BORDER);
        tile(graphics, x + PANEL_BORDER, y + safeHeight - PANEL_BORDER, innerWidth, PANEL_BORDER,
                PANEL_BORDER, sourceFar, PANEL_CENTER, PANEL_BORDER);
        tile(graphics, x, y + PANEL_BORDER, PANEL_BORDER, innerHeight,
                0, PANEL_BORDER, PANEL_BORDER, PANEL_CENTER);
        tile(graphics, x + safeWidth - PANEL_BORDER, y + PANEL_BORDER, PANEL_BORDER, innerHeight,
                sourceFar, PANEL_BORDER, PANEL_BORDER, PANEL_CENTER);
        tile(graphics, x + PANEL_BORDER, y + PANEL_BORDER, innerWidth, innerHeight,
                PANEL_BORDER, PANEL_BORDER, PANEL_CENTER, PANEL_CENTER);
    }

    private static void tile(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        for (int dy = 0; dy < height; dy += sourceHeight) {
            int drawHeight = Math.min(sourceHeight, height - dy);
            for (int dx = 0; dx < width; dx += sourceWidth) {
                int drawWidth = Math.min(sourceWidth, width - dx);
                blit(graphics, x + dx, y + dy, sourceX, sourceY, drawWidth, drawHeight);
            }
        }
    }

    private static void blit(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int sourceX,
            int sourceY,
            int width,
            int height
    ) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PANEL,
                x,
                y,
                sourceX,
                sourceY,
                width,
                height,
                PANEL_TILE,
                PANEL_TILE
        );
    }

    public static void drawHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            String title,
            String subtitle,
            int panelX,
            int panelY,
            int panelWidth
    ) {
        graphics.centeredText(font, title, panelX + panelWidth / 2, panelY + 8, TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.centeredText(font, subtitle, panelX + panelWidth / 2, panelY + 20, TEXT_SECONDARY);
        }
        graphics.fill(panelX + 10, panelY + 34, panelX + panelWidth - 10, panelY + 35, BORDER);
    }

    public static void drawSectionTitle(GuiGraphicsExtractor graphics, Font font, String label, int x, int y) {
        graphics.fill(x, y + 1, x + 2, y + 10, ACCENT);
        graphics.text(font, label, x + 6, y, TEXT_PRIMARY, false);
    }

    public static int rarityColor(FishRarity rarity) {
        return switch (rarity) {
            case COMMON -> TEXT_PRIMARY;
            case UNCOMMON -> 0xFF2F834A;
            case RARE -> ACCENT;
            case EPIC -> 0xFF7550B4;
            case LEGENDARY -> MONEY;
        };
    }

    public static int sizeGradeColor(FishSizeGrade grade) {
        return switch (grade) {
            case STANDARD -> TEXT_SECONDARY;
            case LARGE -> 0xFF2F834A;
            case TROPHY -> ACCENT;
            case MONSTER -> MONEY;
        };
    }
}
