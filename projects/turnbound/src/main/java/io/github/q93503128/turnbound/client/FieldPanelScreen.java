package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Compact quest/result/relay window; field remains visible and readable around it. */
public final class FieldPanelScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int DANGER = 0xFFFF7A59;

    private final FieldUiSnapshot.Mode initialMode;
    private final List<BattleHudButton> travelButtons = new ArrayList<>();
    private long seen = -1;

    public FieldPanelScreen(FieldUiSnapshot.Mode mode) {
        super(Component.literal("TURNBOUND Field"));
        initialMode = mode == null ? FieldUiSnapshot.Mode.QUEST : mode;
    }

    @Override protected void init() { super.init(); rebuildButtons(); }
    @Override public void tick() {
        super.tick();
        if (!ClientFieldState.snapshot().active()) { onClose(); return; }
        if (seen != ClientFieldState.revision()) rebuildButtons();
    }

    private void rebuildButtons() {
        seen = ClientFieldState.revision();
        clearWidgets();
        travelButtons.clear();
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        FieldUiSnapshot.Mode mode = snapshot.mode() == FieldUiSnapshot.Mode.NONE ? initialMode : snapshot.mode();
        Rect panel = panel(mode);
        if (mode == FieldUiSnapshot.Mode.TRAVEL) {
            int y = panel.y() + 55;
            for (FieldUiSnapshot.Travel travel : snapshot.travels()) {
                if (y + 22 > panel.bottom() - 30) break;
                String suffix = travel.current() ? " · 현재" : travel.unlocked() ? "" : " · 잠김";
                BattleHudButton button = addRenderableWidget(new BattleHudButton(panel.x() + 10, y, panel.width() - 20, 22,
                        Component.literal(travel.label() + suffix), BLUE, ignored -> travel(travel.id())));
                button.active = travel.unlocked() && !travel.current();
                travelButtons.add(button);
                y += 26;
            }
        }
        addRenderableWidget(new BattleHudButton(panel.right() - 62, panel.bottom() - 25, 52, 17,
                Component.literal(mode == FieldUiSnapshot.Mode.RESULT ? "계속" : "닫기"),
                mode == FieldUiSnapshot.Mode.RESULT ? GREEN : BLUE, ignored -> onClose()));
    }

    private void travel(String destinationId) {
        ClientPacketDistributor.sendToServer(new FieldCommandPayload("TRAVEL|" + destinationId));
        onClose();
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        FieldUiSnapshot.Mode mode = snapshot.mode() == FieldUiSnapshot.Mode.NONE ? initialMode : snapshot.mode();
        Rect panel = panel(mode);
        int accent = mode == FieldUiSnapshot.Mode.RESULT ? GREEN : mode == FieldUiSnapshot.Mode.TRAVEL ? BLUE : GOLD;
        TurnboundFrameStyle.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), accent);
        switch (mode) {
            case QUEST, NONE -> drawQuest(graphics, panel, snapshot);
            case RESULT -> drawResult(graphics, panel, snapshot);
            case TRAVEL -> drawTravel(graphics, panel);
        }
    }

    private void drawQuest(GuiGraphicsExtractor graphics, Rect p, FieldUiSnapshot s) {
        int x = p.x() + 10, y = p.y() + 9;
        graphics.text(font, Component.literal("남문 정찰관"), x, y, GOLD, true);
        graphics.text(font, Component.literal("CH.1 · 남문 초원"), p.right() - 10 - font.width("CH.1 · 남문 초원"), y, SECONDARY, true);
        y += 17;
        TurnboundFrameStyle.inset(graphics, x, y, p.width() - 20, 38);
        List<String> dialogue = wrap(s.dialogue(), p.width() - 32);
        for (int i = 0; i < Math.min(2, dialogue.size()); i++) graphics.text(font, Component.literal(dialogue.get(i)), x + 6, y + 7 + i * 11, TEXT, true);
        y += 45;
        graphics.text(font, Component.literal("목표"), x, y, GOLD, true);
        y += 12;
        for (String line : wrap(s.objective(), p.width() - 20)) {
            graphics.text(font, Component.literal(line), x, y, TEXT, true); y += 10;
            if (y > p.y() + 116) break;
        }
        y = Math.max(y + 5, p.y() + 122);
        int shown = 0;
        for (FieldUiSnapshot.Encounter e : s.encounters()) {
            if (y + 10 > p.bottom() - 39 || shown >= 4) break;
            String mark = e.cleared() ? "✓" : !e.unlocked() ? "·" : e.boss() ? "◆" : "○";
            int color = e.cleared() ? GREEN : !e.unlocked() ? MUTED : e.boss() ? DANGER : SECONDARY;
            graphics.text(font, Component.literal(mark + " " + e.label()), x, y, color, true);
            y += 11; shown++;
        }
        TurnboundFrameStyle.divider(graphics, x, p.bottom() - 35, p.width() - 20);
        graphics.text(font, Component.literal("XP " + s.earnedXp() + "  ·  Gold " + s.earnedGold()), x, p.bottom() - 28, SECONDARY, true);
    }

    private void drawResult(GuiGraphicsExtractor graphics, Rect p, FieldUiSnapshot s) {
        var r = s.reward();
        int x = p.x() + 12, y = p.y() + 12;
        graphics.text(font, Component.literal(r.chapterCleared() ? "CHAPTER CLEAR" : "전투 승리"), x, y, r.chapterCleared() ? GOLD : GREEN, true);
        y += 21;
        graphics.text(font, Component.literal(r.encounterLabel()), x, y, TEXT, true);
        y += 18;
        TurnboundFrameStyle.inset(graphics, x, y, p.width() - 24, 35);
        graphics.text(font, Component.literal("XP  +" + r.xp()), x + 8, y + 12, GREEN, true);
        String gold = "Gold  +" + r.gold();
        graphics.text(font, Component.literal(gold), p.right() - 20 - font.width(gold), y + 12, GOLD, true);
        y += 45;
        graphics.text(font, Component.literal("다음"), x, y, SECONDARY, true); y += 13;
        for (String line : wrap(s.objective(), p.width() - 24)) {
            graphics.text(font, Component.literal(line), x, y, TEXT, true); y += 11;
            if (y > p.bottom() - 35) break;
        }
    }

    private void drawTravel(GuiGraphicsExtractor graphics, Rect p) {
        int x = p.x() + 10, y = p.y() + 10;
        graphics.text(font, Component.literal("계전석"), x, y, BLUE, true);
        graphics.text(font, Component.literal("발견한 거점으로 이동"), x, y + 16, SECONDARY, true);
        TurnboundFrameStyle.divider(graphics, x, y + 35, p.width() - 20);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (current.length() > 0 && font.width(current.toString() + c) > maxWidth) { lines.add(current.toString()); current.setLength(0); }
            current.append(c);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private Rect panel(FieldUiSnapshot.Mode mode) {
        int desiredW = mode == FieldUiSnapshot.Mode.RESULT ? 260 : mode == FieldUiSnapshot.Mode.TRAVEL ? 250 : 292;
        int desiredH = mode == FieldUiSnapshot.Mode.RESULT ? 160 : mode == FieldUiSnapshot.Mode.TRAVEL ? 190 : 232;
        int w = Math.max(180, Math.min(desiredW, width - 16));
        int h = Math.max(130, Math.min(desiredH, height - 16));
        int x = Math.max(8, (width - w) / 2);
        int y = Math.max(8, (height - h) / 2);
        return new Rect(x, y, w, h);
    }

    @Override public boolean keyPressed(KeyEvent event) { if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; } return super.keyPressed(event); }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; } return super.mouseClicked(event, doubleClick); }
    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }
    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    private record Rect(int x, int y, int width, int height) { int right() { return x + width; } int bottom() { return y + height; } }
}
