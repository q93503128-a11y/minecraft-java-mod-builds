package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Shared M5 semantic visual tokens backed by the verified TURNBOUND: RE UI sprite family. */
public final class UiVisualLanguage {
    public enum FrameState { IDLE, FOCUS, DISABLED, WARNING, SUCCESS }

    public static final Identifier FRAME_IDLE = sprite("frame_idle");
    public static final Identifier FRAME_FOCUS = sprite("frame_focus");
    public static final Identifier FRAME_DISABLED = sprite("frame_disabled");
    public static final Identifier FRAME_WARNING = sprite("frame_warning");
    public static final Identifier FRAME_SUCCESS = sprite("frame_success");
    public static final Identifier TITLE_BOX = sprite("title_surface");
    public static final Identifier BAR_BACKGROUND = sprite("meter_track");
    public static final Identifier HP_PROGRESS = sprite("meter_hp");
    public static final Identifier POISE_PROGRESS = sprite("meter_poise");
    public static final Identifier ENERGY_PROGRESS = sprite("meter_energy");

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFB7BAC4;
    public static final int TEXT_DISABLED = 0xFF858894;
    public static final int TEXT_FOCUS = 0xFFFFE0A6;
    public static final int TEXT_WARNING = 0xFFFFB866;
    public static final int TEXT_SUCCESS = 0xFFA7F3B0;

    private UiVisualLanguage() {}

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "ui/" + path);
    }

    public static void titleBand(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
                                 int height, Component text, int color, boolean centered) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX, x, y, width, height);
        int textX = centered ? x + (width - font.width(text)) / 2 : x + UiLayoutMetrics.SPACE_8;
        int textY = y + Math.max(1, (height - font.lineHeight) / 2);
        graphics.text(font, text, textX, textY, color, true);
    }

    public static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                             FrameState state) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frameSprite(state), x, y, width, height);
    }

    private static Identifier frameSprite(FrameState state) {
        return switch (state) {
            case IDLE -> FRAME_IDLE;
            case FOCUS -> FRAME_FOCUS;
            case DISABLED -> FRAME_DISABLED;
            case WARNING -> FRAME_WARNING;
            case SUCCESS -> FRAME_SUCCESS;
        };
    }

    public static int textColor(FrameState state) {
        return switch (state) {
            case IDLE -> TEXT_PRIMARY;
            case FOCUS -> TEXT_FOCUS;
            case DISABLED -> TEXT_DISABLED;
            case WARNING -> TEXT_WARNING;
            case SUCCESS -> TEXT_SUCCESS;
        };
    }

    public static void meter(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                             int value, int max, Identifier progressSprite) {
        if (width <= 0 || height <= 0 || max <= 0) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND, x, y, width, height);
        int clampedValue = Math.max(0, Math.min(value, max));
        int fill = Math.max(0, Math.min(width, (int) Math.round(width * (clampedValue / (double) max))));
        if (fill > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, progressSprite, x, y, fill, height);
    }

    public static int revealedRows(int presentationTicks, int totalRows, int startTick, int rowIntervalTicks) {
        if (totalRows <= 0 || presentationTicks < startTick) return 0;
        int interval = Math.max(1, rowIntervalTicks);
        return Math.min(totalRows, 1 + (presentationTicks - startTick) / interval);
    }
}
