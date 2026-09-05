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

    private static final int BATTLE_ACTION_GREEN = 0xFF39D353;
    private static final int BATTLE_ACTION_GREEN_SOFT = 0xFF76E58A;

    private TurnboundUiSkin() {}

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        stretch(graphics, PANEL_BROWN, x, y, width, height);
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
        if (active && hovered) graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x22FFFFFF);
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

    /**
     * Battle-only action row. The Kenney grey frame supplies a consistent authored edge, while the interior follows
     * the supplied reference's dark-neutral list + one strong green selected state. This is intentionally separate
     * from generic menu buttons so the rest of TURNBOUND's UI does not inherit combat-specific styling.
     */
    static void battleSkillButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                  boolean active, boolean hovered, boolean selected) {
        stretch(graphics, BUTTON_GREY, x, y, width, height);
        int inner = !active ? 0xE0282B31 : selected ? 0xE01D9C3B : 0xE014181D;
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, inner);

        int edge = !active ? 0xFF5C6168 : selected ? BATTLE_ACTION_GREEN_SOFT : 0xFF5B6570;
        graphics.fill(x + 4, y + 3, x + 6, y + height - 3, edge);

        if (selected && active) {
            graphics.fill(x + 6, y + 3, x + width - 4, y + 5, BATTLE_ACTION_GREEN);
            graphics.fill(x + 6, y + height - 5, x + width - 4, y + height - 3, 0xFF2EBA49);
        }
        if (active && hovered) {
            graphics.fill(x + 6, y + 5, x + width - 4, y + height - 5,
                    selected ? 0x18FFFFFF : 0x1639D353);
        }
    }

    static void check(GuiGraphicsExtractor graphics, int x, int y, int size) {
        stretch(graphics, CHECK_BLUE, x, y, size, Math.max(1, size * 15 / 16));
    }

    private static void stretch(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        // This normalized-UV overload takes x0/y0/x1/y1, not width/height.
        graphics.blit(texture, x, y, x + width, y + height, 0.0F, 1.0F, 0.0F, 1.0F);
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
