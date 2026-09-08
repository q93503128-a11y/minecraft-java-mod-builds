package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Shared M5 visual tokens for TURNBOUND: RE.
 *
 * <p>This is intentionally a semantic layer over the current bridge sprites. Screen code asks for presentation
 * meaning (focus, disabled, warning, success) rather than knowing atlas filenames. The backing sprites can therefore
 * move from vanilla bridge art to the selected TURNBOUND: RE atlas without leaking asset choices into gameplay UI.</p>
 */
public final class UiVisualLanguage {
    public enum FrameState {
        IDLE,
        FOCUS,
        DISABLED,
        WARNING,
        SUCCESS
    }

    public static final Identifier FRAME_IDLE = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    public static final Identifier FRAME_ACTIVE = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    public static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
    public static final Identifier BAR_BACKGROUND = Identifier.withDefaultNamespace("boss_bar/white_background");
    public static final Identifier HP_PROGRESS = Identifier.withDefaultNamespace("boss_bar/red_progress");
    public static final Identifier POISE_PROGRESS = Identifier.withDefaultNamespace("boss_bar/yellow_progress");
    public static final Identifier ENERGY_PROGRESS = Identifier.withDefaultNamespace("boss_bar/blue_progress");

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFB7BAC4;
    public static final int TEXT_DISABLED = 0xFF858894;
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
            FrameState state
    ) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bridgeFrame(state), x, y, width, height);
    }

    /**
     * Transitional source-compatible entry point for the already validated M5 screens.
     * New/edited call sites should use {@link FrameState}; this overload is removed when the selected atlas lands.
     */
    public static void frame(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean focused
    ) {
        frame(graphics, x, y, width, height, focused ? FrameState.FOCUS : FrameState.IDLE);
    }

    /**
     * Temporary mapping while M5 still uses Minecraft-native bridge sprites.
     * The selected external atlas will provide distinct sprites for these states; callers do not need to change.
     */
    private static Identifier bridgeFrame(FrameState state) {
        return switch (state) {
            case FOCUS, WARNING, SUCCESS -> FRAME_ACTIVE;
            case IDLE, DISABLED -> FRAME_IDLE;
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
