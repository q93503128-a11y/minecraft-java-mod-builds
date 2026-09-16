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
        this(x, y, 190, 49, message, onPress);
    }

    public KenneyButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = Objects.requireNonNull(onPress);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        if (this.active) onPress.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        FishingUiTheme.drawPanelPixels(graphics, getX(), getY(), getWidth(), getHeight());

        int inset = Math.max(3, Math.min(5, getHeight() / 6));
        int accent = active ? FishingUiTheme.ACCENT : FishingUiTheme.TEXT_DISABLED;
        graphics.fill(getX() + inset, getY() + inset, getX() + inset + 2, getY() + getHeight() - inset, accent);
        if (!active) {
            graphics.fill(
                    getX() + inset + 4,
                    getY() + inset,
                    getX() + getWidth() - inset,
                    getY() + getHeight() - inset,
                    0x228B949C
            );
        } else if (isHovered()) {
            graphics.fill(
                    getX() + inset + 4,
                    getY() + inset,
                    getX() + getWidth() - inset,
                    getY() + getHeight() - inset,
                    0x222678B8
            );
        }

        var font = Minecraft.getInstance().font;
        int color = active ? FishingUiTheme.TEXT_PRIMARY : FishingUiTheme.TEXT_DISABLED;
        graphics.centeredText(
                font,
                getMessage(),
                getX() + getWidth() / 2 + 2,
                getY() + (getHeight() - font.lineHeight) / 2 + 1,
                color
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
