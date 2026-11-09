package kr.moonseungjun.anomalousclasses.client;

import java.util.List;
import java.util.function.Consumer;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class MenuUi {
    private static final int PANEL = 0xE91A1720;
    private static final int INNER = 0xE90F0D13;
    private static final ResourceLocation[] CLASS_ICONS = new ResourceLocation[ClassId.values().length];
    private static final ResourceLocation[] CLASS_ART = new ResourceLocation[ClassId.values().length];
    private static final int[] ACCENTS = {0xFFC69AE9, 0xFFD6A36D, 0xFF8CBF82, 0xFF70BFD5, 0xFFD0B47C, 0xFFB4C875};

    static {
        for (ClassId id : ClassId.values()) {
            CLASS_ICONS[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "textures/gui/class_icons_v2/" + id.key() + ".png");
            CLASS_ART[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "textures/gui/class_cards_v3/" + id.key() + ".png");
        }
    }

    private MenuUi() {}

    static int accent(ClassId id) { return ACCENTS[id.networkIndex()]; }
    static ResourceLocation classIcon(ClassId id) { return CLASS_ICONS[id.networkIndex()]; }
    static ResourceLocation classArt(ClassId id) { return CLASS_ART[id.networkIndex()]; }

    static void drawPanel(GuiGraphics graphics, Box box, int accent) {
        graphics.fill(box.x(), box.y(), box.right(), box.bottom(), PANEL);
        graphics.fill(box.x() + 4, box.y() + 4, box.right() - 4, box.bottom() - 4, INNER);
        graphics.fill(box.x() + 4, box.y(), box.right() - 4, box.y() + 1, 0xFF9A815F);
        graphics.fill(box.x() + 4, box.bottom() - 1, box.right() - 4, box.bottom(), 0xFF9A815F);
        graphics.fill(box.x(), box.y() + 4, box.x() + 1, box.bottom() - 4, 0xFF9A815F);
        graphics.fill(box.right() - 1, box.y() + 4, box.right(), box.bottom() - 4, 0xFF9A815F);
        graphics.fill(box.x() + 4, box.y() + 4, box.x() + 8, box.y() + 6, accent);
        graphics.fill(box.right() - 8, box.y() + 4, box.right() - 4, box.y() + 6, accent);
        graphics.fill(box.x() + 4, box.bottom() - 6, box.x() + 8, box.bottom() - 4, accent);
        graphics.fill(box.right() - 8, box.bottom() - 6, box.right() - 4, box.bottom() - 4, accent);
    }

    static void drawClassIcon(GuiGraphics graphics, ClassId id, int x, int y, int size) {
        graphics.blit(classIcon(id), x, y, size, size, 0, 0, 96, 96, 96, 96);
    }

    static void drawArtCover(GuiGraphics graphics, ClassId id, Box box) {
        if (box.width() < 1 || box.height() < 1) return;
        float sourceAspect = 16.0f / 9.0f;
        float targetAspect = box.width() / (float) box.height();
        float u = 0, v = 0;
        int sourceWidth = 512, sourceHeight = 288;
        if (targetAspect < sourceAspect) {
            sourceWidth = Math.max(1, Math.round(288 * targetAspect));
            u = (512 - sourceWidth) * 0.5f;
        } else {
            sourceHeight = Math.max(1, Math.round(512 / targetAspect));
            v = (288 - sourceHeight) * 0.5f;
        }
        graphics.blit(classArt(id), box.x(), box.y(), box.width(), box.height(), u, v,
                sourceWidth, sourceHeight, 512, 288);
    }

    static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y,
                             int maxWidth, int color) {
        float scale = Math.min(1.0f, maxWidth / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    static void drawFit(GuiGraphics graphics, Font font, Component text, int x, int y,
                        int maxWidth, int color) {
        float scale = Math.min(1.0f, maxWidth / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    static void drawWrapped(GuiGraphics graphics, Font font, Component text, int x, int y,
                            int maxWidth, int maxLines, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, Math.max(1, maxWidth));
        int count = Math.min(Math.max(0, maxLines), lines.size());
        for (int index = 0; index < count; index++) graphics.drawString(font, lines.get(index), x, y + index * font.lineHeight, color, false);
    }

    static void drawProgressDivider(GuiGraphics graphics, int x, int y, int width, int accent) {
        graphics.fill(x, y, x + width, y + 1, 0xFF51445A);
        graphics.fill(x + width / 4, y, x + width * 3 / 4, y + 1, accent);
    }

    static final class Box {
        private final int x, y, width, height;
        Box(int x, int y, int width, int height) { this.x=x; this.y=y; this.width=width; this.height=height; }
        int x() { return x; } int y() { return y; } int width() { return width; } int height() { return height; }
        int right() { return x + width; } int bottom() { return y + height; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }

    static final class Button extends AbstractButton {
        private final Consumer<Button> action;
        private final int accent;

        Button(Box box, Component label, int accent, Consumer<Button> action) {
            super(box.x(), box.y(), box.width(), box.height(), label);
            this.accent = accent;
            this.action = action;
        }

        @Override public void onPress() { action.accept(this); }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int edge = !active ? 0xFF675F5D : (hovered ? 0xFFE7C994 : 0xFFB99A6F);
            int fill = !active ? 0xD21A1820 : (hovered ? 0xF02D2437 : 0xE51E1A27);
            graphics.fill(getX() + 3, getY(), getX() + width - 3, getY() + height, fill);
            graphics.fill(getX(), getY() + 3, getX() + width, getY() + height - 3, fill);
            graphics.fill(getX() + 3, getY(), getX() + width - 3, getY() + 1, edge);
            graphics.fill(getX() + 3, getY() + height - 1, getX() + width - 3, getY() + height, edge);
            graphics.fill(getX(), getY() + 3, getX() + 1, getY() + height - 3, edge);
            graphics.fill(getX() + width - 1, getY() + 3, getX() + width, getY() + height - 3, edge);
            graphics.fill(getX() + 5, getY() + 2, getX() + 8, getY() + 3, accent);
            graphics.fill(getX() + width - 8, getY() + 2, getX() + width - 5, getY() + 3, accent);
            int available = Math.max(1, width - 10);
            drawCentered(graphics, Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + Math.max(1, (height - Minecraft.getInstance().font.lineHeight) / 2), available,
                    active ? 0xFFF1E9DB : 0xFFAAA39B);
        }
    }
}
