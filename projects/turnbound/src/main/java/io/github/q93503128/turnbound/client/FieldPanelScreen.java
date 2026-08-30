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

/** World-first quest/reward/relay panel. It never replaces the field with a full-screen menu backdrop. */
public final class FieldPanelScreen extends Screen {
    private static final int DEEP = 0xE810131A;
    private static final int PANEL = 0xDB171C26;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int DANGER = 0xFFFF7A59;

    private final FieldUiSnapshot.Mode initialMode;
    private final List<BattleHudButton> travelButtons = new ArrayList<>();
    private BattleHudButton closeButton;
    private long seen = -1;

    public FieldPanelScreen(FieldUiSnapshot.Mode mode) {
        super(Component.literal("TURNBOUND Field"));
        this.initialMode = mode == null ? FieldUiSnapshot.Mode.QUEST : mode;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (!ClientFieldState.snapshot().active()) {
            onClose();
            return;
        }
        if (seen != ClientFieldState.revision()) rebuildButtons();
    }

    private void rebuildButtons() {
        seen = ClientFieldState.revision();
        clearWidgets();
        travelButtons.clear();
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        FieldUiSnapshot.Mode mode = snapshot.mode() == FieldUiSnapshot.Mode.NONE ? initialMode : snapshot.mode();
        Rect panel = panel(mode);
        int buttonY = panel.bottom() - 28;

        if (mode == FieldUiSnapshot.Mode.TRAVEL) {
            int y = panel.y() + 68;
            for (FieldUiSnapshot.Travel travel : snapshot.travels()) {
                String suffix = travel.current() ? "  · 현재 위치" : travel.unlocked() ? "" : "  · 미활성";
                BattleHudButton button = addRenderableWidget(new BattleHudButton(
                        panel.x() + 14, y, panel.width() - 28, 24,
                        Component.literal(travel.label() + suffix), BLUE,
                        ignored -> travel(travel.id())));
                button.active = travel.unlocked() && !travel.current();
                travelButtons.add(button);
                y += 30;
            }
        }

        closeButton = addRenderableWidget(new BattleHudButton(
                panel.right() - 82, buttonY, 68, 20,
                Component.literal(mode == FieldUiSnapshot.Mode.RESULT ? "계속" : "닫기"),
                mode == FieldUiSnapshot.Mode.RESULT ? GREEN : BLUE,
                ignored -> onClose()));
    }

