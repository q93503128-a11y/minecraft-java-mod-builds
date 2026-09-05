package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Runtime skin primitives backed by Kenney UI Adventure Pack (CC0).
 * TURNBOUND owns information hierarchy, tokens and spacing; Kenney supplies low-level frame/button pixels.
 *
 * <p>The source textures are now sliced rather than stretched wholesale. This preserves corner weight and
 * border thickness on wide PC panels/buttons and avoids the warped "rubber rectangle" look that the shared
 * quality standard explicitly warns against.</p>
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
        nineSlice(graphics, PANEL_BROWN, x, y, width, height, 10, 10, 0.12F, 0.88F, 0.12F, 0.88F);
        int inset = Math.min(TurnboundUiTokens.S, Math.max(4, Math.min(width, height) / 4));
        if (width > inset * 2 && height > inset * 2) {
            graphics.fill(x + inset, y + inset, x + width - inset, y + height - inset, 0xB80A0C10);
        }
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANEL_INSET, x, y, width, height, 8, 8, 0.11F, 0.89F, 0.11F, 0.89F);
        int inset = Math.min(6, Math.max(3, Math.min(width, height) / 4));
        if (width > inset * 2 && height > inset * 2) {
            graphics.fill(x + inset, y + inset, x + width - inset, y + height - inset, 0xB0181614);
        }
    }

    static void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       boolean active, boolean hovered, boolean selected, int accent) {
        Identifier texture = !active ? BUTTON_GREY : warm(accent) ? BUTTON_BROWN : BUTTON_BLUE;
        nineSlice(graphics, texture, x, y, width, height, 9, 5, 0.09F, 0.91F, 0.20F, 0.80F);
        if (!active) {
            graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x26000000);
        } else if (hovered) {
            graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x22FFFFFF);
            graphics.fill(x + 6, y + 3, x + width - 6, y + 4, 0x32FFFFFF);
        }
        if (selected) {
            int stateColor = active ? accent : TurnboundUiTokens.DISABLED;
            graphics.fill(x + 4, y + 3, x + 6, y + height - 3, stateColor);
            graphics.fill(x + 6, y + height - 5, x + width - 5, y + height - 3, stateColor);
            // Selection is communicated by both color and a geometric corner mark.
            if (width >= 34 && height >= 16) {
                int mx = x + width - 10, my = y + 5;
                graphics.fill(mx, my, mx + 6, my + 2, stateColor);
                graphics.fill(mx + 4, my, mx + 6, my + 7, stateColor);
            }
        }
    }

    /**
     * Battle-only action row. The Kenney grey frame supplies a consistent authored edge, while the interior follows
     * the supplied reference's dark-neutral list + one strong green selected state. This stays separate from generic
     * menu buttons so management screens do not inherit combat-specific styling.
     */
    static void battleSkillButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                  boolean active, boolean hovered, boolean selected) {
        nineSlice(graphics, BUTTON_GREY, x, y, width, height, 9, 5, 0.09F, 0.91F, 0.20F, 0.80F);
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

    private static void nineSlice(GuiGraphicsExtractor graphics, Identifier texture,
                                  int x, int y, int width, int height,
                                  int horizontalBorder, int verticalBorder,
                                  float uLeft, float uRight, float vTop, float vBottom) {
        if (width < 8 || height < 8) {
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
