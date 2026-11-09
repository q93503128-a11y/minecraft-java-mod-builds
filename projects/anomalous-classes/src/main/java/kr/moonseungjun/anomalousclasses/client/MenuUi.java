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
import net.minecraft.util.FormattedCharSequence;

final class MenuUi {
    private static final ResourceLocation PANEL_ATLAS = ui("frame_atlas");
    private static final ResourceLocation TITLE_PLATE = ui("title_plate");
    private static final ResourceLocation SURFACE_TILE = ui("surface_tile");
    private static final ResourceLocation BUTTON_STATES = ui("button_states");
    private static final ResourceLocation NODE_STATES = ui("node_states");
    private static final ResourceLocation CONNECTION_PATHS = ui("connection_paths");
    private static final ResourceLocation PROGRESS_STATES = ui("progress_states");
    private static final int[] ACCENTS = {0xFFC69AE9, 0xFFD6A36D, 0xFF8CBF82, 0xFF70BFD5, 0xFFD0B47C, 0xFFB4C875};
    private static final ResourceLocation[] CLASS_ICONS = new ResourceLocation[ClassId.values().length];
    private static final ResourceLocation[] CLASS_ART = new ResourceLocation[ClassId.values().length];
    private static final ResourceLocation[] SKILL_ICONS = new ResourceLocation[ClassId.values().length];

    static {
        for (ClassId id : ClassId.values()) {
            CLASS_ICONS[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "textures/gui/class_icons_v2/" + id.key() + ".png");
            CLASS_ART[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "textures/gui/class_cards_v3/" + id.key() + ".png");
            SKILL_ICONS[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "textures/gui/skill_icons_v1/" + id.key() + ".png");
        }
    }

    private MenuUi() {}