    private void travel(String destinationId) {
        ClientPacketDistributor.sendToServer(new FieldCommandPayload("TRAVEL|" + destinationId));
        onClose();
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the authored field visible behind quest/reward/travel UI.
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        FieldUiSnapshot.Mode mode = snapshot.mode() == FieldUiSnapshot.Mode.NONE ? initialMode : snapshot.mode();
        Rect panel = panel(mode);
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), DEEP);
        int accent = mode == FieldUiSnapshot.Mode.RESULT ? GREEN : mode == FieldUiSnapshot.Mode.TRAVEL ? BLUE : GOLD;
        graphics.fill(panel.x(), panel.y(), panel.x() + 3, panel.bottom(), accent);

        switch (mode) {
            case QUEST -> drawQuest(graphics, panel, snapshot);
            case RESULT -> drawResult(graphics, panel, snapshot);
            case TRAVEL -> drawTravel(graphics, panel, snapshot);
            case NONE -> drawQuest(graphics, panel, snapshot);
        }
    }

    private void drawQuest(GuiGraphicsExtractor graphics, Rect panel, FieldUiSnapshot snapshot) {
        int x = panel.x() + 14;
        int y = panel.y() + 12;
        graphics.text(font, Component.literal("남문 정찰관"), x, y, GOLD, true);
        graphics.text(font, Component.literal("CHAPTER 1 · 남문 초원"), x, y + 15, SECONDARY, true);
        y += 38;
        for (String line : wrap(snapshot.dialogue(), panel.width() - 28)) {
            graphics.text(font, Component.literal(line), x, y, TEXT, true);
            y += 11;
        }
        y += 7;
        graphics.text(font, Component.literal("현재 목표"), x, y, SECONDARY, true);
        y += 13;
        graphics.text(font, Component.literal(snapshot.objective()), x, y, TEXT, true);
        y += 19;

        for (FieldUiSnapshot.Encounter encounter : snapshot.encounters()) {
            String mark;
            int color;
            if (encounter.cleared()) {
                mark = "✓";
                color = GREEN;
            } else if (!encounter.unlocked()) {
                mark = "·";
                color = MUTED;
            } else if (encounter.boss()) {
                mark = "◆";
                color = DANGER;
            } else {
                mark = "○";
                color = TEXT;
            }
            graphics.text(font, Component.literal(mark + "  " + encounter.label()), x, y, color, true);
            y += 13;
        }

        int footerY = panel.bottom() - 49;
        graphics.fill(x, footerY - 5, panel.right() - 14, footerY - 4, 0x806D7786);
        graphics.text(font, Component.literal("누적 보상  XP " + snapshot.earnedXp() + "  ·  Gold " + snapshot.earnedGold()),
                x, footerY + 3, SECONDARY, true);
    }

    private void drawResult(GuiGraphicsExtractor graphics, Rect panel, FieldUiSnapshot snapshot) {
        FieldUiSnapshot.Reward reward = snapshot.reward();
        int x = panel.x() + 16;
        int y = panel.y() + 15;
        String title = reward.chapterCleared() ? "CHAPTER 1 CLEAR" : "전투 승리";
        graphics.text(font, Component.literal(title), x, y, reward.chapterCleared() ? GOLD : GREEN, true);
        y += 22;
        graphics.text(font, Component.literal(reward.encounterLabel()), x, y, TEXT, true);
        y += 21;
        graphics.text(font, Component.literal("XP  +" + reward.xp()), x, y, GREEN, true);
        graphics.text(font, Component.literal("Gold  +" + reward.gold()), x + Math.min(120, panel.width() / 2), y, GOLD, true);
        y += 23;
        if (!reward.firstClear()) {
            graphics.text(font, Component.literal("이미 획득한 조우 보상입니다."), x, y, MUTED, true);
            y += 15;
        }
        graphics.fill(x, y + 2, panel.right() - 16, y + 3, 0x806D7786);
        y += 13;
        graphics.text(font, Component.literal("다음 목표"), x, y, SECONDARY, true);
        y += 13;
        for (String line : wrap(snapshot.objective(), panel.width() - 32)) {
            graphics.text(font, Component.literal(line), x, y, TEXT, true);
            y += 11;
        }
    }

    private void drawTravel(GuiGraphicsExtractor graphics, Rect panel, FieldUiSnapshot snapshot) {
        int x = panel.x() + 14;
        int y = panel.y() + 12;
        graphics.text(font, Component.literal("ASTER RELAY"), x, y, BLUE, true);
        graphics.text(font, Component.literal("활성화한 계전석 사이를 이동합니다."), x, y + 16, SECONDARY, true);
        graphics.text(font, Component.literal("미활성 거점은 직접 도달해 계전석을 조사해야 합니다."), x, y + 31, MUTED, true);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String candidate = current.toString() + c;
            if (current.length() > 0 && font.width(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private Rect panel(FieldUiSnapshot.Mode mode) {
        int w = switch (mode) {
            case RESULT -> Math.min(340, Math.max(260, width - 32));
            case TRAVEL -> Math.min(330, Math.max(270, width - 24));
            default -> Math.min(360, Math.max(292, width - 24));
        };
        int h = switch (mode) {
            case RESULT -> Math.min(190, Math.max(150, height - 30));
            case TRAVEL -> Math.min(230, Math.max(180, height - 24));
            default -> Math.min(310, Math.max(240, height - 24));
        };
        int x = mode == FieldUiSnapshot.Mode.QUEST ? 12 : Math.max(12, (width - w) / 2);
        int y = Math.max(12, (height - h) / 2);
        return new Rect(x, y, w, h);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    private record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }
}
