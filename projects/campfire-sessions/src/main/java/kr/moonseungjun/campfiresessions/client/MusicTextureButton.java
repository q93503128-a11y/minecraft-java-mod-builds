package kr.moonseungjun.campfiresessions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
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
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, theme.buttonSprite(), getX(), getY(), getWidth(), getHeight());

        if (!active) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0x85000000);
        } else if (isHovered()) {
            graphics.fill(getX() + 2, getY() + 2, getRight() - 2, getBottom() - 2,
                    style == Style.DANGER ? 0x30FF5D51 : 0x28FFFFFF);
        }

        if (style == Style.PRIMARY) {
            graphics.fill(getX() + 4, getBottom() - 2, getRight() - 4, getBottom() - 1, theme.accent());
        }

        var font = Minecraft.getInstance().font;
        int textColor = active ? theme.text() : 0xFF777C81;
        graphics.centeredText(font, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
