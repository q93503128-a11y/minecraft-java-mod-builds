package kr.moonseungjun.anomalousclasses.client;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSwarmTacticsPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SwarmTacticActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SwarmTacticStatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Uses the existing class menu's frame/button assets. Both full and quick mode
 * edit the same server tactics; changes require an approved response.
 */
public final class SwarmTacticsScreen extends Screen {
    private SwarmTacticStatePayload state;
    private UUID editing = SwarmTacticActionPayload.NONE;
    private EditBox nameField, ratioField;
    private MenuUi.Box outer, surface;
    private boolean pending;
    private int pendingAction = -1;
    private int feedback = SwarmTacticStatePayload.OPEN;

    public SwarmTacticsScreen(SwarmTacticStatePayload initial) {
        super(Component.translatable(initial.view() == RequestSwarmTacticsPayload.QUICK
                ? "screen.anomalousclasses.swarm.quick_title" : "screen.anomalousclasses.swarm.title"));
        state = initial;
        editing = initial.selectedId();
    }

    private static Component label(String suffix) {
        return Component.translatable("screen.anomalousclasses.swarm." + suffix);
    }

    @Override protected void init() {
        int margin = width < 360 ? 5 : 12;
        int vMargin = height < 235 ? 3 : 8;
        outer = new MenuUi.Box(margin, vMargin, Math.max(1, width - margin * 2),
                Math.max(1, height - vMargin * 2));
        int inset = Math.min(26, Math.max(12, Math.min(outer.width(), outer.height()) / 11));
        surface = new MenuUi.Box(outer.x() + inset, outer.y() + 28,
                Math.max(1, outer.width() - inset * 2), Math.max(1, outer.height() - 38));
        int editorY = surface.bottom() - 82;
        int listY = surface.y() + 21;
        int listSpace = Math.max(44, editorY - listY - 6);
        int rowHeight = Math.max(13, Math.min(22, (listSpace - 9) / 4));
        int gap = 5;
        int colWidth = Math.max(20, (surface.width() - gap) / 2);
        for (int i = 0; i < 8; i++) {
            int col = i % 2, row = i / 2;
            MenuUi.Box cell = new MenuUi.Box(surface.x() + col * (colWidth + gap),
                    listY + row * (rowHeight + 3), colWidth, rowHeight);
            if (i < state.tactics().size()) {
                var tactic = state.tactics().get(i);
                boolean selected = tactic.id().equals(state.selectedId()) && !state.temporary();
                Component name = Component.literal((selected ? "◆ " : "") + tactic.name() +
                        "  " + tactic.attackPercent() + "%");
                addRenderableWidget(new MenuUi.Button(cell, name, b -> choose(tactic.id())));
            } else if (i == state.tactics().size()) {
                addRenderableWidget(new MenuUi.Button(cell, label("new"), b -> newEditor()));
            }
        }
        int nameWidth = Math.max(20, surface.width() - 67);
        nameField = new EditBox(font, surface.x(), editorY, nameWidth, 18, label("name"));
        nameField.setMaxLength(24);
        nameField.setHint(label("name_hint"));
        ratioField = new EditBox(font, surface.x() + nameWidth + 5, editorY, 62, 18, label("ratio"));
        ratioField.setMaxLength(3);
        ratioField.setHint(label("ratio_hint"));
        addRenderableWidget(nameField);
        addRenderableWidget(ratioField);
        var found = state.tactics().stream().filter(t -> t.id().equals(editing)).findFirst().orElse(null);
        if (found == null) {
            editing = SwarmTacticActionPayload.NONE;
            ratioField.setValue(state.appliedAttackPercent() >= 0
                    ? Integer.toString(state.appliedAttackPercent()) : "");
        } else {
            nameField.setValue(found.name());
            ratioField.setValue(Integer.toString(found.attackPercent()));
        }

        String[] buttons = {"new","apply","save","temporary","delete","restore","up","down","back"};
        int bw = Math.max(12, (surface.width() - 8) / 3);
        for (int i = 0; i < buttons.length; i++) {
            int code = i;
            MenuUi.Box box = new MenuUi.Box(surface.x() + i % 3 * (bw + 4),
                    editorY + 21 + i / 3 * 19, bw, 17);
            addRenderableWidget(new MenuUi.Button(box, label(buttons[i]), b -> action(code)));
        }
    }

    private void choose(UUID id) {
        if (pending) return;
        if (state.view() == RequestSwarmTacticsPayload.QUICK) {
            send(SwarmTacticActionPayload.SELECT, id, "", -1, -1);
        } else {
            editing = id;
            var tactic = state.tactics().stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
            if (tactic != null) {
                nameField.setValue(tactic.name());
                ratioField.setValue(Integer.toString(tactic.attackPercent()));
            }
        }
    }

    private void newEditor() {
        if (pending) return;
        editing = SwarmTacticActionPayload.NONE;
        nameField.setValue("");
        ratioField.setValue(state.appliedAttackPercent() >= 0
                ? Integer.toString(state.appliedAttackPercent()) : "");
        setFocused(nameField);
    }

