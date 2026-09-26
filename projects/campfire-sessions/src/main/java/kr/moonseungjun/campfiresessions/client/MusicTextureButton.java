package kr.moonseungjun.campfiresessions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MusicTextureButton extends AbstractWidget {
    @FunctionalInterface
    public interface PressAction {
        void onPress(MusicTextureButton button);
    }

    private final Identifier sprite;
    private final PressAction action;

    public MusicTextureButton(int x, int y, int width, int height, Component message, Identifier sprite, PressAction action) {
        super(x, y, width, height, message);
        this.sprite = sprite;
        this.action = action;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        if (active) action.onPress(this);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
        MusicTheme theme = CampfireMusicClient.selectedTrack().theme();
        if (!active) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0x88000000);
        } else if (isHovered()) {
            graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, 0x2AFFFFFF);
        }
        var font = Minecraft.getInstance().font;
        graphics.centeredText(font, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2, active ? theme.text() : 0xFF818181);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
