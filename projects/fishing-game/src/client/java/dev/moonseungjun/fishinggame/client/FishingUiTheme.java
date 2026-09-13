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
    public static final Identifier BUTTON = FishingGameMod.id("textures/gui/kenney/blue_button00.png");

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFC7CDD3;
    public static final int TEXT_MUTED = 0xFF8D959D;
    public static final int TEXT_DISABLED = 0xFF777777;
    public static final int ACCENT = 0xFF86C5FF;
    public static final int MONEY = 0xFFFFD86B;
    public static final int SUCCESS = 0xFFB8FFCF;
    public static final int WARNING = 0xFFFFB264;
    public static final int DANGER = 0xFFFF9C9C;
    public static final int BORDER = 0x557A8792;

    private static final int PANEL_TILE = 100;

    private FishingUiTheme() {
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        PANEL,
                        x + col * PANEL_TILE,
                        y + row * PANEL_TILE,
                        0,
                        0,
                        PANEL_TILE,
                        PANEL_TILE,
                        PANEL_TILE,
                        PANEL_TILE
                );
            }
        }
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
        graphics.centeredText(font, title, panelX + panelWidth / 2, panelY + 12, TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.centeredText(font, subtitle, panelX + panelWidth / 2, panelY + 27, TEXT_SECONDARY);
        }
        graphics.fill(panelX + 12, panelY + 42, panelX + panelWidth - 12, panelY + 43, BORDER);
    }

    public static void drawSectionTitle(GuiGraphicsExtractor graphics, Font font, String label, int x, int y) {
        graphics.fill(x, y + 1, x + 2, y + 11, ACCENT);
        graphics.text(font, label, x + 7, y, TEXT_PRIMARY, true);
    }

    public static int rarityColor(FishRarity rarity) {
        return switch (rarity) {
            case COMMON -> TEXT_PRIMARY;
            case UNCOMMON -> 0xFF8EF3A0;
            case RARE -> ACCENT;
            case EPIC -> 0xFFC697FF;
            case LEGENDARY -> 0xFFFFCF66;
        };
    }

    public static int sizeGradeColor(FishSizeGrade grade) {
        return switch (grade) {
            case STANDARD -> 0xFFD7DCE1;
            case LARGE -> 0xFF8EF3A0;
            case TROPHY -> ACCENT;
            case MONSTER -> 0xFFFFCF66;
        };
    }
}
