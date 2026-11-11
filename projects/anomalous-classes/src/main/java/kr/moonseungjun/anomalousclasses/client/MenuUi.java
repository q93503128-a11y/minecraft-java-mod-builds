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
    private static final ResourceLocation[] PANEL_CORNERS = {
            ui("frame_parts/corner_tl"), ui("frame_parts/corner_tr"),
            ui("frame_parts/corner_bl"), ui("frame_parts/corner_br")
    };
    private static final ResourceLocation[] TOP_RAILS = {
            ui("frame_parts/rail_top_left"), ui("frame_parts/rail_top_right")
    };
    private static final ResourceLocation[] BOTTOM_RAILS = {
            ui("frame_parts/rail_bottom_left"), ui("frame_parts/rail_bottom_right")
    };
    private static final ResourceLocation[] SIDE_RAILS = {
            ui("frame_parts/rail_side_left"), ui("frame_parts/rail_side_right")
    };
    private static final ResourceLocation[] SIDE_JEWELS = {
            ui("frame_parts/jewel_side_left"), ui("frame_parts/jewel_side_right")
    };
    private static final ResourceLocation TOP_JEWEL = ui("frame_parts/jewel_top");
    private static final ResourceLocation BOTTOM_JEWEL = ui("frame_parts/jewel_bottom");
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
        int corner = Math.max(6, Math.min(40, Math.min(box.width(), box.height()) / 10));
        corner = Math.min(corner, Math.min(box.width() / 2, box.height() / 2));
        int railOffset = scale(corner, 32, 160);
        int railThickness = Math.max(1, scale(corner, 64, 160));
        int centerX = box.x() + box.width() / 2;
        boolean ornaments = corner >= 14 && box.width() >= 120 && box.height() >= 48;
        int jewelWidth = ornaments ? Math.max(1, scale(corner, 64, 160)) : 0;
        int jewelHeight = ornaments ? Math.max(1, scale(corner, 88, 160)) : 0;
        int jewelLeft = centerX - jewelWidth / 2;
        int jewelRight = centerX + (jewelWidth + 1) / 2;
        int edgeStart = box.x() + corner - 1;
        int edgeEnd = box.right() - corner + 1;
        int topRailY = box.y() + railOffset;
        int bottomRailY = box.bottom() - corner + railOffset;

        if (ornaments) {
            drawHorizontalRail(graphics, TOP_RAILS[0], edgeStart, topRailY, jewelLeft, railThickness, 12, 24, 64);
            drawHorizontalRail(graphics, TOP_RAILS[1], jewelRight, topRailY, edgeEnd, railThickness, 12, 24, 64);
            drawHorizontalRail(graphics, BOTTOM_RAILS[0], edgeStart, bottomRailY, jewelLeft, railThickness, 12, 24, 64);
            drawHorizontalRail(graphics, BOTTOM_RAILS[1], jewelRight, bottomRailY, edgeEnd, railThickness, 12, 24, 64);
        } else {
            drawHorizontalRail(graphics, TOP_RAILS[0], edgeStart, topRailY, edgeEnd, railThickness, 12, 24, 64);
            drawHorizontalRail(graphics, BOTTOM_RAILS[0], edgeStart, bottomRailY, edgeEnd, railThickness, 12, 24, 64);
        }

        int centerY = box.y() + box.height() / 2;
        int sideJewelWidth = ornaments ? Math.max(1, scale(corner, 48, 160)) : 0;
        int sideJewelHeight = ornaments ? Math.max(1, scale(corner, 64, 160)) : 0;
        int sideJewelTop = centerY - sideJewelHeight / 2;
        int sideLeftX = box.x() + railOffset;
        int sideRightX = box.right() - corner + railOffset;
        int sideEnd = box.bottom() - corner + 1;
        int sideStart = box.y() + corner - 1;
        if (ornaments) {
            drawVerticalRail(graphics, SIDE_RAILS[0], sideLeftX, sideStart, sideJewelTop, railThickness, 12, 64, 24);
            drawVerticalRail(graphics, SIDE_RAILS[0], sideLeftX, sideJewelTop + sideJewelHeight,
                    sideEnd, railThickness, 12, 64, 24);
            drawVerticalRail(graphics, SIDE_RAILS[1], sideRightX, sideStart, sideJewelTop, railThickness, 12, 64, 24);
            drawVerticalRail(graphics, SIDE_RAILS[1], sideRightX, sideJewelTop + sideJewelHeight,
                    sideEnd, railThickness, 12, 64, 24);
        } else {
            drawVerticalRail(graphics, SIDE_RAILS[0], sideLeftX, sideStart, sideEnd, railThickness, 12, 64, 24);
            drawVerticalRail(graphics, SIDE_RAILS[1], sideRightX, sideStart, sideEnd, railThickness, 12, 64, 24);
        }

        drawSprite(graphics, PANEL_CORNERS[0], box.x(), box.y(), corner, corner, 160, 160);
        drawSprite(graphics, PANEL_CORNERS[1], box.right() - corner, box.y(), corner, corner, 160, 160);
        drawSprite(graphics, PANEL_CORNERS[2], box.x(), box.bottom() - corner, corner, corner, 160, 160);
        drawSprite(graphics, PANEL_CORNERS[3], box.right() - corner, box.bottom() - corner, corner, corner, 160, 160);

        if (ornaments) {
            int sideJewelLeft = sideLeftX + (railThickness - sideJewelWidth) / 2;
            int sideJewelRight = sideRightX + (railThickness - sideJewelWidth) / 2;
            region(graphics, SIDE_JEWELS[0], sideJewelLeft, sideJewelTop, sideJewelWidth, sideJewelHeight,
                    0, 0, 24, 64, 24, 64);
            region(graphics, SIDE_JEWELS[1], sideJewelRight, sideJewelTop, sideJewelWidth, sideJewelHeight,
                    0, 0, 24, 64, 24, 64);
            int topJewelY = topRailY + (railThickness - jewelHeight) / 2;
            int bottomJewelY = bottomRailY + (railThickness - jewelHeight) / 2;
            region(graphics, TOP_JEWEL, jewelLeft, topJewelY, jewelWidth, jewelHeight,
                    0, 0, 32, 88, 32, 88);
            region(graphics, BOTTOM_JEWEL, jewelLeft, bottomJewelY, jewelWidth, jewelHeight,
                    0, 0, 32, 88, 32, 88);
        }
    }

    private static int scale(int value, int numerator, int denominator) {
        return Math.max(1, Math.round(value * (float) numerator / denominator));
    }

    private static void drawSprite(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                                   int width, int height, int sourceWidth, int sourceHeight) {
        region(graphics, texture, x, y, width, height, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
    }

    private static void drawHorizontalRail(GuiGraphics graphics, ResourceLocation texture,
                                           int x, int y, int endX, int height, int sampleX,
                                           int textureWidth, int textureHeight) {
        region(graphics, texture, x, y, endX - x, height, sampleX, 0, 1, textureHeight,
                textureWidth, textureHeight);
    }

    private static void drawVerticalRail(GuiGraphics graphics, ResourceLocation texture,
                                         int x, int y, int endY, int width, int sampleY,
                                         int textureWidth, int textureHeight) {
        region(graphics, texture, x, y, width, endY - y, 0, sampleY, textureWidth, 1,
                textureWidth, textureHeight);
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
        int cap = Math.min(height, Math.max(1, width / 2));
        int centerWidth = Math.max(0, width - cap * 2);
        region(graphics, BUTTON_STATES, x, y, cap, height, 0, row * 256, 256, 256, 1024, 1024);
        region(graphics, BUTTON_STATES, x + cap, y, centerWidth, height,
                256, row * 256, 512, 256, 1024, 1024);
        region(graphics, BUTTON_STATES, x + width - cap, y, cap, height,
                768, row * 256, 256, 256, 1024, 1024);
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

    static void drawFitRight(GuiGraphics graphics, Font font, Component text, int right, int y,
                             int maxWidth, int color) {
        Component display = fit(font, text, maxWidth);
        graphics.drawString(font, display, right - font.width(display), y, color, false);
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
