package kr.moonseungjun.campfiresessions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
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
        if (this.active) this.action.onPress(this);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, getX(), getY(), getWidth(), getHeight());
        if (!this.active) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0x66000000);
        } else if (isHovered()) {
            graphics.fill(getX() + 2, getY() + 2, getRight() - 2, getBottom() - 2, 0x18FFFFFF);
        }
        int color = this.active ? 0xFFF8EBD2 : 0xFF8B8172;
        graphics.text(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + (getWidth() - Minecraft.getInstance().font.width(getMessage())) / 2,
                getY() + (getHeight() - 8) / 2,
                color,
                true
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
