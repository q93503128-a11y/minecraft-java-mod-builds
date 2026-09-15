package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Direct rendering facade for vendored Kenney Input Prompts Pixel number-key glyphs. */
final class InputPromptVisuals {
    private static final int GLYPH_SIZE = 16;
    private static final Identifier[] NUMBER_KEYS = {
            sprite("key_1"), sprite("key_2"), sprite("key_3"),
            sprite("key_4"), sprite("key_5"), sprite("key_6")
    };

    private InputPromptVisuals() {}

    static int glyphSize() {
        return GLYPH_SIZE;
    }

    static boolean hasNumberKey(int ordinal) {
        return ordinal >= 1 && ordinal <= NUMBER_KEYS.length;
    }

    static void numberKey(GuiGraphicsExtractor graphics, int ordinal, int x, int y) {
        if (!hasNumberKey(ordinal)) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NUMBER_KEYS[ordinal - 1], x, y, GLYPH_SIZE, GLYPH_SIZE);
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "input/" + path);
    }
}
