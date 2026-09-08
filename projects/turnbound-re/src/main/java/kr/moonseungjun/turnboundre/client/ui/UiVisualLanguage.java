package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Shared M5 visual tokens for TURNBOUND: RE.
 *
 * <p>This is intentionally a small semantic layer over Minecraft's built-in sprite atlas. It keeps production
 * screens from inventing unrelated colors, frames, and meter treatments while the final authored atlas is being
 * selected and screenshot-validated. It owns presentation only; no gameplay state is derived here.</p>
 */
public final class UiVisualLanguage {
    public static final Identifier FRAME_IDLE = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    public static final Identifier FRAME_ACTIVE = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    public static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
    public static final Identifier BAR_BACKGROUND = Identifier.withDefaultNamespace("boss_bar/white_background");
    public static final Identifier HP_PROGRESS = Identifier.withDefaultNamespace("boss_bar/red_progress");
    public static final Identifier POISE_PROGRESS = Identifier.withDefaultNamespace("boss_bar/yellow_progress");
    public static final Identifier ENERGY_PROGRESS = Identifier.withDefaultNamespace("boss_bar/blue_progress");

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFB7BAC4;
    public static final int TEXT_FOCUS = 0xFFFFE0A6;
    public static final int TEXT_WARNING = 0xFFFFB866;
    public static final int TEXT_SUCCESS = 0xFFA7F3B0;

    private UiVisualLanguage() {}

    public static void titleBand(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component text,
            int color,
            boolean centered
    ) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX, x, y, width, height);
        int textX = centered ? x + (width - font.width(text)) / 2 : x + UiLayoutMetrics.SPACE_8;
        int textY = y + Math.max(1, (height - font.lineHeight) / 2);
        graphics.text(font, text, textX, textY, color, true);
    }

    public static void frame(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active
    ) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, active ? FRAME_ACTIVE : FRAME_IDLE, x, y, width, height);
    }

    public static void meter(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int value,
            int max,
            Identifier progressSprite
    ) {
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
