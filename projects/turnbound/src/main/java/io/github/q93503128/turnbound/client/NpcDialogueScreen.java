package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Small world-preserving dialogue panel for physical NPC conversations. */
public final class NpcDialogueScreen extends Screen {
    private final String speaker;
    private final String dialogue;
    private int panelLeft, panelTop, panelWidth, panelHeight;

    public NpcDialogueScreen(String speaker, String dialogue) {
        super(Component.literal(speaker == null || speaker.isBlank() ? "대화" : speaker));
        this.speaker = speaker == null ? "" : speaker.trim();
        this.dialogue = dialogue == null ? "" : dialogue.trim();
    }

    static NpcDialogueScreen decode(String encoded) {
        if (encoded == null) return new NpcDialogueScreen("", "");
        int split = encoded.indexOf('\n');
        if (split < 0) return new NpcDialogueScreen("", encoded);
        return new NpcDialogueScreen(encoded.substring(0, split), encoded.substring(split + 1));
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(520, Math.max(260, width - 36));
        List<String> lines = UiTextLayout.wrap(dialogue, panelWidth - 40, 96);
        panelHeight = Math.min(150, Math.max(82, 50 + lines.size() * 12));
        panelLeft = (width - panelWidth) / 2;
        panelTop = height - panelHeight - 34;
        addRenderableWidget(new BattleHudButton(
                panelLeft + panelWidth - 72, panelTop + panelHeight - 26, 58, 18,
                Component.literal("닫기"), TurnboundUiTokens.PRIMARY, ignored -> onClose()));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){}

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        TurnboundUiSkin.panel(graphics, panelLeft, panelTop, panelWidth, panelHeight);
        graphics.text(font, Component.literal(speaker.isBlank() ? "대화" : speaker),
                panelLeft + 16, panelTop + 13, TurnboundUiTokens.ACCENT, true);
        TurnboundFrameStyle.divider(graphics, panelLeft + 16, panelTop + 29, panelWidth - 32);
        int y = panelTop + 39;
        for (String line : UiTextLayout.wrap(dialogue, panelWidth - 40, 96)) {
            if (y + font.lineHeight >= panelTop + panelHeight - 30) break;
            graphics.text(font, Component.literal(line), panelLeft + 20, y, TurnboundUiTokens.TEXT_PRIMARY, false);
            y += 12;
        }
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean isPauseScreen(){return false;}
}
