package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Small world-preserving dialogue panel for physical NPC conversations. */
public final class NpcDialogueScreen extends Screen {
    private static final String CHOICE_PREFIX = "@@TURNBOUND_CHOICE@@";
    private final String speaker;
    private final String dialogue;
    private final List<Choice> choices;
    private int panelLeft, panelTop, panelWidth, panelHeight;
    private int scrollLine;

    private record Choice(String label, String command) {}

    public NpcDialogueScreen(String speaker, String dialogue) {
        this(speaker, dialogue, List.of());
    }

    private NpcDialogueScreen(String speaker, String dialogue, List<Choice> choices) {
        super(Component.literal(speaker == null || speaker.isBlank() ? "대화" : speaker));
        this.speaker = speaker == null ? "" : speaker.trim();
        this.dialogue = dialogue == null ? "" : dialogue.trim();
        this.choices = choices == null ? List.of() : List.copyOf(choices);
    }

    static NpcDialogueScreen decode(String encoded) {
        if (encoded == null) return new NpcDialogueScreen("", "");
        String[] lines = encoded.split("\n", -1);
        String speaker = lines.length == 0 ? "" : lines[0];
        StringBuilder body = new StringBuilder();
        List<Choice> choices = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(CHOICE_PREFIX)) {
                String raw = line.substring(CHOICE_PREFIX.length());
                int tab = raw.indexOf('\t');
                if (tab > 0 && choices.size() < 3) {
                    String label = raw.substring(0, tab).trim();
                    String command = raw.substring(tab + 1).trim();
                    if (!label.isBlank()) choices.add(new Choice(label, command));
                }
                continue;
            }
            if (!body.isEmpty()) body.append('\n');
            body.append(line);
        }
        return new NpcDialogueScreen(speaker, body.toString(), choices);
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(620, Math.max(300, width - 28));
        List<String> lines = wrappedLines();
        int choiceSpace = choices.isEmpty() ? 0 : 24;
        panelHeight = Math.min(224, Math.max(104 + choiceSpace, 58 + choiceSpace + Math.min(lines.size(), 9) * 12));
        panelLeft = (width - panelWidth) / 2;
        panelTop = height - panelHeight - 24;
        scrollLine = Math.max(0, Math.min(scrollLine, maxScroll(lines)));

        if (!choices.isEmpty()) {
            int gap = 4;
            int x = panelLeft + 14;
            int available = panelWidth - 96;
            int buttonW = Math.max(72, (available - gap * (choices.size() - 1)) / choices.size());
            int y = panelTop + panelHeight - 45;
            for (int i = 0; i < choices.size(); i++) {
                Choice choice = choices.get(i);
                int xx = x + i * (buttonW + gap);
                addRenderableWidget(new BattleHudButton(
                        xx, y, buttonW, 17, Component.literal(choice.label()),
                        TurnboundUiTokens.PRIMARY, ignored -> select(choice)));
            }
        }

        addRenderableWidget(new BattleHudButton(
                panelLeft + panelWidth - 68, panelTop + panelHeight - 23, 54, 16,
                Component.literal("닫기"), TurnboundUiTokens.MUTED, ignored -> onClose()));
    }

    private void select(Choice choice) {
        if (choice != null && !choice.command().isBlank() && !"CLOSE".equals(choice.command())) {
            ClientPacketDistributor.sendToServer(new FieldCommandPayload(choice.command()));
        }
        onClose();
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
            int trackBottom = panelTop + panelHeight - (choices.isEmpty() ? 30 : 54);
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
        return Math.max(2, (panelHeight - (choices.isEmpty() ? 62 : 86)) / 12);
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
