package kr.moonseungjun.campfiresessions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MusicTextureButton extends AbstractWidget {
    public enum Style { PRIMARY, SECONDARY, DANGER }

    @FunctionalInterface
    public interface PressAction { void onPress(MusicTextureButton button); }

    private final Style style;
    private final PressAction action;

    public MusicTextureButton(int x, int y, int width, int height, Component message, Style style, PressAction action) {
        super(x, y, width, height, message);
        this.style = style;
        this.action = action;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        if (active) action.onPress(this);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        MusicTheme theme = CampfireMusicClient.selectedTrack().theme();
        int border = switch (style) {
            case PRIMARY -> theme.accent();
            case DANGER -> 0xFFFF716B;
            case SECONDARY -> 0xFF56616B;
        };
        int background = switch (style) {
            case PRIMARY -> 0xD929333A;
            case DANGER -> 0xD9342022;
            case SECONDARY -> 0xC91A2026;
        };
        if (isHovered() && active) background = switch (style) {
            case PRIMARY -> 0xEA35434B;
            case DANGER -> 0xEA543034;
            case SECONDARY -> 0xE1263038;
        };
        if (!active) background = 0xA014171A;

        graphics.fill(getX() + 1, getY(), getRight() - 1, getBottom(), border);
        graphics.fill(getX(), getY() + 1, getRight(), getBottom() - 1, border);
        graphics.fill(getX() + 2, getY() + 1, getRight() - 2, getBottom() - 1, background);
        graphics.fill(getX() + 1, getY() + 2, getRight() - 1, getBottom() - 2, background);

        var font = Minecraft.getInstance().font;
        graphics.centeredText(font, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2,
                active ? theme.text() : 0xFF777B80);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
