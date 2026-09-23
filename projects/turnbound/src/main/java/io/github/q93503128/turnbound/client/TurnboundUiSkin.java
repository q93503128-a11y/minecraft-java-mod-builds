package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Production management/map skin backed directly by Foozle RPG UI Set 1 (CC0).
 *
 * <p>TURNBOUND supplies data and navigation. The visible panel, button and orb chrome is external artwork,
 * avoiding the Minecraft-button / AI-card look that failed the first real-client survey.</p>
 */
final class TurnboundUiSkin {
    private static final Identifier PANEL = foozle("panel_1.png");
    private static final Identifier BUTTON = foozle("button.png");
    private static final Identifier ORB_BASE = foozle("main_button_bg.png");
    private static final Identifier ORB = foozle("main_button_overlay.png");
    private static final Identifier ORB_LIGHT = foozle("main_button_overlay_light.png");
    private static final Identifier ORB_DARK = foozle("main_button_overlay_dark.png");
    private static final Identifier CHECK_BLUE = Identifier.fromNamespaceAndPath(
            Turnbound.MOD_ID, "textures/gui/kenney/icon_check_blue.png");

    private static final int BATTLE_ACTION_GREEN = 0xFF39D353;
    private static final int BATTLE_ACTION_GREEN_SOFT = 0xFF76E58A;

    private TurnboundUiSkin() {}

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANEL, x, y, width, height, 18, 18, 0.12F, 0.88F, 0.16F, 0.84F);
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        nineSlice(graphics, BUTTON, x, y, width, height, 8, 7, 0.18F, 0.82F, 0.22F, 0.78F);
    }

    static void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       boolean active, boolean hovered, boolean selected, int accent) {
        nineSlice(graphics, BUTTON, x, y, width, height, 8, 7, 0.18F, 0.82F, 0.22F, 0.78F);
        if (!active) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0x72000000);
        } else if (selected) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, (accent & 0x00FFFFFF) | 0x36000000);
        } else if (hovered) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0x20FFFFFF);
        }
    }

    static void orbBase(GuiGraphicsExtractor graphics, int x, int y, int size) {
        stretch(graphics, ORB_BASE, x, y, size, size);
    }

    static void orbOverlay(GuiGraphicsExtractor graphics, int x, int y, int size,
                           boolean active, boolean hovered, boolean selected) {
        Identifier texture = !active ? ORB_DARK : hovered || selected ? ORB_LIGHT : ORB;
        stretch(graphics, texture, x, y, size, size);
    }

    static void battleSkillButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                  boolean active, boolean hovered, boolean selected) {
        nineSlice(graphics, BUTTON, x, y, width, height, 8, 7, 0.18F, 0.82F, 0.22F, 0.78F);
        int inner = !active ? 0xA8000000 : selected ? 0x8020A43C : 0x38101418;
        graphics.fill(x + 6, y + 6, x + width - 6, y + height - 6, inner);
        if (selected && active) {
            graphics.fill(x + 7, y + height - 6, x + width - 7, y + height - 4, BATTLE_ACTION_GREEN);
        } else if (active && hovered) {
            graphics.fill(x + 7, y + 6, x + width - 7, y + 8, BATTLE_ACTION_GREEN_SOFT);
        }
    }

    static void check(GuiGraphicsExtractor graphics, int x, int y, int size) {
        stretch(graphics, CHECK_BLUE, x, y, size, Math.max(1, size * 15 / 16));
    }

    private static void nineSlice(GuiGraphicsExtractor graphics, Identifier texture,
                                  int x, int y, int width, int height,
                                  int horizontalBorder, int verticalBorder,
                                  float uLeft, float uRight, float vTop, float vBottom) {
        if (width < 12 || height < 12) {
            stretch(graphics, texture, x, y, width, height);
            return;
        }
        int bx = Math.min(horizontalBorder, Math.max(1, width / 2 - 1));
        int by = Math.min(verticalBorder, Math.max(1, height / 2 - 1));
        int x1 = x + bx, x2 = x + width - bx;
        int y1 = y + by, y2 = y + height - by;

        region(graphics, texture, x, y, x1, y1, 0.0F, uLeft, 0.0F, vTop);
        region(graphics, texture, x1, y, x2, y1, uLeft, uRight, 0.0F, vTop);
        region(graphics, texture, x2, y, x + width, y1, uRight, 1.0F, 0.0F, vTop);

        region(graphics, texture, x, y1, x1, y2, 0.0F, uLeft, vTop, vBottom);
        region(graphics, texture, x1, y1, x2, y2, uLeft, uRight, vTop, vBottom);
        region(graphics, texture, x2, y1, x + width, y2, uRight, 1.0F, vTop, vBottom);

        region(graphics, texture, x, y2, x1, y + height, 0.0F, uLeft, vBottom, 1.0F);
        region(graphics, texture, x1, y2, x2, y + height, uLeft, uRight, vBottom, 1.0F);
        region(graphics, texture, x2, y2, x + width, y + height, uRight, 1.0F, vBottom, 1.0F);
    }

    private static void region(GuiGraphicsExtractor graphics, Identifier texture,
                               int x0, int y0, int x1, int y1,
                               float u0, float u1, float v0, float v1) {
        if (x1 <= x0 || y1 <= y0) return;
        graphics.blit(texture, x0, y0, x1, y1, u0, u1, v0, v1);
    }

    private static void stretch(GuiGraphicsExtractor graphics, Identifier texture,
                                int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.blit(texture, x, y, x + width, y + height, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static Identifier foozle(String file) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "textures/gui/foozle/" + file);
    }
}
