package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Layout-only renderer for verified Kenney CC0 interface components.
 * Decorative shapes come from external artwork; this class only performs nine-slice composition and state binding.
 */
public final class ExternalRpgUi {
    private static final Identifier FANTASY_PANEL = texture("fantasy_panel.png");
    private static final Identifier PANEL_BACKGROUND = texture("panel_background.png");
    private static final Identifier BUTTON_NORMAL = texture("button_normal.png");
    private static final Identifier BUTTON_PRESSED = texture("button_pressed.png");

    private ExternalRpgUi() {
    }

    public static void dimWorld(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0xE20B1016);
    }

    public static void window(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        shadow(graphics, x, y, width, height);
        nineSlice(graphics, PANEL_BACKGROUND, 64, 64, 12,
                x + 7, y + 7, width - 14, height - 14);
        nineSlice(graphics, FANTASY_PANEL, 48, 48, 14,
                x, y, width, height);
    }

    public static void card(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        nineSlice(graphics, PANEL_BACKGROUND, 64, 64, 10, x, y, width, height);
    }

    public static void button(GuiGraphicsExtractor graphics, Font font,
                              int x, int y, int width, int height,
                              String label, boolean selected, boolean hovered, boolean enabled) {
        Identifier texture = selected || hovered ? BUTTON_PRESSED : BUTTON_NORMAL;
        int textureWidth = texture == BUTTON_PRESSED ? 192 : 64;
        nineSlice(graphics, texture, textureWidth, 64, 14, x, y, width, height);
        if (!enabled) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0x9A252A30);
        } else if (selected) {
            graphics.fill(x + 6, y + height - 6, x + width - 6, y + height - 4, 0xFFD9B867);
        }
        int color = enabled ? 0xFFF8EBC9 : 0xFFAAA79F;
        graphics.centeredText(font, Component.literal(label), x + width / 2,
                y + Math.max(5, (height - 8) / 2), color);
    }

    public static void iconButton(GuiGraphicsExtractor graphics, Font font, Item icon,
                                  int x, int y, int width, int height,
                                  String label, boolean selected, boolean hovered) {
        button(graphics, font, x, y, width, height, "", selected, hovered, true);
        int iconX = x + 10;
        int iconY = y + Math.max(4, (height - 16) / 2);
        graphics.fakeItem(new ItemStack(icon), iconX, iconY);
        graphics.text(font, Component.literal(label), x + 32,
                y + Math.max(5, (height - 8) / 2), 0xFFF8EBC9);
    }

    public static void iconFrame(GuiGraphicsExtractor graphics, Item icon, int x, int y, int size) {
        card(graphics, x, y, size, size);
        graphics.pose().pushMatrix();
        float scale = Math.max(1.0F, (size - 10) / 16.0F);
        graphics.pose().translate(x + 5, y + 5);
        graphics.pose().scale(scale, scale);
        graphics.fakeItem(new ItemStack(icon), 0, 0);
        graphics.pose().popMatrix();
    }

    public static void title(GuiGraphicsExtractor graphics, Font font,
                             String title, String subtitle, int x, int y) {
        graphics.text(font, Component.literal(title), x, y, 0xFFF1D28A, true);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.text(font, Component.literal(subtitle), x, y + 13, 0xFFCFBE9C, false);
        }
    }

    public static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, 0xFF6A5338);
        graphics.fill(x + 12, y + 1, x + width - 12, y + 2, 0xFFB99858);
    }

    public static void progress(GuiGraphicsExtractor graphics, Font font,
                                int x, int y, int width,
                                String label, String value, float ratio, int fillColor) {
        graphics.text(font, Component.literal(label), x, y, 0xFF5D4632, false);
        int valueWidth = font.width(value);
        graphics.text(font, Component.literal(value), x + width - valueWidth, y, 0xFF33271E, false);
        card(graphics, x, y + 11, width, 17);
        int inside = Math.max(0, width - 10);
        int filled = Math.round(inside * clamp(ratio));
        if (filled > 0) graphics.fill(x + 5, y + 16, x + 5 + filled, y + 22, fillColor);
    }

    public static void badge(GuiGraphicsExtractor graphics, Font font,
                             int x, int y, String text, int fillColor) {
        int width = Math.max(42, Math.min(160, font.width(text) + 14));
        card(graphics, x, y, width, 20);
        graphics.fill(x + 5, y + 5, x + width - 5, y + 15, fillColor);
        graphics.centeredText(font, Component.literal(text), x + width / 2, y + 6, 0xFFF7EAC9);
    }

    private static void shadow(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x + 6, y + 8, x + width + 6, y + height + 8, 0x8F000000);
    }

    private static void nineSlice(GuiGraphicsExtractor graphics,
                                  Identifier texture, int textureWidth, int textureHeight, int sourceBorder,
                                  int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int destinationBorder = Math.min(sourceBorder, Math.min(width / 2, height / 2));
        int middleWidth = Math.max(0, width - destinationBorder * 2);
        int middleHeight = Math.max(0, height - destinationBorder * 2);
        int sourceMiddleWidth = Math.max(1, textureWidth - sourceBorder * 2);
        int sourceMiddleHeight = Math.max(1, textureHeight - sourceBorder * 2);

        slice(graphics, texture, textureWidth, textureHeight,
                x, y, destinationBorder, destinationBorder,
                0, 0, sourceBorder, sourceBorder);
        slice(graphics, texture, textureWidth, textureHeight,
                x + width - destinationBorder, y, destinationBorder, destinationBorder,
                textureWidth - sourceBorder, 0, sourceBorder, sourceBorder);
        slice(graphics, texture, textureWidth, textureHeight,
                x, y + height - destinationBorder, destinationBorder, destinationBorder,
                0, textureHeight - sourceBorder, sourceBorder, sourceBorder);
        slice(graphics, texture, textureWidth, textureHeight,
                x + width - destinationBorder, y + height - destinationBorder,
                destinationBorder, destinationBorder,
                textureWidth - sourceBorder, textureHeight - sourceBorder,
                sourceBorder, sourceBorder);

        if (middleWidth > 0) {
            slice(graphics, texture, textureWidth, textureHeight,
                    x + destinationBorder, y, middleWidth, destinationBorder,
                    sourceBorder, 0, sourceMiddleWidth, sourceBorder);
            slice(graphics, texture, textureWidth, textureHeight,
                    x + destinationBorder, y + height - destinationBorder,
                    middleWidth, destinationBorder,
                    sourceBorder, textureHeight - sourceBorder,
                    sourceMiddleWidth, sourceBorder);
        }
        if (middleHeight > 0) {
            slice(graphics, texture, textureWidth, textureHeight,
                    x, y + destinationBorder, destinationBorder, middleHeight,
                    0, sourceBorder, sourceBorder, sourceMiddleHeight);
            slice(graphics, texture, textureWidth, textureHeight,
                    x + width - destinationBorder, y + destinationBorder,
                    destinationBorder, middleHeight,
                    textureWidth - sourceBorder, sourceBorder,
                    sourceBorder, sourceMiddleHeight);
        }
        if (middleWidth > 0 && middleHeight > 0) {
            slice(graphics, texture, textureWidth, textureHeight,
                    x + destinationBorder, y + destinationBorder, middleWidth, middleHeight,
                    sourceBorder, sourceBorder, sourceMiddleWidth, sourceMiddleHeight);
        }
    }

    private static void slice(GuiGraphicsExtractor graphics, Identifier texture,
                              int textureWidth, int textureHeight,
                              int x, int y, int width, int height,
                              int u, int v, int sourceWidth, int sourceHeight) {
        if (width <= 0 || height <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y, u, v, width, height,
                sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(
                LivingKingdoms.MOD_ID, "textures/gui/kenney/" + file
        );
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
