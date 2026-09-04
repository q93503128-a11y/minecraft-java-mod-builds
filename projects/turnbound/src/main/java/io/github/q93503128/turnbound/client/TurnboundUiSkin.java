package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Runtime skin primitives backed by Kenney UI Adventure Pack (CC0).
 * TURNBOUND owns layout/information hierarchy; the frame/button pixel design comes from the external pack.
 */
final class TurnboundUiSkin {
    private static final Identifier PANEL_BROWN = id("panel_brown.png");
    private static final Identifier PANEL_INSET = id("panel_inset_beige_light.png");
    private static final Identifier BUTTON_BLUE = id("button_long_blue.png");
    private static final Identifier BUTTON_BROWN = id("button_long_brown.png");
    private static final Identifier BUTTON_GREY = id("button_long_grey.png");
    private static final Identifier CHECK_BLUE = id("icon_check_blue.png");

    private TurnboundUiSkin() {}

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        stretch(graphics, PANEL_BROWN, x, y, width, height);
        // Kenney supplies the silhouette and border language; this veil keeps small Minecraft font legible.
        graphics.fill(x + 7, y + 7, x + width - 7, y + height - 7, 0xB80A0C10);
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        stretch(graphics, PANEL_INSET, x, y, width, height);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xB0181614);
    }

    static void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       boolean active, boolean hovered, boolean selected, int accent) {
        Identifier texture = !active ? BUTTON_GREY : warm(accent) ? BUTTON_BROWN : BUTTON_BLUE;
        stretch(graphics, texture, x, y, width, height);

        // Hover is intentionally transient and soft. It must not look like a committed selection.
        if (active && hovered) {
            graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x22FFFFFF);
        }

        // Selection uses both colour and shape/icon so state is not communicated by colour alone.
        if (selected) {
            int stateColor = active ? accent : 0xFF777777;
            graphics.fill(x + 4, y + 3, x + 6, y + height - 3, stateColor);
            graphics.fill(x + 6, y + height - 5, x + width - 5, y + height - 3, stateColor);
            if (width >= 36 && height >= 16) {
                int mark = Math.min(8, height - 8);
                check(graphics, x + width - mark - 4, y + 4, mark);
            }
        }
    }

    static void check(GuiGraphicsExtractor graphics, int x, int y, int size) {
        stretch(graphics, CHECK_BLUE, x, y, size, Math.max(1, size * 15 / 16));
    }

    private static void stretch(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        // 26.2 blit uses x/y plus width/height. Passing x+width/y+height made every later widget grow
        // with its screen position and is what caused the visible right-edge spill in the E menu.
        graphics.blit(texture, x, y, width, height, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static boolean warm(int accent) {
        int r = (accent >>> 16) & 0xFF;
        int g = (accent >>> 8) & 0xFF;
        int b = accent & 0xFF;
        return r > b + 20 && g > b - 10;
    }

    private static Identifier id(String file) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "textures/gui/kenney/" + file);
    }
}