    private int percent() {
        try {
            int value = Integer.parseInt(ratioField.getValue().trim());
            return value >= 0 && value <= 100 ? value : -1;
        } catch (NumberFormatException invalid) { return -1; }
    }

    private void action(int action) {
        if (pending) return;
        switch (action) {
            case 0 -> newEditor();
            case 1 -> {
                if (!editing.equals(SwarmTacticActionPayload.NONE))
                    send(SwarmTacticActionPayload.SELECT, editing, "", -1, -1);
            }
            case 2 -> {
                if (percent() < 0 || nameField.getValue().isBlank()) {
                    feedback = SwarmTacticStatePayload.REJECTED; return;
                }
                send(editing.equals(SwarmTacticActionPayload.NONE)
                        ? SwarmTacticActionPayload.CREATE : SwarmTacticActionPayload.UPDATE,
                        editing, nameField.getValue(), percent(), -1);
            }
            case 3 -> {
                if (percent() < 0) { feedback = SwarmTacticStatePayload.REJECTED; return; }
                send(SwarmTacticActionPayload.TEMPORARY,
                        SwarmTacticActionPayload.NONE, "", percent(), -1);
            }
            case 4 -> {
                if (!editing.equals(SwarmTacticActionPayload.NONE))
                    send(SwarmTacticActionPayload.DELETE, editing, "", -1, -1);
            }
            case 5 -> {
                if (state.temporary())
                    send(SwarmTacticActionPayload.RESTORE,
                            SwarmTacticActionPayload.NONE, "", -1, -1);
            }
            case 6, 7 -> {
                int index = -1;
                for (int i = 0; i < state.tactics().size(); i++)
                    if (state.tactics().get(i).id().equals(editing)) index = i;
                int next = index + (action == 6 ? -1 : 1);
                if (index >= 0 && next >= 0 && next < state.tactics().size())
                    send(SwarmTacticActionPayload.MOVE, editing, "", -1, next);
            }
            case 8 -> onClose();
            default -> { }
        }
    }

    private void send(int operation, UUID id, String name, int value, int position) {
        pending = true;
        pendingAction = operation;
        feedback = SwarmTacticStatePayload.OPEN;
        PacketDistributor.sendToServer(new SwarmTacticActionPayload(
                state.view(), operation, state.revision(), id, name, value, position));
    }

    public void applyServerState(SwarmTacticStatePayload approved) {
        if (approved.view() != state.view()) return;
        int operation = pendingAction;
        state = approved;
        pending = false;
        pendingAction = -1;
        feedback = approved.result();
        if (approved.result() == SwarmTacticStatePayload.UPDATED
                && operation == SwarmTacticActionPayload.SELECT
                && state.view() == RequestSwarmTacticsPayload.QUICK) {
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }
        if (approved.result() == SwarmTacticStatePayload.UPDATED) {
            if (operation == SwarmTacticActionPayload.CREATE) editing = state.selectedId();
            if (operation == SwarmTacticActionPayload.DELETE) editing = SwarmTacticActionPayload.NONE;
        }
        clearWidgets();
        init();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);
        MenuUi.drawInsetSurface(graphics, surface);
        MenuUi.drawFit(graphics, font, Component.translatable(
                "screen.anomalousclasses.swarm.counts", state.alive(), state.maximum(),
                state.attack(), state.guard(), state.lift(), state.regroup()),
                surface.x() + 2, surface.y() + 2, surface.width() - 4, 0xFFF4E8D7);
        String ratio = state.appliedAttackPercent() < 0 ? "-" :
                state.appliedAttackPercent() + ":" + (100 - state.appliedAttackPercent());
        Component current = state.temporary() ? label("temporary_status") :
                state.tactics().stream().filter(t -> t.id().equals(state.selectedId()))
                        .findFirst().map(t -> (Component) Component.literal(t.name())).orElse(label("none"));
        MenuUi.drawFit(graphics, font, Component.translatable(
                "screen.anomalousclasses.swarm.current", ratio, current),
                surface.x() + 2, surface.y() + 12, surface.width() - 4, 0xFFD8D0C5);
        for (Renderable widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        if (pending || feedback == SwarmTacticStatePayload.STALE
                || feedback == SwarmTacticStatePayload.REJECTED) {
            MenuUi.drawCentered(graphics, font, label(pending ? "pending" :
                    feedback == SwarmTacticStatePayload.STALE ? "stale" : "invalid"),
                    width / 2, outer.bottom() - 8, outer.width() - 16, 0xFFFFDAB7);
        }
    }

    @Override public void onClose() {
        if (state.view() == RequestSwarmTacticsPayload.MANAGEMENT
                && ClassManagementNavigation.shouldReturnToProfile()) {
            ClassManagementNavigation.clearReturnToProfile();
            PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
        } else if (minecraft != null) minecraft.setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }
}
