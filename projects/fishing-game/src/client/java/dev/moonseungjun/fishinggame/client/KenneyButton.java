package dev.moonseungjun.fishinggame.client;

import java.util.Objects;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class KenneyButton extends AbstractWidget {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("fishinggame", "textures/gui/kenney/blue_button00.png");
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 0, 0, 190, 49, 190, 49);
        if (isHovered() && active) {
            graphics.fill(getX() + 3, getY() + 3, getX() + 187, getY() + 46, 0x22FFFFFF);
        }
        int color = active ? 0xFFFFFFFF : 0xFF777777;
        graphics.centeredText(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX() + 95, getY() + 20, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
