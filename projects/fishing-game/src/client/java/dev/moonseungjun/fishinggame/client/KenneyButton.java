package dev.moonseungjun.fishinggame.client;

import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class KenneyButton extends AbstractWidget {
    private final Runnable onPress;

    public KenneyButton(int x, int y, Component message, Runnable onPress) {
        super(x, y, 190, 49, message);
        this.onPress = Objects.requireNonNull(onPress);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        if (this.active) onPress.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        FishingUiTheme.drawPanelPixels(graphics, getX(), getY(), getWidth(), getHeight());

        int accent = active ? FishingUiTheme.ACCENT : FishingUiTheme.TEXT_DISABLED;
        graphics.fill(getX() + 7, getY() + 7, getX() + 10, getY() + getHeight() - 7, accent);
        if (!active) {
            graphics.fill(getX() + 12, getY() + 7, getX() + getWidth() - 7, getY() + getHeight() - 7, 0x228B949C);
        } else if (isHovered()) {
            graphics.fill(getX() + 12, getY() + 7, getX() + getWidth() - 7, getY() + getHeight() - 7, 0x222678B8);
        }

        var font = Minecraft.getInstance().font;
        int color = active ? FishingUiTheme.TEXT_PRIMARY : FishingUiTheme.TEXT_DISABLED;
        graphics.centeredText(
                font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - font.lineHeight) / 2 + 1,
                color
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
