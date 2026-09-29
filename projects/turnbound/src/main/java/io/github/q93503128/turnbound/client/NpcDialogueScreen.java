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
    private int scrollLine;

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
        panelWidth = Math.min(620, Math.max(300, width - 28));
        List<String> lines = wrappedLines();
        panelHeight = Math.min(200, Math.max(104, 58 + Math.min(lines.size(), 9) * 12));
        panelLeft = (width - panelWidth) / 2;
        panelTop = height - panelHeight - 24;
        scrollLine = Math.max(0, Math.min(scrollLine, maxScroll(lines)));
        addRenderableWidget(new BattleHudButton(
                panelLeft + panelWidth - 68, panelTop + panelHeight - 23, 54, 16,
                Component.literal("닫기"), TurnboundUiTokens.PRIMARY, ignored -> onClose()));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){}

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        TurnboundUiSkin.panel(graphics, panelLeft, panelTop, panelWidth, panelHeight);
        graphics.text(font, Component.literal(speaker.isBlank() ? "대화" : speaker),
                panelLeft + 14, panelTop + 9, TurnboundUiTokens.ACCENT, true);
        TurnboundFrameStyle.divider(graphics, panelLeft + 14, panelTop + 23, panelWidth - 28);
        List<String> lines = wrappedLines();
        int visible = visibleLines();
        int end = Math.min(lines.size(), scrollLine + visible);
        int y = panelTop + 30;
        for (int i = scrollLine; i < end; i++) {
            graphics.text(font, Component.literal(lines.get(i)), panelLeft + 16, y, TurnboundUiTokens.TEXT_PRIMARY, false);
            y += 12;
        }
        int max = maxScroll(lines);
        if (max > 0) {
            int trackX = panelLeft + panelWidth - 11;
            int trackTop = panelTop + 30;
            int trackBottom = panelTop + panelHeight - 30;
            graphics.fill(trackX, trackTop, trackX + 2, trackBottom, 0x553A3A3A);
            int trackH = Math.max(1, trackBottom - trackTop);
            int thumbH = Math.max(10, trackH * visible / Math.max(visible, lines.size()));
            int thumbY = trackTop + (trackH - thumbH) * scrollLine / max;
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, TurnboundUiTokens.ACCENT);
        }
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY) {
        List<String> lines = wrappedLines();
        int max = maxScroll(lines);
        if (max > 0 && scrollY != 0) {
            scrollLine = Math.max(0, Math.min(max, scrollLine + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
    }

    private List<String> wrappedLines() {
        return UiTextLayout.wrap(dialogue, Math.max(80, panelWidth - 42), 96);
    }

    private int visibleLines() {
        return Math.max(2, (panelHeight - 62) / 12);
    }

    private int maxScroll(List<String> lines) {
        return Math.max(0, lines.size() - visibleLines());
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
