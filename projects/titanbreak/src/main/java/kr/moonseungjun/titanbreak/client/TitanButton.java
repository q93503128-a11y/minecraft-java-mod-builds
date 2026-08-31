package kr.moonseungjun.titanbreak.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Button with the same hard-surface visual language as TITANBREAK station interfaces. */
public final class TitanButton extends Button {
    private TitanButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static TitanButton create(Component message, OnPress onPress,
                                     int x, int y, int width, int height) {
        return new TitanButton(x, y, width, height, message, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TitanInterfaceTheme.cyberButton(graphics, Minecraft.getInstance().font, getMessage(),
                getX(), getY(), getWidth(), getHeight(), active, isHoveredOrFocused());
    }
}