    private static ResourceLocation ui(String name) {
        return ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                "textures/gui/menu_v1/" + name + ".png");
    }

    static int accent(ClassId id) { return ACCENTS[id.networkIndex()]; }
    static ResourceLocation classIcon(ClassId id) { return CLASS_ICONS[id.networkIndex()]; }
    static ResourceLocation classArt(ClassId id) { return CLASS_ART[id.networkIndex()]; }

    static void drawPanel(GuiGraphics graphics, Box box) {
        drawSurface(graphics, box);
        int cell = 256;
        int edge = Math.max(6, Math.min(12, Math.min(box.width(), box.height()) / 16));
        int middleWidth = Math.max(1, box.width() - edge * 2);
        int middleHeight = Math.max(1, box.height() - edge * 2);

        region(graphics, PANEL_ATLAS, box.x(), box.y(), edge, edge, 0, 0, cell, cell, 768, 768);
        region(graphics, PANEL_ATLAS, box.right() - edge, box.y(), edge, edge, cell * 2, 0, cell, cell, 768, 768);
        region(graphics, PANEL_ATLAS, box.x(), box.bottom() - edge, edge, edge, 0, cell * 2, cell, cell, 768, 768);
        region(graphics, PANEL_ATLAS, box.right() - edge, box.bottom() - edge, edge, edge, cell * 2, cell * 2, cell, cell, 768, 768);

        region(graphics, PANEL_ATLAS, box.x() + edge, box.y(), middleWidth, edge, cell, 0, cell, cell, 768, 768);
        region(graphics, PANEL_ATLAS, box.x() + edge, box.bottom() - edge, middleWidth, edge, cell, cell * 2, cell, cell, 768, 768);
        region(graphics, PANEL_ATLAS, box.x(), box.y() + edge, edge, middleHeight, 0, cell, cell, cell, 768, 768);
        region(graphics, PANEL_ATLAS, box.right() - edge, box.y() + edge, edge, middleHeight, cell * 2, cell, cell, cell, 768, 768);
    }

    static void drawTitle(GuiGraphics graphics, Font font, Component title, Box outer, int screenWidth, int screenHeight) {
        int plateHeight = screenHeight < 280 ? 24 : 32;
        int plateWidth = Math.min(340, Math.max(150, outer.width() * 36 / 100));
        plateWidth = Math.min(plateWidth, Math.max(1, outer.width() - 12));
        int cap = Math.max(1, (int) Math.round(plateHeight * 132.0 / 342.0));
        int jewel = Math.max(1, (int) Math.round(plateHeight * 112.0 / 342.0));
        int middle = Math.max(2, plateWidth - cap * 2 - jewel);
        int leftMiddle = middle / 2;
        int rightMiddle = middle - leftMiddle;
        int x = (screenWidth - plateWidth) / 2;
        int y = outer.y() - 3;

        region(graphics, TITLE_PLATE, x, y, cap, plateHeight, 0, 0, 132, 342, 1024, 342);
        region(graphics, TITLE_PLATE, x + cap, y, leftMiddle, plateHeight, 132, 0, 324, 342, 1024, 342);
        region(graphics, TITLE_PLATE, x + cap + leftMiddle, y, jewel, plateHeight, 456, 0, 112, 342, 1024, 342);
        region(graphics, TITLE_PLATE, x + cap + leftMiddle + jewel, y, rightMiddle, plateHeight,
                568, 0, 324, 342, 1024, 342);
        region(graphics, TITLE_PLATE, x + plateWidth - cap, y, cap, plateHeight,
                892, 0, 132, 342, 1024, 342);

        int titleY = y + Math.max(1, (plateHeight - font.lineHeight) / 2);
        drawCentered(graphics, font, title, screenWidth / 2, titleY, Math.max(1, plateWidth - 40), 0xFFF1E7D7);
    }

    static void drawInsetSurface(GuiGraphics graphics, Box box) {
        drawSurface(graphics, box);
    }

    private static void drawSurface(GuiGraphics graphics, Box box) {
        int tile = 64;
        for (int y = box.y(); y < box.bottom(); y += tile) {
            int height = Math.min(tile, box.bottom() - y);
            int sourceHeight = Math.max(1, height * 4);
            for (int x = box.x(); x < box.right(); x += tile) {
                int width = Math.min(tile, box.right() - x);
                int sourceWidth = Math.max(1, width * 4);
                region(graphics, SURFACE_TILE, x, y, width, height, 0, 0,
                        sourceWidth, sourceHeight, 256, 256);
            }
        }
    }

    private static void region(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                               int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        if (width < 1 || height < 1 || sourceWidth < 1 || sourceHeight < 1) return;
        graphics.blit(texture, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    static void drawClassIcon(GuiGraphics graphics, ClassId id, int x, int y, int size) {
        graphics.blit(classIcon(id), x, y, size, size, 0, 0, 96, 96, 96, 96);
    }

    static void drawSkillIcon(GuiGraphics graphics, ClassId id, int index, int x, int y, int size) {
        int cell = 256;
        int u = Math.floorMod(index, 4) * cell;
        int v = Math.floorDiv(Math.max(0, index), 4) * cell;
        region(graphics, SKILL_ICONS[id.networkIndex()], x, y, size, size, u, v, cell, cell, 1024, 1024);
    }

    static void drawNodeFrame(GuiGraphics graphics, int state, int x, int y, int size) {
        int cell = 512;
        int safeState = Math.max(0, Math.min(3, state));
        int u = safeState % 2 * cell;
        int v = safeState / 2 * cell;
        region(graphics, NODE_STATES, x, y, size, size, u, v, cell, cell, 1024, 1024);
    }

    static void drawConnection(GuiGraphics graphics, int type, Box box) {
        int cell = 512;
        int safeType = Math.max(0, Math.min(3, type));
        int u = safeType % 2 * cell;
        int v = safeType / 2 * cell;
        region(graphics, CONNECTION_PATHS, box.x(), box.y(), box.width(), box.height(), u, v, cell, cell, 1024, 1024);
    }

    static void drawButtonSkin(GuiGraphics graphics, int x, int y, int width, int height, int state) {
        int row = Math.max(0, Math.min(3, state));
        region(graphics, BUTTON_STATES, x, y, width, height, 0, row * 256, 1024, 256, 1024, 1024);
    }

    static void drawRankProgress(GuiGraphics graphics, int x, int y, int width, int height, double progress) {
        if (width < 1 || height < 1) return;
        region(graphics, PROGRESS_STATES, x, y, width, height, 0, 307, 1024, 76, 1024, 1024);
        int fillWidth = (int) Math.round(width * Math.max(0.0, Math.min(1.0, progress)));
        if (fillWidth > 0) {
            region(graphics, PROGRESS_STATES, x, y, fillWidth, height, 0, 639, 1024, 76, 1024, 1024);
        }
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
        Component display = fit(font, text, maxWidth);
        graphics.drawString(font, display, centerX - font.width(display) / 2, y, color, false);
    }

    static void drawFit(GuiGraphics graphics, Font font, Component text, int x, int y,
                        int maxWidth, int color) {
        graphics.drawString(font, fit(font, text, maxWidth), x, y, color, false);
    }

    private static Component fit(Font font, Component text, int maxWidth) {
        if (maxWidth < 1) return Component.empty();
        if (font.width(text) <= maxWidth) return text;
        String value = text.getString();
        String ellipsis = "…";
        int contentWidth = Math.max(0, maxWidth - font.width(ellipsis));
        String prefix = font.plainSubstrByWidth(value, contentWidth).stripTrailing();
        Component clipped = Component.literal(prefix + ellipsis);
        return font.width(clipped) <= maxWidth ? clipped : Component.empty();
    }

    static void drawWrapped(GuiGraphics graphics, Font font, Component text, int x, int y,
                            int maxWidth, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(1, maxWidth));
        int count = Math.min(Math.max(0, maxLines), lines.size());
        for (int index = 0; index < count; index++) {
            graphics.drawString(font, lines.get(index), x, y + index * font.lineHeight, color, false);
        }
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
        private int pressedFrames;

        Button(Box box, Component label, Consumer<Button> action) {
            super(box.x(), box.y(), box.width(), box.height(), label);
            this.action = action;
        }

        @Override public void onPress() {
            pressedFrames = 2;
            action.accept(this);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int state = pressedFrames > 0 ? 2 : (!active ? 3 : (isHoveredOrFocused() ? 1 : 0));
            if (pressedFrames > 0) pressedFrames--;
            drawButtonSkin(graphics, getX(), getY(), width, height, state);
            int available = Math.max(1, width - Math.max(8, width / 12));
            drawCentered(graphics, Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + Math.max(1, (height - Minecraft.getInstance().font.lineHeight) / 2),
                    available, active ? 0xFFF1E9DB : 0xFFAAA39B);
        }
    }
}
